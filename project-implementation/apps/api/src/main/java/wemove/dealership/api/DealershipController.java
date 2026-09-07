package wemove.dealership.api;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wemove.dealership.api.DealershipDtos.*;
import wemove.dealership.service.DealershipService;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import wemove.platform.api.*;
import wemove.platform.idempotency.IdempotencyExecutor;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class DealershipController {
    private final IdentityPort identity;
    private final DealershipService dealership;

    public DealershipController(IdentityPort identity, DealershipService dealership) {
        this.identity = identity;
        this.dealership = dealership;
    }

    private UUID user(Authentication auth) {
        ActorContext actor = identity.requireActiveActor(auth);
        if (actor.baseRole() != BaseRole.USER)
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "该入口仅供普通用户和经销商使用。");
        return actor.actorId();
    }

    private UUID admin(Authentication auth) {
        ActorContext actor = identity.requireActiveActor(auth);
        if (actor.baseRole() != BaseRole.ADMIN)
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "该入口仅供管理员使用。");
        return actor.actorId();
    }

    private <T> ResponseEntity<ApiEnvelope<T>> ok(T value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(value));
    }

    private <T> ResponseEntity<ApiEnvelope<T>> result(IdempotencyExecutor.Result<T> result, int status) {
        return ResponseEntity.status(result.replayed() ? 200 : status)
                .cacheControl(CacheControl.noStore())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(ApiEnvelope.of(result.value()));
    }

    @GetMapping("/channels")
    public ApiEnvelope<PageResult<ChannelView>> publicChannels(
            @RequestParam(required = false) String countryOrRegion,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiEnvelope.of(dealership.publicChannels(countryOrRegion, city, page, pageSize));
    }

    @GetMapping("/channels/{id}")
    public ApiEnvelope<ChannelView> publicChannel(@PathVariable UUID id) {
        return ApiEnvelope.of(dealership.publicChannel(id));
    }

    @PostMapping("/dealer-applications")
    public ResponseEntity<?> createApplication(Authentication auth,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ApplicationRequest request) {
        return result(dealership.createApplication(user(auth), key, request), 201);
    }

    @GetMapping("/dealer-applications")
    public ResponseEntity<?> ownApplications(Authentication auth) {
        return ok(dealership.ownApplications(user(auth)));
    }

    @GetMapping("/dealer-applications/{id}")
    public ResponseEntity<?> ownApplication(Authentication auth, @PathVariable UUID id) {
        return ok(dealership.ownApplication(user(auth), id));
    }

    @PostMapping("/dealer-applications/{id}/resubmit")
    public ResponseEntity<?> resubmit(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ResubmitRequest request) {
        return result(dealership.resubmit(user(auth), id, key, request), 200);
    }

    @GetMapping("/dealer/catalog")
    public ResponseEntity<?> dealerCatalog(Authentication auth) {
        return ok(dealership.dealerCatalog(user(auth), null));
    }

    @GetMapping("/dealer/catalog/{productId}")
    public ResponseEntity<?> dealerProduct(Authentication auth, @PathVariable UUID productId) {
        List<DealerProductView> products = dealership.dealerCatalog(user(auth), List.of(productId));
        if (products.isEmpty()) throw wemove.dealership.service.DealershipRules.notFound();
        return ok(products.getFirst());
    }

    @PostMapping("/inquiries")
    public ResponseEntity<?> createInquiry(Authentication auth,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody InquiryRequest request) {
        return result(dealership.createInquiry(user(auth), key, request), 201);
    }

    @GetMapping("/inquiries")
    public ResponseEntity<?> inquiries(Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ok(dealership.inquiries(user(auth), false, status, page, pageSize));
    }

    @GetMapping("/inquiries/{id}")
    public ResponseEntity<?> inquiry(Authentication auth, @PathVariable UUID id) {
        return ok(dealership.inquiry(user(auth), id, false));
    }

    @PostMapping("/inquiries/{id}/close")
    public ResponseEntity<?> closeInquiry(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionCommand request) {
        return result(dealership.closeInquiry(user(auth), id, key, request, false), 200);
    }

    @GetMapping("/admin/dealer-applications")
    public ResponseEntity<?> adminApplications(Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        admin(auth);
        return ok(dealership.adminApplications(status, page, pageSize));
    }

    @GetMapping("/admin/dealer-applications/{id}")
    public ResponseEntity<?> adminApplication(Authentication auth, @PathVariable UUID id) {
        admin(auth);
        return ok(dealership.adminApplication(id));
    }

    @PostMapping("/admin/dealer-applications/{id}/review")
    public ResponseEntity<?> review(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ReviewRequest request) {
        return result(dealership.review(admin(auth), id, key, request), 200);
    }

    @GetMapping("/admin/inquiries")
    public ResponseEntity<?> adminInquiries(Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        UUID actor = admin(auth);
        return ok(dealership.inquiries(actor, true, status, page, pageSize));
    }

    @GetMapping("/admin/inquiries/{id}")
    public ResponseEntity<?> adminInquiry(Authentication auth, @PathVariable UUID id) {
        return ok(dealership.inquiry(admin(auth), id, true));
    }

    @PostMapping("/admin/inquiries/{id}/start")
    public ResponseEntity<?> startInquiry(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionOnly request) {
        return result(dealership.startInquiry(admin(auth), id, key, request.expectedVersion()), 200);
    }

    @PostMapping("/admin/inquiries/{id}/replies")
    public ResponseEntity<?> replyInquiry(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody InquiryReplyRequest request) {
        return result(dealership.replyInquiry(admin(auth), id, key, request), 200);
    }

    @PostMapping("/admin/inquiries/{id}/close")
    public ResponseEntity<?> adminCloseInquiry(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionCommand request) {
        return result(dealership.closeInquiry(admin(auth), id, key, request, true), 200);
    }

    @GetMapping("/admin/companies")
    public ResponseEntity<?> companies(Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        admin(auth);
        return ok(dealership.companies(status, page, pageSize));
    }

    @GetMapping("/admin/companies/{id}")
    public ResponseEntity<?> company(Authentication auth, @PathVariable UUID id) {
        admin(auth);
        return ok(dealership.company(id));
    }

    @PatchMapping("/admin/companies/{id}")
    public ResponseEntity<?> updateCompany(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody CompanyUpdateRequest request) {
        return ok(dealership.updateCompany(admin(auth), id, request));
    }

    @PostMapping("/admin/companies/{id}/suspend")
    public ResponseEntity<?> suspendCompany(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionCommand request) {
        return result(dealership.companyStatus(admin(auth), id, key, request, false), 200);
    }

    @PostMapping("/admin/companies/{id}/restore")
    public ResponseEntity<?> restoreCompany(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionCommand request) {
        return result(dealership.companyStatus(admin(auth), id, key, request, true), 200);
    }

    @GetMapping("/admin/channels")
    public ResponseEntity<?> adminChannels(Authentication auth,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        admin(auth);
        return ok(dealership.adminChannels(published, page, pageSize));
    }

    @GetMapping("/admin/channels/{id}")
    public ResponseEntity<?> adminChannel(Authentication auth, @PathVariable UUID id) {
        admin(auth);
        return ok(dealership.adminChannel(id));
    }

    @PostMapping("/admin/channels")
    public ResponseEntity<?> createChannel(Authentication auth,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ChannelRequest request) {
        return result(dealership.createChannel(admin(auth), key, request), 201);
    }

    @PatchMapping("/admin/channels/{id}")
    public ResponseEntity<?> updateChannel(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody ChannelRequest request) {
        return ok(dealership.updateChannel(admin(auth), id, request));
    }

    @PostMapping("/admin/channels/{id}/publish")
    public ResponseEntity<?> publishChannel(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionCommand request) {
        return result(dealership.channelStatus(admin(auth), id, key, request, true), 200);
    }

    @PostMapping("/admin/channels/{id}/unpublish")
    public ResponseEntity<?> unpublishChannel(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody VersionCommand request) {
        return result(dealership.channelStatus(admin(auth), id, key, request, false), 200);
    }
}
