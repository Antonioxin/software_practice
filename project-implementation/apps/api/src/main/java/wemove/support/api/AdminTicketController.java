package wemove.support.api;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wemove.identity.domain.BaseRole;
import wemove.platform.ActorContext;
import wemove.platform.IdentityPort;
import wemove.platform.api.ApiEnvelope;
import wemove.platform.idempotency.IdempotencyExecutor;
import wemove.support.api.SupportDtos.*;
import wemove.support.domain.TicketRules;
import wemove.support.service.TicketCommandService;
import wemove.support.service.TicketQueryService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 后台工单处理（FR-21）：管理员查询、开始处理、公开回复、内部备注与关闭。 */
@RestController
@RequestMapping("/api/v1/admin/tickets")
public class AdminTicketController {
    private final IdentityPort identity;
    private final TicketCommandService commands;
    private final TicketQueryService queries;

    public AdminTicketController(
            IdentityPort identity, TicketCommandService commands, TicketQueryService queries) {
        this.identity = identity;
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<List<TicketSummary>>> list(
            Authentication a,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        UUID admin = admin(a);
        Page<TicketSummary> result =
                queries.list(
                        null,
                        SupportParsers.type(type),
                        SupportParsers.status(status),
                        from,
                        to,
                        SupportParsers.pageable(page, pageSize));
        PageMeta meta = new PageMeta(page, pageSize, result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.page(result.getContent(), meta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiEnvelope<TicketDetail>> detail(
            Authentication a, @PathVariable UUID id) {
        admin(a);
        TicketDetail ticket =
                queries.detail(
                        queries.ticket(id).orElseThrow(TicketRules::notFound), true);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.of(ticket));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiEnvelope<TicketDetail>> start(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody StartRequest request) {
        return result(commands.start(admin(a), id, key, request));
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<ApiEnvelope<TicketDetail>> reply(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody ReplyRequest request) {
        return result(commands.reply(admin(a), id, key, request));
    }

    @PostMapping("/{id}/internal-notes")
    public ResponseEntity<ApiEnvelope<TicketDetail>> note(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody NoteRequest request) {
        return result(commands.addNote(admin(a), id, key, request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiEnvelope<TicketDetail>> close(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody AdminCloseRequest request) {
        return result(commands.closeByAdmin(admin(a), id, key, request));
    }

    private UUID admin(Authentication a) {
        ActorContext actor = identity.requireActiveActor(a);
        if (actor.baseRole() != BaseRole.ADMIN)
            throw new wemove.platform.api.ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "您无权访问管理后台。");
        return actor.actorId();
    }

    private <T> ResponseEntity<ApiEnvelope<T>> result(IdempotencyExecutor.Result<T> result) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(ApiEnvelope.of(result.value()));
    }
}
