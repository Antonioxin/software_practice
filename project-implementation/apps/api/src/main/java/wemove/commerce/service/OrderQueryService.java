package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import wemove.commerce.api.CommerceDtos.*;
import wemove.commerce.domain.Order;
import wemove.commerce.repository.CommerceRepository;

import java.time.Instant;
import java.util.*;

@Service
public class OrderQueryService {
    private final CommerceRepository db;

    public OrderQueryService(CommerceRepository db) {
        this.db = db;
    }

    @Transactional(readOnly = true)
    public OrderPage list(
            UUID actor,
            boolean admin,
            String status,
            Instant start,
            Instant end,
            int page,
            int pageSize) {
        if (page < 1 || page > 1000000 || pageSize < 1 || pageSize > 50)
            throw invalid("page", "page需为1—1000000，pageSize为1—50。");
        if (status != null
                && !Set.of("PENDING_PAYMENT", "PAID", "CANCELLED", "SHIPPED", "COMPLETED")
                        .contains(status)) throw invalid("status", "订单状态无效。");
        if (start != null && end != null && !start.isBefore(end))
            throw invalid("start", "开始时间必须早于结束时间。");
        UUID user = admin ? null : actor;
        return new OrderPage(
                db.list(user, status, start, end, (page - 1) * pageSize, pageSize).stream()
                        .map(this::summary)
                        .toList(),
                page,
                pageSize,
                db.count(user, status, start, end));
    }

    @Transactional(readOnly = true)
    public Detail read(UUID actor, UUID id, boolean admin) {
        Order order = db.order(id, false);
        CommerceAccess.owned(order, actor, admin);
        return detail(order, admin);
    }

    public Summary summary(Order o) {
        return new Summary(
                o.id,
                o.orderNumber,
                o.status,
                o.version,
                o.currency,
                o.totalFen,
                o.mode,
                o.createdAt);
    }

    public Detail detail(Order o, boolean admin) {
        List<String> actions = new ArrayList<>();
        if (o.status.equals("PENDING_PAYMENT")) {
            actions.add("CANCEL");
            if (!admin) actions.add("MOCK_PAYMENT");
        }
        if (o.status.equals("PAID")) {
            actions.add("CANCEL");
            if (admin) actions.add("MOCK_SHIPMENT");
        }
        if (o.status.equals("SHIPPED") && !admin) actions.add("CONFIRM_RECEIPT");
        return new Detail(
                o.id,
                o.orderNumber,
                o.status,
                o.version,
                o.currency,
                o.totalFen,
                o.mode,
                o.createdAt,
                o.subtotalFen,
                o.shippingFen,
                o.taxFen,
                o.discountFen,
                new ShippingAddress(
                        o.recipient, o.phone, o.countryOrRegion, o.region, o.city, o.addressLine),
                o.remark,
                db.items(o.id).stream()
                        .map(
                                i ->
                                        new Line(
                                                i.productId,
                                                i.sku,
                                                i.name,
                                                i.unitPriceFen,
                                                i.quantity,
                                                i.subtotalFen,
                                                true,
                                                null,
                                                false,
                                                i.unitPriceFen))
                        .toList(),
                db.attempts(o.id).stream()
                        .map(
                                p ->
                                        new Attempt(
                                                p.id,
                                                p.outcome,
                                                p.amountFen,
                                                p.simulationReference,
                                                p.createdAt,
                                                p.mode))
                        .toList(),
                db.refunds(o.id).stream()
                        .map(
                                r ->
                                        new RefundView(
                                                r.id,
                                                r.amountFen,
                                                r.simulationReference,
                                                r.createdAt,
                                                r.reason,
                                                r.mode))
                        .toList(),
                db.history(o.id).stream()
                        .map(
                                h ->
                                        new History(
                                                h.action,
                                                h.fromStatus,
                                                h.toStatus,
                                                h.orderVersion,
                                                h.reason,
                                                h.createdAt))
                        .toList(),
                actions,
                o.logisticsName,
                o.trackingNumber,
                o.paidAt,
                o.shippedAt,
                o.completedAt);
    }
}
