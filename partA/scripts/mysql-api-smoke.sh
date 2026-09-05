#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
part_a_dir="$(cd "$script_dir/.." && pwd)"
jar_path="$part_a_dir/backend/target/identity-service-1.0.0.jar"
api_origin="${SMOKE_ORIGIN:-http://localhost:5173}"
api_base="${SMOKE_API_BASE:-http://127.0.0.1:8080/api/v1}"

: "${DB_URL:?请设置 DB_URL}"
: "${DB_USERNAME:?请设置 DB_USERNAME}"
: "${DB_PASSWORD:?请设置 DB_PASSWORD}"
: "${SMOKE_ADMIN_EMAIL:?请设置 SMOKE_ADMIN_EMAIL，并与 BOOTSTRAP_ADMIN_EMAIL 一致}"
: "${SMOKE_ADMIN_PASSWORD:?请设置 SMOKE_ADMIN_PASSWORD，并与 BOOTSTRAP_ADMIN_PASSWORD 一致}"

if [[ ! -f "$jar_path" ]]; then
  echo "未找到后端 JAR，请先执行：cd partA/backend && mvn package" >&2
  exit 1
fi

run_dir="$(mktemp -d /tmp/wemove-role-a-smoke.XXXXXX)"
user_cookie="$run_dir/user.cookies"
admin_cookie="$run_dir/admin.cookies"
server_log="$run_dir/backend.log"
smoke_email="smoke-$(date +%s%N)@example.test"
smoke_password="AdultPass123"
server_pid=""

cleanup() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    kill "$server_pid"
    wait "$server_pid" 2>/dev/null || true
  fi
  rm -rf "$run_dir"
}
trap cleanup EXIT

BOOTSTRAP_ADMIN_EMAIL="$SMOKE_ADMIN_EMAIL" \
BOOTSTRAP_ADMIN_PASSWORD="$SMOKE_ADMIN_PASSWORD" \
BOOTSTRAP_ADMIN_NICKNAME="冒烟测试管理员" \
java -jar "$jar_path" >"$server_log" 2>&1 &
server_pid=$!

for _ in $(seq 1 40); do
  if curl -fsS -c "$user_cookie" "$api_base/auth/csrf" >"$run_dir/user-csrf.json" 2>/dev/null; then
    break
  fi
  if ! kill -0 "$server_pid" 2>/dev/null; then
    sed -n '1,240p' "$server_log" >&2
    exit 1
  fi
  sleep 1
done

if [[ ! -s "$run_dir/user-csrf.json" ]]; then
  sed -n '1,240p' "$server_log" >&2
  echo "后端未在预期时间内就绪" >&2
  exit 1
fi

user_csrf="$(jq -er '.data.token' "$run_dir/user-csrf.json")"
curl -fsS -b "$user_cookie" -c "$user_cookie" \
  -H "Origin: $api_origin" -H "X-CSRF-Token: $user_csrf" -H "Content-Type: application/json" \
  -d "{\"email\":\"$smoke_email\",\"nickname\":\"冒烟用户\",\"password\":\"$smoke_password\",\"confirmPassword\":\"$smoke_password\",\"adultConfirmed\":true,\"termsVersion\":\"2026-09-05\",\"privacyVersion\":\"2026-09-05\",\"termsAccepted\":true,\"privacyAccepted\":true}" \
  "$api_base/auth/register" | jq -e '.data.baseRole == "USER" and .data.accountStatus == "ACTIVE"' >/dev/null

curl -fsS -b "$user_cookie" -c "$user_cookie" \
  -H "Origin: $api_origin" -H "X-CSRF-Token: $user_csrf" -H "Content-Type: application/json" \
  -d "{\"email\":\"$smoke_email\",\"password\":\"$smoke_password\"}" \
  "$api_base/auth/login" | jq -e '.data.email == "'"$smoke_email"'"' >/dev/null

curl -fsS -b "$user_cookie" -c "$user_cookie" "$api_base/auth/csrf" >"$run_dir/user-csrf.json"
user_csrf="$(jq -er '.data.token' "$run_dir/user-csrf.json")"
curl -fsS -b "$user_cookie" -c "$user_cookie" -X PATCH \
  -H "Origin: $api_origin" -H "X-CSRF-Token: $user_csrf" -H "Content-Type: application/json" \
  -d '{"nickname":"已验证用户","phone":"+86 138-0000-0000"}' \
  "$api_base/account/profile" | jq -e '.data.nickname == "已验证用户" and .data.phone == "+8613800000000"' >/dev/null

curl -fsS -c "$admin_cookie" "$api_base/auth/csrf" >"$run_dir/admin-csrf.json"
admin_csrf="$(jq -er '.data.token' "$run_dir/admin-csrf.json")"
curl -fsS -b "$admin_cookie" -c "$admin_cookie" \
  -H "Origin: $api_origin" -H "X-CSRF-Token: $admin_csrf" -H "Content-Type: application/json" \
  -d "{\"email\":\"$SMOKE_ADMIN_EMAIL\",\"password\":\"$SMOKE_ADMIN_PASSWORD\"}" \
  "$api_base/auth/login" | jq -e '.data.baseRole == "ADMIN"' >/dev/null

curl -fsS -b "$admin_cookie" -c "$admin_cookie" "$api_base/auth/csrf" >"$run_dir/admin-csrf.json"
admin_csrf="$(jq -er '.data.token' "$run_dir/admin-csrf.json")"
curl -fsS -b "$admin_cookie" "$api_base/admin/users?email=$smoke_email" >"$run_dir/users.json"
user_id="$(jq -er '.data[0].id' "$run_dir/users.json")"
user_version="$(jq -er '.data[0].version' "$run_dir/users.json")"

curl -fsS -b "$admin_cookie" -X POST \
  -H "Origin: $api_origin" -H "X-CSRF-Token: $admin_csrf" \
  -H "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid)" -H "Content-Type: application/json" \
  -d "{\"expectedVersion\":$user_version,\"reason\":\"本地 MySQL 冒烟验证\"}" \
  "$api_base/admin/users/$user_id/disable" >"$run_dir/disabled.json"
jq -e '.data.accountStatus == "DISABLED"' "$run_dir/disabled.json" >/dev/null

old_session_status="$(curl -sS -o /dev/null -w '%{http_code}' -b "$user_cookie" "$api_base/auth/me")"
[[ "$old_session_status" == "401" ]]

user_version="$(jq -er '.data.version' "$run_dir/disabled.json")"
curl -fsS -b "$admin_cookie" -X POST \
  -H "Origin: $api_origin" -H "X-CSRF-Token: $admin_csrf" \
  -H "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid)" -H "Content-Type: application/json" \
  -d "{\"expectedVersion\":$user_version,\"reason\":\"完成本地 MySQL 冒烟验证\"}" \
  "$api_base/admin/users/$user_id/restore" | jq -e '.data.accountStatus == "ACTIVE"' >/dev/null

echo "MySQL API smoke OK: register, login, profile, disable/session revoke, restore"
