package wemove.support.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wemove.platform.IdentityPort;
import wemove.platform.api.ApiEnvelope;
import wemove.platform.idempotency.IdempotencyExecutor;
import wemove.support.api.SupportDtos.*;
import wemove.support.domain.TicketRules;
import wemove.support.domain.TicketStatus;
import wemove.support.domain.TicketType;
import wemove.support.service.TicketAccess;
import wemove.support.service.TicketCommandService;
import wemove.support.service.TicketQueryService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 用户工单入口（FR-19/20）：仅启用的非管理员账户，仅限本人工单。 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final IdentityPort identity;
    private final TicketCommandService commands;
    private final TicketQueryService queries;

    public TicketController(
            IdentityPort identity, TicketCommandService commands, TicketQueryService queries) {
        this.identity = identity;
        this.commands = commands;
        this.queries = queries;
    }

    @PostMapping
    public ResponseEntity<ApiEnvelope<TicketDetail>> create(
            Authentication a,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody CreateTicketRequest request) {
        return result(commands.create(user(a), key, request), 201);
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<List<TicketSummary>>> list(
            Authentication a,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        UUID actor = user(a);
        Page<TicketSummary> result =
                queries.list(
                        actor,
                        SupportParsers.type(type),
                        SupportParsers.status(status),
                        null,
                        null,
                        SupportParsers.pageable(page, pageSize));
        PageMeta meta = new PageMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.page(result.getContent(), meta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiEnvelope<TicketDetail>> detail(Authentication a, @PathVariable UUID id) {
        UUID actor = user(a);
        TicketDetail ticket = queries.ownedDetail(actor, id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.of(ticket));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiEnvelope<TicketDetail>> followUp(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody FollowUpRequest request) {
        return result(commands.followUp(user(a), id, key, request), 200);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiEnvelope<TicketDetail>> close(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody UserCloseRequest request) {
        return result(commands.closeByUser(user(a), id, key, request), 200);
    }

    private UUID user(Authentication a) {
        var actor = identity.requireActiveActor(a);
        TicketAccess.requireRole(actor, false);
        return actor.actorId();
    }

    private <T> ResponseEntity<ApiEnvelope<T>> result(IdempotencyExecutor.Result<T> result, int status) {
        return ResponseEntity.status(result.replayed() ? 200 : status)
                .cacheControl(CacheControl.noStore())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(ApiEnvelope.of(result.value()));
    }
}
