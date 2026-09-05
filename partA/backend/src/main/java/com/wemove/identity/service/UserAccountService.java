package com.wemove.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wemove.identity.api.*;
import com.wemove.identity.config.WemoveProperties;
import com.wemove.identity.domain.*;
import com.wemove.identity.platform.*;
import com.wemove.identity.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {
    private final UserRepository users;
    private final UserConsentRepository consents;
    private final AccountStatusHistoryRepository histories;
    private final IdempotencyRecordRepository idempotency;
    private final PasswordEncoder encoder;
    private final WemoveProperties properties;
    private final DealerIdentityPort dealerIdentity;
    private final AuditPort audit;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public UserAccountService(UserRepository users, UserConsentRepository consents,
                              AccountStatusHistoryRepository histories,
                              IdempotencyRecordRepository idempotency, PasswordEncoder encoder,
                              WemoveProperties properties, DealerIdentityPort dealerIdentity,
                              AuditPort audit, JdbcTemplate jdbc, ObjectMapper mapper) {
        this.users = users;
        this.consents = consents;
        this.histories = histories;
        this.idempotency = idempotency;
        this.encoder = encoder;
        this.properties = properties;
        this.dealerIdentity = dealerIdentity;
        this.audit = audit;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional
    public Dtos.Actor register(Dtos.RegisterRequest request) {
        String email = request.email().strip();
        String normalizedEmail = normalizeEmail(email);
        String nickname = request.nickname().strip();
        validateCodePoints("nickname", nickname, 2, 30, "昵称需为 2—30 个字符。");
        validatePassword(request.password(), request.confirmPassword());
        if (!request.termsVersion().equals(properties.registration().termsVersion()) ||
            !request.privacyVersion().equals(properties.registration().privacyVersion())) {
            throw field("termsVersion", "CONSENT_VERSION_EXPIRED", "说明已更新，请重新阅读并确认。");
        }
        if (users.existsByEmailNormalized(normalizedEmail)) {
            throw field("email", "EMAIL_ALREADY_EXISTS", "该邮箱已注册。");
        }
        Instant now = clock.instant();
        UserEntity user = UserEntity.create(email, normalizedEmail, encoder.encode(request.password()),
            nickname, BaseRole.USER, now);
        users.save(user);
        consents.save(new UserConsentEntity(user.getId(), request.termsVersion(), request.privacyVersion(), now));
        return actor(user);
    }

    @Transactional(readOnly = true)
    public Dtos.Actor actor(UUID id) {
        return actor(requireUser(id));
    }

    @Transactional
    public Dtos.Actor updateProfile(UUID id, Dtos.ProfileRequest request) {
        UserEntity user = requireUser(id);
        if (user.getBaseRole() != BaseRole.USER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_PROFILE_READ_ONLY", "管理员资料不通过个人中心修改。");
        }
        String nickname = request.nickname().strip();
        validateCodePoints("nickname", nickname, 2, 30, "昵称需为 2—30 个字符。");
        String phone = normalizePhone(request.phone());
        user.updateProfile(nickname, phone, clock.instant());
        return actor(user);
    }

    @Transactional(readOnly = true)
    public Page<Dtos.UserSummary> list(String email, String nickname, BaseRole role,
                                        AccountStatus status, Pageable pageable) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (email != null && !email.isBlank()) predicates.add(cb.like(root.get("emailNormalized"), "%" + normalizeEmail(email) + "%"));
            if (nickname != null && !nickname.isBlank()) predicates.add(cb.like(root.get("nickname"), "%" + nickname.strip() + "%"));
            if (role != null) predicates.add(cb.equal(root.get("baseRole"), role));
            if (status != null) predicates.add(cb.equal(root.get("accountStatus"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return users.findAll(spec, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public Dtos.UserDetail detail(UUID id) {
        UserEntity user = requireUser(id);
        List<Dtos.StatusHistory> items = histories.findByUserIdOrderByCreatedAtDesc(id).stream()
            .map(h -> new Dtos.StatusHistory(h.getAction(), h.getPreviousStatus(), h.getNewStatus(),
                h.getReason(), h.getCreatedAt())).toList();
        return new Dtos.UserDetail(summary(user), items);
    }

    @Transactional
    public CommandResult changeStatus(UUID actorId, UUID targetId, String action, UUID key,
                                      Dtos.AccountCommand request) {
        String operation = action.equals("DISABLE") ? "disableUser" : "restoreUser";
        String requestHash = sha256(targetId + "|" + action + "|" + request.expectedVersion() + "|" + request.reason().strip());
        Optional<IdempotencyRecordEntity> existing = idempotency.findByActorIdAndOperationIdAndIdempotencyKey(actorId, operation, key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "同一请求标识不能用于不同内容。");
            }
            try {
                return new CommandResult(mapper.readValue(existing.get().getResponseJson(), Dtos.UserSummary.class), true);
            } catch (JsonProcessingException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_DATA_INVALID", "已保存的请求结果无法读取。");
            }
        }
        UserEntity target = users.findForUpdateById(targetId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。"));
        if (target.getBaseRole() == BaseRole.ADMIN) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_TARGET_PROTECTED", "本模块不允许停用或恢复管理员。");
        }
        if (target.getVersion() != request.expectedVersion()) {
            throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "账户已更新，请刷新后重试。");
        }
        AccountStatus next = action.equals("DISABLE") ? AccountStatus.DISABLED : AccountStatus.ACTIVE;
        if (target.getAccountStatus() == next) {
            throw new ApiException(HttpStatus.CONFLICT, "STATE_CONFLICT", "账户已处于目标状态。");
        }
        Instant now = clock.instant();
        AccountStatus previous = target.changeStatus(next, now);
        users.flush();
        histories.save(new AccountStatusHistoryEntity(targetId, actorId, action, previous, next, request.reason().strip(), now));
        if (next == AccountStatus.DISABLED) revokeSessions(target.getEmailNormalized());
        audit.append(new AuditPort.AuditEvent(actorId, "ACCOUNT_" + action, "USER", targetId,
            "SUCCESS", request.reason().strip(), now));
        Dtos.UserSummary result = summary(target);
        try {
            idempotency.save(new IdempotencyRecordEntity(actorId, operation, key, requestHash,
                mapper.writeValueAsString(result), 200, now));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return new CommandResult(result, false);
    }

    private void revokeSessions(String principalName) {
        List<String> ids = jdbc.queryForList("SELECT PRIMARY_ID FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", String.class, principalName);
        for (String id : ids) jdbc.update("DELETE FROM SPRING_SESSION WHERE PRIMARY_ID = ?", id);
    }

    private Dtos.Actor actor(UserEntity user) {
        String derived = dealerIdentity.derivedIdentity(user.getId());
        Set<String> capabilities = new LinkedHashSet<>();
        if (user.getBaseRole() == BaseRole.ADMIN) {
            capabilities.addAll(Set.of("ADMIN_USERS_READ", "ADMIN_USERS_WRITE"));
        } else {
            capabilities.addAll(Set.of("ACCOUNT_PROFILE_READ", "ACCOUNT_PROFILE_WRITE"));
            if ("DEALER".equals(derived)) capabilities.add("DEALER_ACCESS");
        }
        return new Dtos.Actor(user.getId(), user.getEmail(), user.getNickname(), user.getPhone(),
            user.getBaseRole().name(), user.getAccountStatus().name(), derived, capabilities, user.getVersion());
    }

    private Dtos.UserSummary summary(UserEntity user) {
        return new Dtos.UserSummary(user.getId(), user.getEmail(), user.getNickname(), user.getPhone(),
            user.getBaseRole().name(), user.getAccountStatus().name(), dealerIdentity.derivedIdentity(user.getId()),
            user.getVersion(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private UserEntity requireUser(UUID id) {
        return users.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。"));
    }

    public static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String normalized = phone.replaceAll("[\\s\\-()]", "");
        if (!normalized.matches("^\\+?\\d{6,20}$")) throw field("phone", "INVALID_PHONE", "请输入有效联系电话，可包含国家区号。");
        return normalized;
    }

    private void validatePassword(String password, String confirmation) {
        int count = password.codePointCount(0, password.length());
        if (count < 8 || count > 64 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw field("password", "WEAK_PASSWORD", "密码需为 8—64 个字符，并包含字母和数字。");
        }
        if (!password.equals(confirmation)) throw field("confirmPassword", "PASSWORD_MISMATCH", "两次输入的密码不一致。");
    }

    private void validateCodePoints(String field, String value, int min, int max, String message) {
        int count = value.codePointCount(0, value.length());
        if (count < min || count > max) throw field(field, "INVALID_LENGTH", message);
    }

    private ApiException field(String field, String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。",
            List.of(new ApiException.FieldViolation(field, code, message)));
    }

    private String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public record CommandResult(Dtos.UserSummary user, boolean replayed) {}
}
