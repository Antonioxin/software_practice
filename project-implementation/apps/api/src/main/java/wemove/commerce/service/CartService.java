package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import wemove.catalog.platform.CatalogPort;
import wemove.commerce.api.CommerceDtos.*;
import wemove.commerce.domain.*;
import wemove.commerce.repository.CommerceRepository;
import wemove.platform.UnitOfWork;
import wemove.platform.idempotency.IdempotencyExecutor;

import java.time.Instant;
import java.util.*;

@Service
public class CartService {
    private final CommerceRepository db;
    private final CatalogPort catalog;
    private final CommerceAccess access;
    private final UnitOfWork work;
    private final IdempotencyExecutor commands;

    public CartService(
            CommerceRepository db,
            CatalogPort catalog,
            CommerceAccess access,
            UnitOfWork work,
            IdempotencyExecutor commands) {
        this.db = db;
        this.catalog = catalog;
        this.access = access;
        this.work = work;
        this.commands = commands;
    }

    @Transactional(readOnly = true)
    public CartView read(UUID actor) {
        return view(db.cart(actor, false), false);
    }

    public IdempotencyExecutor.Result<CartView> add(UUID actor, UUID key, AddItem request) {
        return commands.execute(
                actor,
                "commerce.addCartItem",
                key,
                "/api/v1/cart/items",
                request,
                CartView.class,
                200,
                null,
                () -> {
                    access.lock(actor, false);
                    Cart cart = lockedCart(actor);
                    var items = db.cartItems(cart.id);
                    CartItem item =
                            items.stream()
                                    .filter(i -> i.productId.equals(request.productId()))
                                    .findFirst()
                                    .orElse(null);
                    int quantity = request.quantity() + (item == null ? 0 : item.quantity);
                    if (quantity > 99) throw invalid("quantity", "同商品最多99件。");
                    if (item == null && items.size() >= 20) throw invalid("items", "购物车最多20种商品。");
                    var snapshot =
                            catalog.lockRetailSnapshot(
                                            List.of(
                                                    new CatalogPort.RequestedItem(
                                                            request.productId(), quantity)))
                                    .getFirst();
                    validate(snapshot);
                    boolean created = item == null;
                    if (created) {
                        item = new CartItem();
                        item.id = UUID.randomUUID();
                        item.cartId = cart.id;
                        item.productId = request.productId();
                    }
                    item.quantity = quantity;
                    item.lastConfirmedUnitPriceFen = snapshot.retailUnitPriceFen();
                    if (created) db.save(item);
                    touch(cart);
                    db.flush();
                    return view(cart, false);
                });
    }

    public CartView update(UUID actor, UUID product, UpdateItem request) {
        return work.run(
                () -> {
                    access.lock(actor, false);
                    Cart cart = lockedCart(actor);
                    checkVersion(cart, request.cartVersion());
                    CartItem item =
                            db.cartItems(cart.id).stream()
                                    .filter(i -> i.productId.equals(product))
                                    .findFirst()
                                    .orElseThrow(CommerceRules::notFound);
                    var snapshot =
                            catalog.lockRetailSnapshot(
                                            List.of(
                                                    new CatalogPort.RequestedItem(
                                                            product, request.quantity())))
                                    .getFirst();
                    validate(snapshot);
                    if (item.quantity != request.quantity()
                            || item.lastConfirmedUnitPriceFen != snapshot.retailUnitPriceFen()) {
                        item.quantity = request.quantity();
                        item.lastConfirmedUnitPriceFen = snapshot.retailUnitPriceFen();
                        touch(cart);
                    }
                    db.flush();
                    return view(cart, false);
                });
    }

    public CartView delete(UUID actor, UUID product, long version) {
        if (version < 1) throw invalid("cartVersion", "版本从1开始。");
        return work.run(
                () -> {
                    access.lock(actor, false);
                    Cart cart = db.cart(actor, true);
                    if (cart == null) {
                        if (version != 1) throw conflict("CART_CHANGED", "购物车已变更。");
                        return view(null, false);
                    }
                    checkVersion(cart, version);
                    var removed =
                            db.cartItems(cart.id).stream()
                                    .filter(i -> product == null || i.productId.equals(product))
                                    .toList();
                    if (!removed.isEmpty()) {
                        removed.forEach(db::remove);
                        touch(cart);
                    }
                    db.flush();
                    return view(cart, false);
                });
    }

    Cart lockedCart(UUID actor) {
        Cart cart = db.cart(actor, true);
        if (cart == null) {
            cart = new Cart();
            cart.id = UUID.randomUUID();
            cart.userId = actor;
            cart.updatedAt = Instant.now();
            db.save(cart);
            db.flush();
        }
        return cart;
    }

    CartView view(Cart cart, boolean protectedRead) {
        if (cart == null) return new CartView(1, List.of(), 0, false, "CNY");
        var items = db.cartItems(cart.id);
        var requested =
                items.stream()
                        .map(i -> new CatalogPort.RequestedItem(i.productId, i.quantity))
                        .toList();
        var snapshots =
                protectedRead
                        ? catalog.lockRetailSnapshot(requested)
                        : catalog.getRetailSnapshot(requested);
        Map<UUID, CartItem> previous = new HashMap<>();
        items.forEach(i -> previous.put(i.productId, i));
        List<Line> lines = new ArrayList<>();
        long total = 0;
        for (var p : snapshots) {
            boolean valid =
                    p.published()
                            && p.availableQuantity() >= p.requestedQuantity()
                            && p.retailUnitPriceFen() > 0;
            String reason =
                    !p.published()
                            ? "商品已下架"
                            : p.availableQuantity() < p.requestedQuantity() ? "库存不足" : null;
            long subtotal =
                    p.retailUnitPriceFen() > 0
                            ? subtotal(p.retailUnitPriceFen(), p.requestedQuantity())
                            : 0;
            long old = previous.get(p.id()).lastConfirmedUnitPriceFen;
            lines.add(
                    new Line(
                            p.id(),
                            p.sku(),
                            p.name(),
                            p.retailUnitPriceFen(),
                            p.requestedQuantity(),
                            subtotal,
                            valid,
                            reason,
                            old != p.retailUnitPriceFen(),
                            old));
            total = Math.addExact(total, subtotal);
        }
        return new CartView(
                cart.version,
                lines,
                total,
                !lines.isEmpty() && lines.stream().allMatch(Line::valid),
                "CNY");
    }

    static void validate(CatalogPort.RetailProductSnapshot p) {
        if (!p.published() || p.retailUnitPriceFen() < 1)
            throw conflict("PRODUCT_UNAVAILABLE", "商品不存在或已下架。");
        if (p.availableQuantity() < p.requestedQuantity())
            throw conflict("INSUFFICIENT_STOCK", "库存不足。");
    }

    static void checkVersion(Cart cart, long version) {
        if (cart.version != version) throw conflict("CART_CHANGED", "购物车已变更，请重新预览。");
    }

    static void touch(Cart cart) {
        cart.version = Math.incrementExact(cart.version);
        cart.updatedAt = Instant.now();
    }
}
