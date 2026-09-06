package wemove.commerce.api;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import wemove.commerce.api.CommerceDtos.*;
import wemove.commerce.service.*;
import wemove.platform.*;
import wemove.platform.api.ApiEnvelope;
import wemove.platform.idempotency.IdempotencyExecutor;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class CommerceController {
    private final IdentityPort identity;
    private final CartService carts;
    private final CheckoutService checkout;
    private final OrderQueryService orders;
    private final OrderCommandService commands;

    public CommerceController(
            IdentityPort identity,
            CartService carts,
            CheckoutService checkout,
            OrderQueryService orders,
            OrderCommandService commands) {
        this.identity = identity;
        this.carts = carts;
        this.checkout = checkout;
        this.orders = orders;
        this.commands = commands;
    }

    private UUID actor(Authentication auth, boolean admin) {
        var actor = identity.requireActiveActor(auth);
        CommerceAccess.requireRole(actor, admin);
        return actor.actorId();
    }

    private <T> ResponseEntity<ApiEnvelope<T>> ok(T value) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(value));
    }

    private <T> ResponseEntity<ApiEnvelope<T>> result(
            IdempotencyExecutor.Result<T> result, int status) {
        return ResponseEntity.status(result.replayed() ? 200 : status)
                .cacheControl(CacheControl.noStore())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(ApiEnvelope.of(result.value()));
    }

    @GetMapping("/cart")
    public ResponseEntity<?> cart(Authentication a) {
        return ok(carts.read(actor(a, false)));
    }

    @PostMapping("/cart/items")
    public ResponseEntity<?> add(
            Authentication a,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AddItem r) {
        return result(carts.add(actor(a, false), key, r), 200);
    }

    @PatchMapping("/cart/items/{id}")
    public ResponseEntity<?> update(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody UpdateItem r) {
        return ok(carts.update(actor(a, false), id, r));
    }

    @DeleteMapping("/cart/items/{id}")
    public ResponseEntity<?> delete(
            Authentication a, @PathVariable UUID id, @RequestParam long cartVersion) {
        return ok(carts.delete(actor(a, false), id, cartVersion));
    }

    @DeleteMapping("/cart/items")
    public ResponseEntity<?> clear(Authentication a, @RequestParam long cartVersion) {
        return ok(carts.delete(actor(a, false), null, cartVersion));
    }

    @PostMapping("/checkout-previews")
    public ResponseEntity<?> preview(
            Authentication a, @RequestBody(required = false) Map<String, Object> body) {
        if (body != null && !body.isEmpty())
            throw wemove.commerce.domain.CommerceRules.invalid("body", "整车预览不接受选择项。");
        return ok(checkout.preview(actor(a, false)));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> create(
            Authentication a,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CreateOrder r) {
        return result(checkout.create(actor(a, false), key, r), 201);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> list(
            Authentication a,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ok(orders.list(actor(a, false), false, status, null, null, page, pageSize));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> detail(Authentication a, @PathVariable UUID id) {
        return ok(orders.read(actor(a, false), id, false));
    }

    @PostMapping("/orders/{id}/mock-payments")
    public ResponseEntity<?> pay(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody Payment r) {
        return result(commands.payment(actor(a, false), id, key, r), 200);
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancel(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody Cancel r) {
        return result(commands.cancel(actor(a, false), id, key, r, false), 200);
    }

    @PostMapping("/orders/{id}/confirm-receipt")
    public ResponseEntity<?> receipt(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody Receipt r) {
        return result(commands.receipt(actor(a, false), id, key, r), 200);
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<?> adminList(
            Authentication a,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ok(orders.list(actor(a, true), true, status, start, end, page, pageSize));
    }

    @GetMapping("/admin/orders/{id}")
    public ResponseEntity<?> adminDetail(Authentication a, @PathVariable UUID id) {
        return ok(orders.read(actor(a, true), id, true));
    }

    @PostMapping("/admin/orders/{id}/cancel")
    public ResponseEntity<?> adminCancel(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody Cancel r) {
        return result(commands.cancel(actor(a, true), id, key, r, true), 200);
    }

    @PostMapping("/admin/orders/{id}/mock-shipment")
    public ResponseEntity<?> shipment(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody Shipment r) {
        return result(commands.shipment(actor(a, true), id, key, r), 200);
    }
}
