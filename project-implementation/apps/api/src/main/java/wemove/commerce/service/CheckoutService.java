package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import wemove.catalog.platform.CatalogPort;
import wemove.commerce.api.CommerceDtos.*;
import wemove.commerce.domain.*;
import wemove.commerce.repository.CommerceRepository;
import wemove.platform.*;
import wemove.platform.idempotency.IdempotencyExecutor;

import java.time.*;
import java.util.*;

@Service
public class CheckoutService {
    @org.springframework.beans.factory.annotation.Autowired private TransactionProbe probe;
    private final CommerceRepository db;
    private final CartService carts;
    private final CatalogPort catalog;
    private final CommerceAccess access;
    private final UnitOfWork work;
    private final IdempotencyExecutor commands;
    private final ObjectMapper mapper;
    private final OrderQueryService queries;
    private final OrderJournal journal;

    public CheckoutService(
            CommerceRepository db,
            CartService carts,
            CatalogPort catalog,
            CommerceAccess access,
            UnitOfWork work,
            IdempotencyExecutor commands,
            ObjectMapper mapper,
            OrderQueryService queries,
            OrderJournal journal) {
        this.db = db;
        this.carts = carts;
        this.catalog = catalog;
        this.access = access;
        this.work = work;
        this.commands = commands;
        this.mapper = mapper;
        this.queries = queries;
        this.journal = journal;
    }

    public PreviewView preview(UUID actor) {
        return work.run(
                () -> {
                    access.lock(actor, false);
                    Cart cart = db.cart(actor, true);
                    CartView view = carts.view(cart, true);
                    if (view.items().isEmpty()) throw invalid("cart", "购物车为空。");
                    if (!view.canCheckout())
                        throw conflict("PRODUCT_UNAVAILABLE", "请移除不可售商品或调整数量。");
                    String token = UUID.randomUUID() + "." + UUID.randomUUID();
                    Instant now = Instant.now();
                    PreviewView result =
                            new PreviewView(
                                    token,
                                    cart.version,
                                    now.plusSeconds(900),
                                    "CNY",
                                    view.items(),
                                    view.totalFen(),
                                    0,
                                    0,
                                    0,
                                    view.totalFen());
                    CheckoutPreview p = new CheckoutPreview();
                    p.id = UUID.randomUUID();
                    p.tokenHash = commands.hash(token);
                    p.userId = actor;
                    p.cartId = cart.id;
                    p.cartVersion = cart.version;
                    p.createdAt = now;
                    p.expiresAt = result.expiresAt();
                    // Persist only the token digest, never the bearer token itself.
                    try {
                        p.snapshotJson =
                                mapper.writeValueAsString(
                                        new PreviewView(
                                                null,
                                                result.cartVersion(),
                                                result.expiresAt(),
                                                "CNY",
                                                result.items(),
                                                result.subtotalFen(),
                                                0,
                                                0,
                                                0,
                                                result.totalFen()));
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                    db.save(p);
                    db.flush();
                    return result;
                });
    }

    public IdempotencyExecutor.Result<Summary> create(UUID actor, UUID key, CreateOrder request) {
        ShippingAddress address = address(request.shippingAddress());
        String remark = request.remark() == null ? null : text("remark", request.remark(), 0, 2000);
        CreateOrder normalized =
                new CreateOrder(
                        request.previewToken(),
                        request.cartVersion(),
                        address,
                        remark,
                        request.clientTotalFen());
        return commands.execute(
                actor,
                "commerce.createOrder",
                key,
                "/api/v1/orders",
                normalized,
                Summary.class,
                201,
                null,
                () -> {
                    access.lock(actor, false);
                    Cart cart = db.cart(actor, true);
                    CheckoutPreview preview =
                            db.preview(commands.hash(request.previewToken()), actor);
                    if (!preview.expiresAt.isAfter(Instant.now()))
                        throw conflict("CHECKOUT_PREVIEW_EXPIRED", "预览已过期，请重新确认。");
                    if (db.consumed(preview.id))
                        throw conflict("STATE_CONFLICT", "该预览已创建订单，请查询订单列表。");
                    if (cart == null
                            || !cart.id.equals(preview.cartId)
                            || cart.version != request.cartVersion()
                            || cart.version != preview.cartVersion)
                        throw conflict("CART_CHANGED", "购物车已变更，请重新预览。");
                    PreviewView saved;
                    try {
                        saved = mapper.readValue(preview.snapshotJson, PreviewView.class);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                    var items = db.cartItems(cart.id);
                    Map<UUID, Integer> current = new HashMap<>();
                    items.forEach(i -> current.put(i.productId, i.quantity));
                    if (current.size() != saved.items().size()
                            || saved.items().stream()
                                    .anyMatch(
                                            i ->
                                                    !Objects.equals(
                                                            current.get(i.productId()),
                                                            i.quantity())))
                        throw conflict("CART_CHANGED", "购物车内容已变更。");
                    var snapshots =
                            catalog.lockRetailSnapshot(
                                    items.stream()
                                            .map(
                                                    i ->
                                                            new CatalogPort.RequestedItem(
                                                                    i.productId, i.quantity))
                                            .toList());
                    Map<UUID, Line> old = new HashMap<>();
                    saved.items().forEach(i -> old.put(i.productId(), i));
                    long total = 0;
                    for (var p : snapshots) {
                        CartService.validate(p);
                        if (p.retailUnitPriceFen() != old.get(p.id()).unitPriceFen())
                            throw conflict("PRICE_CHANGED", "零售价已变更，请重新预览确认。");
                        total =
                                Math.addExact(
                                        total,
                                        subtotal(p.retailUnitPriceFen(), p.requestedQuantity()));
                    }
                    Order order = new Order();
                    order.id = UUID.randomUUID();
                    order.orderNumber =
                            "WM" + order.id.toString().replace("-", "").toUpperCase(Locale.ROOT);
                    order.userId = actor;
                    order.previewId = preview.id;
                    order.status = "PENDING_PAYMENT";
                    order.subtotalFen = total;
                    order.totalFen = total;
                    order.createdAt = Instant.now();
                    order.recipient = address.recipient();
                    order.phone = address.phone();
                    order.countryOrRegion = address.countryOrRegion();
                    order.region = address.region();
                    order.city = address.city();
                    order.addressLine = address.addressLine();
                    order.remark = remark;
                    db.save(order);
                    db.flush();
                    probe.hit("order.created");
                    for (var p : snapshots) {
                        OrderItem i = new OrderItem();
                        i.id = UUID.randomUUID();
                        i.orderId = order.id;
                        i.productId = p.id();
                        i.sku = p.sku();
                        i.name = p.name();
                        i.unitPriceFen = p.retailUnitPriceFen();
                        i.quantity = p.requestedQuantity();
                        i.subtotalFen = subtotal(i.unitPriceFen, i.quantity);
                        db.save(i);
                    }
                    items.forEach(db::remove);
                    CartService.touch(cart);
                    db.flush();
                    probe.hit("cart.cleared");
                    journal.append(order, actor, "CREATE", null, null);
                    db.flush();
                    return queries.summary(order);
                });
    }
}
