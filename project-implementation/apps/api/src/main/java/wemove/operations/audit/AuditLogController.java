package wemove.operations.audit;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wemove.identity.domain.BaseRole;
import wemove.platform.ActorContext;
import wemove.platform.IdentityPort;
import wemove.platform.api.ApiEnvelope;
import wemove.platform.api.ApiException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR-32 关键操作记录的只读检索入口（管理员）。审计不提供任何修改或删除接口，
 * 查询动作本身也不写入审计。
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {
    private static final int MAX_PAGE_SIZE = 50;
    private final IdentityPort identity;
    private final AuditRecordRepository records;

    public AuditLogController(IdentityPort identity, AuditRecordRepository records) {
        this.identity = identity;
        this.records = records;
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<List<AuditView>>> list(
            Authentication a,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) UUID objectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        requireAdmin(a);
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "分页参数不合法。");
        org.springframework.data.jpa.domain.Specification<AuditRecord> spec =
                (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (actorId != null) predicates.add(cb.equal(root.get("actorId"), actorId));
                    if (action != null && !action.isBlank())
                        predicates.add(cb.equal(root.get("action"), action.strip()));
                    if (objectType != null && !objectType.isBlank())
                        predicates.add(cb.equal(root.get("objectType"), objectType.strip()));
                    if (objectId != null) predicates.add(cb.equal(root.get("objectId"), objectId));
                    if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
                    if (to != null) predicates.add(cb.lessThan(root.get("occurredAt"), to));
                    return cb.and(predicates.toArray(Predicate[]::new));
                };
        PageRequest pageable =
                PageRequest.of(
                        page - 1,
                        pageSize,
                        Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by("id")));
        Page<AuditView> result = records.findAll(spec, pageable).map(AuditView::from);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ApiEnvelope.page(
                                result.getContent(),
                                new PageMeta(page, pageSize, result.getTotalElements(), result.getTotalPages())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiEnvelope<AuditView>> detail(
            Authentication a, @PathVariable UUID id) {
        requireAdmin(a);
        AuditView view =
                AuditView.from(records.findById(id).orElseThrow(this::notFound));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(view));
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。");
    }

    private void requireAdmin(Authentication a) {
        ActorContext actor = identity.requireActiveActor(a);
        if (actor.baseRole() != BaseRole.ADMIN)
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "您无权访问管理后台。");
    }

    public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {}
}
