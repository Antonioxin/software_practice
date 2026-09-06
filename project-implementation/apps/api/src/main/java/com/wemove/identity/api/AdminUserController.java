package com.wemove.identity.api;

import com.wemove.identity.domain.*;
import com.wemove.identity.platform.*;
import com.wemove.identity.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final IdentityPort identity;
    private final UserAccountService accounts;

    public AdminUserController(IdentityPort identity, UserAccountService accounts) {
        this.identity = identity;
        this.accounts = accounts;
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<List<Dtos.UserSummary>>> list(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String nickname,
        @RequestParam(required = false) String baseRole,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        Authentication authentication) {
        requireAdmin(authentication);
        if (page < 1 || pageSize < 1 || pageSize > 50) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "分页参数不合法。");
        }
        BaseRole role = parseEnum(baseRole, BaseRole.class, "baseRole");
        AccountStatus accountStatus = parseEnum(status, AccountStatus.class, "status");
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
        Page<Dtos.UserSummary> result = accounts.list(email, nickname, role, accountStatus, pageable);
        Dtos.PageMeta meta = new Dtos.PageMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiEnvelope.page(result.getContent(), meta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiEnvelope<Dtos.UserDetail>> detail(@PathVariable UUID id, Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(accounts.detail(id)));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiEnvelope<Dtos.UserSummary>> disable(
        @PathVariable UUID id, @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody Dtos.AccountCommand request, Authentication authentication) {
        return command(id, key, request, authentication, "DISABLE");
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiEnvelope<Dtos.UserSummary>> restore(
        @PathVariable UUID id, @RequestHeader("Idempotency-Key") UUID key,
        @Valid @RequestBody Dtos.AccountCommand request, Authentication authentication) {
        return command(id, key, request, authentication, "RESTORE");
    }

    private ResponseEntity<ApiEnvelope<Dtos.UserSummary>> command(UUID id, UUID key,
        Dtos.AccountCommand request, Authentication authentication, String action) {
        ActorContext actor = requireAdmin(authentication);
        UserAccountService.CommandResult result = accounts.changeStatus(actor.actorId(), id, action, key, request);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (result.replayed()) response.header("Idempotency-Replayed", "true");
        return response.body(ApiEnvelope.of(result.user()));
    }

    private ActorContext requireAdmin(Authentication authentication) {
        ActorContext actor = identity.requireActiveActor(authentication);
        if (actor.baseRole() != BaseRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "您无权访问管理后台。");
        }
        return actor;
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type, String field) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", field + " 不合法。");
        }
    }
}
