# Page Dependency Trees

No authored frontend exists at initialization time. Planned dependency trees for the new target:

## `/login`

- `partA/frontend/src/pages/LoginPage.vue`
  - `partA/frontend/src/components/AuthShell.vue`
  - `partA/frontend/src/stores/session.ts`
  - `partA/frontend/src/services/http.ts`

## `/register`

- `partA/frontend/src/pages/RegisterPage.vue`
  - `partA/frontend/src/components/AuthShell.vue`
  - `partA/frontend/src/services/http.ts`

## `/account/profile`

- `partA/frontend/src/pages/ProfilePage.vue`
  - `partA/frontend/src/components/SiteShell.vue`
  - `partA/frontend/src/stores/session.ts`
  - `partA/frontend/src/services/http.ts`

## `/admin/users`

- `partA/frontend/src/pages/AdminUsersPage.vue`
  - `partA/frontend/src/components/SiteShell.vue`
  - `partA/frontend/src/services/http.ts`
  - `partA/frontend/src/components/StatusBadge.vue`
