package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.*;

import org.springframework.stereotype.Service;

import wemove.catalog.platform.InventoryPort;
import wemove.commerce.api.CommerceDtos.*;
import wemove.commerce.domain.*;
import wemove.commerce.repository.CommerceRepository;
import wemove.platform.idempotency.IdempotencyExecutor;

import java.time.Instant;
import java.util.*;

@Service
public class OrderCommandService {
    @org.springframework.beans.factory.annotation.Autowired
    private wemove.platform.TransactionProbe probe;

    private final CommerceRepository db;
    private final CommerceAccess access;
    private final IdempotencyExecutor commands;
    private final InventoryPort inventory;
    private final OrderJournal journal;
    private final OrderQueryService queries;

    public OrderCommandService(
            CommerceRepository db,
            CommerceAccess access,
            IdempotencyExecutor commands,
            InventoryPort inventory,
            OrderJournal journal,
            OrderQueryService queries) {
        this.db = db;
        this.access = access;
        this.commands = commands;
        this.inventory = inventory;
        this.journal = journal;
        this.queries = queries;
    }

    public IdempotencyExecutor.Result<Detail> payment(
            UUID actor, UUID id, UUID key, Payment request) {
        return execute(
                actor,
                id,
                key,
                "mockPayment",
                "mock-payments",
                request,
                request.expectedVersion(),
                false,
                o -> {
                    state(o.status, "PENDING_PAYMENT");
                    if (request.outcome() == Outcome.SUCCESS) {
                        inventory.deductForPayment(o.id, items(o));
                        o.status = "PAID";
                        o.paidAt = Instant.now();
                    }
                    PaymentAttempt p = new PaymentAttempt();
                    p.id = UUID.randomUUID();
                    p.orderId = o.id;
                    p.outcome = request.outcome().name();
                    p.amountFen = o.totalFen;
                    p.simulationReference = "SIM-PAY-" + p.id;
                    p.actorId = actor;
                    p.createdAt = Instant.now();
                    p.successOrderId = request.outcome() == Outcome.SUCCESS ? o.id : null;
                    db.save(p);
                    db.flush();
                    probe.hit("payment.created");
                    return request.outcome() == Outcome.SUCCESS ? "模拟付款成功" : "模拟付款失败";
                });
    }

    public IdempotencyExecutor.Result<Detail> cancel(
            UUID actor, UUID id, UUID key, Cancel request, boolean admin) {
        String reason = text("reason", request.reason(), 2, 500);
        return execute(
                actor,
                id,
                key,
                admin ? "cancelAdminOrder" : "cancelOwnOrder",
                "cancel",
                new Cancel(request.expectedVersion(), reason),
                request.expectedVersion(),
                admin,
                o -> {
                    state(o.status, "PENDING_PAYMENT", "PAID");
                    if (o.status.equals("PAID")) {
                        PaymentAttempt p =
                                db.attempts(o.id).stream()
                                        .filter(a -> a.outcome.equals("SUCCESS"))
                                        .findFirst()
                                        .orElseThrow(
                                                () ->
                                                        new IllegalStateException(
                                                                "Paid order has no successful"
                                                                        + " payment"));
                        if (p.amountFen != o.totalFen)
                            throw new IllegalStateException(
                                    "Payment amount disagrees with snapshot");
                        inventory.restoreForCancellation(o.id, items(o));
                        Refund r = new Refund();
                        r.id = UUID.randomUUID();
                        r.orderId = o.id;
                        r.paymentAttemptId = p.id;
                        r.amountFen = p.amountFen;
                        r.simulationReference = "SIM-REFUND-" + r.id;
                        r.actorId = actor;
                        r.reason = reason;
                        r.createdAt = Instant.now();
                        db.save(r);
                        db.flush();
                        probe.hit("refund.created");
                    }
                    o.status = "CANCELLED";
                    o.cancelledAt = Instant.now();
                    return reason;
                });
    }

    public IdempotencyExecutor.Result<Detail> shipment(
            UUID actor, UUID id, UUID key, Shipment request) {
        String name = text("logisticsName", request.logisticsName(), 2, 50),
                tracking = text("trackingNumber", request.trackingNumber(), 3, 50);
        if (!tracking.matches("[A-Za-z0-9-]+")) throw invalid("trackingNumber", "运单号仅限字母、数字和连字符。");
        return execute(
                actor,
                id,
                key,
                "mockShipment",
                "mock-shipment",
                new Shipment(request.expectedVersion(), name, tracking),
                request.expectedVersion(),
                true,
                o -> {
                    state(o.status, "PAID");
                    o.status = "SHIPPED";
                    o.logisticsName = name;
                    o.trackingNumber = tracking;
                    o.shippedBy = actor;
                    o.shippedAt = Instant.now();
                    return null;
                });
    }

    public IdempotencyExecutor.Result<Detail> receipt(
            UUID actor, UUID id, UUID key, Receipt request) {
        return execute(
                actor,
                id,
                key,
                "confirmReceipt",
                "confirm-receipt",
                request,
                request.expectedVersion(),
                false,
                o -> {
                    state(o.status, "SHIPPED");
                    o.status = "COMPLETED";
                    o.completedAt = Instant.now();
                    return null;
                });
    }

    private IdempotencyExecutor.Result<Detail> execute(
            UUID actor,
            UUID id,
            UUID key,
            String operation,
            String path,
            Object request,
            long expected,
            boolean admin,
            java.util.function.Function<Order, String> change) {
        return commands.execute(
                actor,
                "commerce." + operation,
                key,
                "/api/v1/" + (admin ? "admin/" : "") + "orders/" + id + "/" + path,
                request,
                Detail.class,
                200,
                null,
                () -> {
                    access.lock(actor, admin);
                    Order order = db.order(id, true);
                    CommerceAccess.owned(order, actor, admin);
                    version(order.version, expected);
                    String previous = order.status;
                    String reason = change.apply(order);
                    order.version = Math.incrementExact(order.version);
                    journal.append(order, actor, operation, previous, reason);
                    db.flush();
                    return queries.detail(order, admin);
                });
    }

    private List<InventoryPort.InventoryItem> items(Order order) {
        return db.items(order.id).stream()
                .map(i -> new InventoryPort.InventoryItem(i.productId, i.quantity))
                .toList();
    }
}
