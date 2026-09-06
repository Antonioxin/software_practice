package wemove.commerce;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import wemove.catalog.api.CatalogDtos;
import wemove.catalog.domain.*;
import wemove.catalog.platform.*;
import wemove.catalog.service.InventoryService;
import wemove.commerce.api.CommerceDtos.*;
import wemove.commerce.platform.*;
import wemove.commerce.service.*;
import wemove.identity.domain.*;
import wemove.identity.repository.UserRepository;
import wemove.identity.security.UserPrincipal;
import wemove.platform.*;
import wemove.platform.api.ApiException;
import wemove.platform.idempotency.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Opt-in only: MYSQL_TEST_URL must name the dedicated commerce_test database. Never H2. */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql:.*commerce_test.*")
@SpringBootTest(
        properties = {
            "spring.datasource.url=${MYSQL_TEST_URL}",
            "spring.datasource.username=root",
            "spring.datasource.password=${MYSQL_TEST_PASSWORD}",
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=2"
        })
@AutoConfigureMockMvc
class CommerceMySqlTest {
    @Autowired CartService carts;
    @Autowired CheckoutService checkout;
    @Autowired OrderCommandService commands;
    @Autowired OrderQueryService queries;
    @Autowired InventoryPort inventory;
    @Autowired InventoryService stock;
    @Autowired CatalogPort catalog;
    @Autowired CommerceMetricsPort metrics;
    @Autowired OrdersPort references;
    @Autowired UserRepository users;
    @Autowired IdentityPort identity;
    @Autowired UnitOfWork work;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired ObjectMapper mapper;
    @Autowired IdempotencyExecutor executor;
    @Autowired IdempotencyRecordRepository records;
    @Autowired MockMvc mvc;
    @MockitoSpyBean TransactionProbe probe;
    UUID user, admin;
    static final ShippingAddress ADDRESS =
            new ShippingAddress("测试收件人", "+86 138-0000-0000", "中国", null, "上海", "测试路123号");

    @BeforeEach
    void setup() {
        reset(probe);
        user = user(BaseRole.USER);
        admin = user(BaseRole.ADMIN);
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("8.");
    }

    @AfterEach
    void resetProbe() {
        reset(probe);
    }

    UUID user(BaseRole role) {
        return work.run(
                () ->
                        users.saveAndFlush(
                                        UserEntity.create(
                                                UUID.randomUUID() + "@example.test",
                                                UUID.randomUUID() + "@example.test",
                                                "unused-hash",
                                                "测试账户",
                                                role,
                                                Instant.now()))
                                .getId());
    }

    UUID product(long price, int quantity) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into"
                    + " catalog_products(id,sku,name,category_id,summary,age_min,play_type,scene,main_image_id,retail_unit_price_fen,dealer_enabled,status,display_order,version,created_at,updated_at)"
                    + " values(UUID_TO_BIN(?),?,'测试商品',UUID_TO_BIN('10000000-0000-0000-0000-000000000102'),'合成交易测试商品',4,'THROWING','BOTH','seed-img-ring-toss',?,false,'PUBLISHED',0,0,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))",
                id.toString(),
                "T-" + id.toString().substring(0, 30),
                price);
        jdbc.update(
                "insert into inventory_balances(product_id,quantity,version,updated_at)"
                        + " values(UUID_TO_BIN(?),?,0,UTC_TIMESTAMP(6))",
                id.toString(),
                quantity);
        return id;
    }

    int quantity(UUID p) {
        return jdbc.queryForObject(
                "select quantity from inventory_balances where product_id=UUID_TO_BIN(?)",
                Integer.class,
                p.toString());
    }

    CreateOrder request(PreviewView p) {
        return new CreateOrder(p.previewToken(), p.cartVersion(), ADDRESS, "测试备注", 100L);
    }

    Summary order(UUID actor, UUID product, int quantity) {
        carts.add(actor, UUID.randomUUID(), new AddItem(product, quantity));
        return checkout.create(actor, UUID.randomUUID(), request(checkout.preview(actor))).value();
    }

    Detail pay(UUID actor, Summary o) {
        return commands.payment(
                        actor, o.id(), UUID.randomUUID(), new Payment(o.version(), Outcome.SUCCESS))
                .value();
    }

    long count(String table, UUID order) {
        return jdbc.queryForObject(
                "select count(*) from " + table + " where order_id=UUID_TO_BIN(?)",
                Long.class,
                order.toString());
    }

    List<Object> race(Supplier<?> left, Supplier<?> right) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Object> a =
                    () -> {
                        barrier.await(5, TimeUnit.SECONDS);
                        try {
                            return left.get();
                        } catch (ApiException e) {
                            return e.getCode();
                        }
                    };
            Callable<Object> b =
                    () -> {
                        barrier.await(5, TimeUnit.SECONDS);
                        try {
                            return right.get();
                        } catch (ApiException e) {
                            return e.getCode();
                        }
                    };
            var f = pool.submit(a);
            var g = pool.submit(b);
            return List.of(f.get(20, TimeUnit.SECONDS), g.get(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void exactSnapshotPriceReplayAndCompleteLifecycle() {
        UUID p = product(7435, 10);
        Summary o = order(user, p, 2);
        assertThat(o.totalFen()).isEqualTo(14870);
        assertThat(quantity(p)).isEqualTo(10);
        assertThat(carts.read(user).items()).isEmpty();
        jdbc.update(
                "update catalog_products set retail_unit_price_fen=9999 where id=UUID_TO_BIN(?)",
                p.toString());
        Detail paid = pay(user, o);
        assertThat(paid.totalFen()).isEqualTo(14870);
        assertThat(quantity(p)).isEqualTo(8);
        Detail shipped =
                commands.shipment(
                                admin,
                                o.id(),
                                UUID.randomUUID(),
                                new Shipment(paid.version(), "模拟物流", "SIM-123"))
                        .value();
        Detail done =
                commands.receipt(user, o.id(), UUID.randomUUID(), new Receipt(shipped.version()))
                        .value();
        assertThat(done.status()).isEqualTo("COMPLETED");
        assertThat(done.allowedActions()).isEmpty();
        assertThat(count("commerce_order_history", o.id())).isEqualTo(4);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from operations_audit_records where"
                                        + " object_id=UUID_TO_BIN(?)",
                                Long.class,
                                o.id().toString()))
                .isEqualTo(4);
    }

    @Test
    void paidCancellationRestoresExactlyAndRefundsOnce() {
        UUID p = product(100, 4);
        Summary o = order(user, p, 2);
        Detail paid = pay(user, o);
        jdbc.update(
                "update catalog_products set status='UNLISTED' where id=UUID_TO_BIN(?)",
                p.toString());
        UUID key = UUID.randomUUID();
        Cancel r = new Cancel(paid.version(), "测试取消");
        var first = commands.cancel(user, o.id(), key, r, false);
        var replay = commands.cancel(user, o.id(), key, r, false);
        assertThat(first.value().status()).isEqualTo("CANCELLED");
        assertThat(replay.replayed()).isTrue();
        assertThat(quantity(p)).isEqualTo(4);
        assertThat(count("commerce_refunds", o.id())).isEqualTo(1);
        assertThat(first.value().refunds().getFirst().amountFen()).isEqualTo(200);
        assertThatThrownBy(
                        () ->
                                commands.cancel(
                                        user,
                                        o.id(),
                                        UUID.randomUUID(),
                                        new Cancel(first.value().version(), "再次取消"),
                                        false))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void fullEffectValidationRejectsWrongQuantityOmittedAndExtraItems() {
        UUID a = product(100, 5),
                b = product(100, 5),
                c = product(100, 5),
                source = UUID.randomUUID();
        var items =
                List.of(
                        new InventoryPort.InventoryItem(a, 1),
                        new InventoryPort.InventoryItem(b, 2));
        work.run(
                () -> {
                    inventory.deductForPayment(source, items);
                    return null;
                });
        for (var wrong :
                List.of(
                        List.of(
                                new InventoryPort.InventoryItem(a, 2),
                                new InventoryPort.InventoryItem(b, 2)),
                        List.of(new InventoryPort.InventoryItem(a, 1)),
                        List.of(
                                new InventoryPort.InventoryItem(a, 1),
                                new InventoryPort.InventoryItem(b, 2),
                                new InventoryPort.InventoryItem(c, 1)))) {
            assertThatThrownBy(
                            () ->
                                    work.run(
                                            () -> {
                                                inventory.restoreForCancellation(source, wrong);
                                                return null;
                                            }))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(
                            () ->
                                    work.run(
                                            () -> {
                                                inventory.deductForPayment(source, wrong);
                                                return null;
                                            }))
                    .isInstanceOf(ApiException.class);
        }
        assertThat(quantity(a)).isEqualTo(4);
        assertThat(quantity(b)).isEqualTo(3);
        work.run(
                () -> {
                    inventory.restoreForCancellation(source, items);
                    return null;
                });
        assertThat(quantity(a)).isEqualTo(5);
        assertThat(quantity(b)).isEqualTo(5);
    }

    @Test
    void staleManagedProductIsRefreshedByProtectedSnapshot() {
        UUID p = product(100, 5);
        work.run(
                () -> {
                    assertThat(
                                    catalog.getRetailSnapshot(
                                                    List.of(new CatalogPort.RequestedItem(p, 1)))
                                            .getFirst()
                                            .retailUnitPriceFen())
                            .isEqualTo(100);
                    CompletableFuture.runAsync(
                                    () ->
                                            jdbc.update(
                                                    "update catalog_products set"
                                                            + " retail_unit_price_fen=222 where"
                                                            + " id=UUID_TO_BIN(?)",
                                                    p.toString()))
                            .join();
                    assertThat(
                                    catalog.lockRetailSnapshot(
                                                    List.of(new CatalogPort.RequestedItem(p, 1)))
                                            .getFirst()
                                            .retailUnitPriceFen())
                            .isEqualTo(222);
                    return null;
                });
        assertThatThrownBy(
                        () ->
                                catalog.lockRetailSnapshot(
                                        List.of(new CatalogPort.RequestedItem(p, 1))))
                .isInstanceOf(
                        org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void completedCreateReplaysAfterExpiryAndCartChange() {
        UUID p = product(100, 5);
        carts.add(user, UUID.randomUUID(), new AddItem(p, 1));
        PreviewView preview = checkout.preview(user);
        UUID key = UUID.randomUUID();
        CreateOrder r = request(preview);
        Summary first = checkout.create(user, key, r).value();
        jdbc.update(
                "update commerce_checkout_previews set"
                        + " expires_at=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 DAY) where"
                        + " user_id=UUID_TO_BIN(?)",
                user.toString());
        carts.add(user, UUID.randomUUID(), new AddItem(p, 1));
        var replay = checkout.create(user, key, r);
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.value().id()).isEqualTo(first.id());
        assertThat(carts.read(user).items()).hasSize(1);
    }

    @Test
    void priceChangeAndFailedPaymentLeaveNoPartialEffects() {
        UUID p = product(100, 5);
        carts.add(user, UUID.randomUUID(), new AddItem(p, 1));
        PreviewView preview = checkout.preview(user);
        jdbc.update(
                "update catalog_products set retail_unit_price_fen=200 where id=UUID_TO_BIN(?)",
                p.toString());
        assertThatThrownBy(() -> checkout.create(user, UUID.randomUUID(), request(preview)))
                .isInstanceOf(ApiException.class);
        assertThat(carts.read(user).items()).hasSize(1);
        Summary o =
                checkout.create(user, UUID.randomUUID(), request(checkout.preview(user))).value();
        Detail failed =
                commands.payment(user, o.id(), UUID.randomUUID(), new Payment(1L, Outcome.FAILURE))
                        .value();
        assertThat(failed.version()).isEqualTo(2);
        assertThat(failed.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(quantity(p)).isEqualTo(5);
        assertThat(count("commerce_payment_attempts", o.id())).isEqualTo(1);
    }

    @Test
    void sameKeyConcurrentCreateAndCrossResourceConflict() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID actor = user(BaseRole.USER), p = product(100, 2);
            carts.add(actor, UUID.randomUUID(), new AddItem(p, 1));
            CreateOrder r = request(checkout.preview(actor));
            UUID key = UUID.randomUUID();
            var results =
                    race(
                            () -> checkout.create(actor, key, r),
                            () -> checkout.create(actor, key, r));
            assertThat(results).allMatch(v -> v instanceof IdempotencyExecutor.Result<?>);
            var x = (IdempotencyExecutor.Result<?>) results.get(0);
            var y = (IdempotencyExecutor.Result<?>) results.get(1);
            assertThat(x.replayed()).isNotEqualTo(y.replayed());
            assertThat(x.value()).isEqualTo(y.value());
            assertThat(quantity(p)).isEqualTo(2);
        }
        UUID a = product(100, 2), b = product(100, 2), key = UUID.randomUUID();
        var r = new CatalogDtos.StockAdjustmentRequest(StockDirection.INCREASE, 1, "测试调整");
        stock.adjust(admin, a, key, r);
        assertThatThrownBy(() -> stock.adjust(admin, b, key, r)).isInstanceOf(ApiException.class);
        assertThat(quantity(b)).isEqualTo(2);
    }

    @Test
    void lastItemAndSameOrderDifferentKeyPaymentRaces() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID p = product(100, 1), other = user(BaseRole.USER);
            Summary a = order(user, p, 1), b = order(other, p, 1);
            var results = race(() -> pay(user, a), () -> pay(other, b));
            assertThat(results.stream().filter(v -> v instanceof Detail).count()).isEqualTo(1);
            assertThat(quantity(p)).isZero();
            assertThat(
                            count("commerce_payment_attempts", a.id())
                                    + count("commerce_payment_attempts", b.id()))
                    .isEqualTo(1);
            UUID q = product(100, 3);
            Summary c = order(user, q, 1);
            results = race(() -> pay(user, c), () -> pay(user, c));
            assertThat(results.stream().filter(v -> v instanceof Detail).count()).isEqualTo(1);
            assertThat(quantity(q)).isEqualTo(2);
        }
    }

    @Test
    void paymentCancelAndCancelShipmentRaces() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID p = product(100, 2);
            Summary o = order(user, p, 1);
            Summary initial = o;
            race(
                    () -> pay(user, initial),
                    () ->
                            commands.cancel(
                                    admin,
                                    initial.id(),
                                    UUID.randomUUID(),
                                    new Cancel(1L, "并发取消"),
                                    true));
            Detail current = queries.read(user, o.id(), false);
            assertThat(current.status()).isIn("PAID", "CANCELLED");
            assertThat(quantity(p)).isEqualTo(current.status().equals("PAID") ? 1 : 2);
            if (current.status().equals("CANCELLED")) {
                o = order(user, p, 1);
                current = pay(user, o);
            }
            UUID id = o.id();
            long version = current.version();
            race(
                    () ->
                            commands.cancel(
                                    user,
                                    id,
                                    UUID.randomUUID(),
                                    new Cancel(version, "并发取消"),
                                    false),
                    () ->
                            commands.shipment(
                                    admin,
                                    id,
                                    UUID.randomUUID(),
                                    new Shipment(version, "模拟物流", "SIM-123")));
            current = queries.read(user, id, false);
            assertThat(current.status()).isIn("CANCELLED", "SHIPPED");
            assertThat(quantity(p)).isEqualTo(current.status().equals("CANCELLED") ? 2 : 1);
            assertThat(count("commerce_refunds", id))
                    .isEqualTo(current.status().equals("CANCELLED") ? 1 : 0);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "order.created",
                "cart.cleared",
                "history.created",
                "audit.created",
                "idempotency.completed"
            })
    void createFaultRollsBackAndOriginalKeyRecovers(String point) {
        UUID p = product(100, 4);
        carts.add(user, UUID.randomUUID(), new AddItem(p, 1));
        CreateOrder r = request(checkout.preview(user));
        UUID key = UUID.randomUUID();
        doThrow(new IllegalStateException("injected")).when(probe).hit(point);
        assertThatThrownBy(() -> checkout.create(user, key, r))
                .isInstanceOf(IllegalStateException.class);
        assertThat(carts.read(user).items()).hasSize(1);
        assertThat(queries.list(user, false, null, null, null, 1, 20).total()).isZero();
        assertThat(quantity(p)).isEqualTo(4);
        assertThat(
                        records.findByActorIdAndOperationIdAndIdempotencyKey(
                                user, "commerce.createOrder", key))
                .isEmpty();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from operations_audit_records where"
                                        + " actor_id=UUID_TO_BIN(?)",
                                Long.class,
                                user.toString()))
                .isZero();
        reset(probe);
        assertThat(checkout.create(user, key, r).replayed()).isFalse();
        assertThat(carts.read(user).items()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "inventory.deducted",
                "payment.created",
                "history.created",
                "audit.created",
                "idempotency.completed"
            })
    void paymentFaultRollsBackEveryProductAndRecovers(String point) {
        UUID a = product(100, 5), b = product(200, 5);
        carts.add(user, UUID.randomUUID(), new AddItem(a, 1));
        Summary o = order(user, b, 2);
        UUID key = UUID.randomUUID();
        Payment r = new Payment(1L, Outcome.SUCCESS);
        doThrow(new IllegalStateException("injected")).when(probe).hit(point);
        assertThatThrownBy(() -> commands.payment(user, o.id(), key, r))
                .isInstanceOf(IllegalStateException.class);
        assertThat(quantity(a)).isEqualTo(5);
        assertThat(quantity(b)).isEqualTo(5);
        assertThat(count("commerce_payment_attempts", o.id())).isZero();
        assertThat(count("commerce_order_history", o.id())).isEqualTo(1);
        reset(probe);
        assertThat(commands.payment(user, o.id(), key, r).value().status()).isEqualTo("PAID");
        assertThat(quantity(a)).isEqualTo(4);
        assertThat(quantity(b)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "inventory.restored",
                "refund.created",
                "history.created",
                "audit.created",
                "idempotency.completed"
            })
    void refundFaultRollsBackAndRecovers(String point) {
        UUID a = product(100, 5), b = product(200, 5);
        carts.add(user, UUID.randomUUID(), new AddItem(a, 1));
        Summary o = order(user, b, 2);
        Detail paid = pay(user, o);
        UUID key = UUID.randomUUID();
        Cancel r = new Cancel(paid.version(), "故障测试");
        doThrow(new IllegalStateException("injected")).when(probe).hit(point);
        assertThatThrownBy(() -> commands.cancel(user, o.id(), key, r, false))
                .isInstanceOf(IllegalStateException.class);
        assertThat(quantity(a)).isEqualTo(4);
        assertThat(quantity(b)).isEqualTo(3);
        assertThat(count("commerce_refunds", o.id())).isZero();
        assertThat(queries.read(user, o.id(), false).status()).isEqualTo("PAID");
        reset(probe);
        commands.cancel(user, o.id(), key, r, false);
        assertThat(quantity(a)).isEqualTo(5);
        assertThat(quantity(b)).isEqualTo(5);
        assertThat(count("commerce_refunds", o.id())).isEqualTo(1);
    }

    @Test
    void metricsUsesPaymentCohortAndCurrentRefundSnapshot() {
        UUID p = product(10000, 3);
        Summary o = order(user, p, 1);
        Detail paid = pay(user, o);
        jdbc.update(
                "update commerce_payment_attempts set created_at='2026-09-05 12:00:00' where"
                        + " order_id=UUID_TO_BIN(?)",
                o.id().toString());
        commands.cancel(user, o.id(), UUID.randomUUID(), new Cancel(paid.version(), "次日退款"), false);
        var result =
                work.run(
                        () ->
                                metrics.read(
                                        Instant.parse("2026-09-05T00:00:00Z"),
                                        Instant.parse("2026-09-06T00:00:00Z")));
        assertThat(result.netPaidFen()).isEqualTo("0");
        assertThatThrownBy(() -> metrics.read(Instant.EPOCH, Instant.now()))
                .isInstanceOf(
                        org.springframework.transaction.IllegalTransactionStateException.class);
        assertThat(
                        references
                                .requireOwnedReference(
                                        new ActorContext(
                                                user, "", BaseRole.USER, AccountStatus.ACTIVE),
                                        o.id())
                                .status())
                .isEqualTo("CANCELLED");
        assertThatThrownBy(
                        () ->
                                references.requireOwnedReference(
                                        new ActorContext(
                                                admin, "", BaseRole.ADMIN, AccountStatus.ACTIVE),
                                        o.id()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void strictHttpAndPrivateResourcePermissions() throws Exception {
        var auth =
                UsernamePasswordAuthenticationToken.authenticated(
                        new UserPrincipal(
                                user,
                                "test@example.test",
                                "unused",
                                "测试账户",
                                BaseRole.USER,
                                AccountStatus.ACTIVE,
                                0),
                        null,
                        List.of());
        UUID p = product(100, 3);
        for (String quantity : List.of("1.5", "\"1\"", "null"))
            mvc.perform(
                            post("/api/v1/cart/items")
                                    .with(authentication(auth))
                                    .with(csrf())
                                    .header("Origin", "http://localhost:5173")
                                    .contentType("application/json")
                                    .header("Idempotency-Key", UUID.randomUUID())
                                    .content(
                                            "{\"productId\":\""
                                                    + p
                                                    + "\",\"quantity\":"
                                                    + quantity
                                                    + "}"))
                    .andExpect(status().isUnprocessableEntity());
        mvc.perform(
                        post("/api/v1/cart/items")
                                .with(authentication(auth))
                                .with(csrf())
                                .header("Origin", "http://localhost:5173")
                                .contentType("application/json")
                                .header("Idempotency-Key", UUID.randomUUID())
                                .content(
                                        "{\"productId\":\"" + p + "\",\"quantity\":1,\"price\":1}"))
                .andExpect(status().isUnprocessableEntity());
        Summary o = order(user, p, 1);
        assertThatThrownBy(() -> queries.read(admin, o.id(), false))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> carts.add(admin, UUID.randomUUID(), new AddItem(p, 1)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void legacyStockReplayAndCollisionNeverApplyNewEffects() throws Exception {
        UUID a = product(100, 5), b = product(100, 5), key = UUID.randomUUID();
        var request = new CatalogDtos.StockAdjustmentRequest(StockDirection.INCREASE, 1, "历史调整");
        var result = stock.adjust(admin, a, UUID.randomUUID(), request).value();
        String hash =
                HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                "INCREASE|1|历史调整"
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8)));
        String json = mapper.writeValueAsString(result);
        work.run(
                () -> {
                    records.saveAndFlush(
                            new IdempotencyRecordEntity(
                                    admin,
                                    "catalog.adjustStock:" + a,
                                    key,
                                    hash,
                                    json,
                                    200,
                                    Instant.now()));
                    return null;
                });
        assertThat(stock.adjust(admin, a, key, request).replayed()).isTrue();
        assertThat(quantity(a)).isEqualTo(6);
        assertThatThrownBy(() -> stock.adjust(admin, b, key, request))
                .isInstanceOf(ApiException.class);
        assertThat(quantity(b)).isEqualTo(5);
        work.run(
                () -> {
                    records.saveAndFlush(
                            new IdempotencyRecordEntity(
                                    admin,
                                    "catalog.adjustStock:" + b,
                                    key,
                                    hash,
                                    json,
                                    200,
                                    Instant.now()));
                    return null;
                });
        assertThatThrownBy(() -> stock.adjust(admin, a, key, request))
                .isInstanceOf(ApiException.class);
        assertThat(quantity(a)).isEqualTo(6);
    }

    @Test
    void cartLimitsAndStaleVersionAreEnforced() {
        List<UUID> products = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UUID p = product(99999999, 100);
            products.add(p);
            carts.add(user, UUID.randomUUID(), new AddItem(p, 99));
        }
        assertThat(carts.read(user).totalFen()).isEqualTo(197999998020L);
        UUID extra = product(100, 100);
        assertThatThrownBy(() -> carts.add(user, UUID.randomUUID(), new AddItem(extra, 1)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                carts.add(
                                        user,
                                        UUID.randomUUID(),
                                        new AddItem(products.getFirst(), 1)))
                .isInstanceOf(ApiException.class);
        long version = carts.read(user).cartVersion();
        carts.delete(user, extra, version);
        assertThat(carts.read(user).cartVersion()).isEqualTo(version);
        assertThatThrownBy(() -> carts.delete(user, extra, version - 1))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void reverseProductInputAndConcurrentAddCreateDoNotLoseItems() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID a = product(100, 5), b = product(200, 5), other = user(BaseRole.USER);
            carts.add(user, UUID.randomUUID(), new AddItem(a, 1));
            Summary x = order(user, b, 1);
            carts.add(other, UUID.randomUUID(), new AddItem(b, 1));
            Summary y = order(other, a, 1);
            var results = race(() -> pay(user, x), () -> pay(other, y));
            assertThat(results).allMatch(r -> r instanceof Detail);
            assertThat(quantity(a)).isEqualTo(3);
            assertThat(quantity(b)).isEqualTo(3);
            UUID actor = user(BaseRole.USER), c = product(100, 5);
            carts.add(actor, UUID.randomUUID(), new AddItem(a, 1));
            CreateOrder r = request(checkout.preview(actor));
            var created =
                    race(
                            () -> checkout.create(actor, UUID.randomUUID(), r),
                            () -> carts.add(actor, UUID.randomUUID(), new AddItem(c, 1)));
            boolean success = created.getFirst() instanceof IdempotencyExecutor.Result<?>;
            assertThat(carts.read(actor).items()).hasSize(success ? 1 : 2);
            assertThat(carts.read(actor).items()).anyMatch(i -> i.productId().equals(c));
        }
    }

    @Autowired wemove.identity.service.UserAccountService accounts;
    @Autowired wemove.catalog.service.CatalogService productService;

    @Test
    void adminStockAndUnpublicationCompeteWithPayment() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID p = product(100, 1);
            Summary o = order(user, p, 1);
            var results =
                    race(
                            () -> pay(user, o),
                            () ->
                                    stock.adjust(
                                            admin,
                                            p,
                                            UUID.randomUUID(),
                                            new CatalogDtos.StockAdjustmentRequest(
                                                    StockDirection.DECREASE, 1, "并发库存调整")));
            assertThat(results.stream().filter(v -> !(v instanceof String)).count()).isEqualTo(1);
            assertThat(quantity(p)).isZero();
            UUID q = product(100, 1);
            Summary another = order(user, q, 1);
            race(
                    () -> pay(user, another),
                    () ->
                            productService.changePublication(
                                    admin,
                                    q,
                                    UUID.randomUUID(),
                                    new CatalogDtos.VersionCommand(0L),
                                    false));
            Detail current = queries.read(user, another.id(), false);
            assertThat(current.status()).isIn("PAID", "PENDING_PAYMENT");
            assertThat(quantity(q)).isEqualTo(current.status().equals("PAID") ? 0 : 1);
            assertThat(count("commerce_payment_attempts", another.id()))
                    .isEqualTo(current.status().equals("PAID") ? 1 : 0);
        }
    }

    @Test
    void accountDisableAndPaymentHaveSerialOutcome() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID actor = user(BaseRole.USER), p = product(100, 2);
            Summary o = order(actor, p, 1);
            var results =
                    race(
                            () -> pay(actor, o),
                            () ->
                                    accounts.changeStatus(
                                            admin,
                                            actor,
                                            "DISABLE",
                                            UUID.randomUUID(),
                                            new wemove.identity.api.Dtos.AccountCommand(
                                                    0L, "并发停用测试")));
            assertThat(users.findById(actor).orElseThrow().getAccountStatus())
                    .isEqualTo(AccountStatus.DISABLED);
            boolean paid = results.getFirst() instanceof Detail;
            assertThat(quantity(p)).isEqualTo(paid ? 1 : 2);
            assertThatThrownBy(
                            () ->
                                    commands.payment(
                                            actor,
                                            o.id(),
                                            UUID.randomUUID(),
                                            new Payment(1L, Outcome.SUCCESS)))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Test
    void metricHalfOpenBoundsAndRefundCohort() {
        UUID p = product(100, 5);
        Summary a = order(user, p, 1), b = order(user, p, 1);
        Detail paid = pay(user, a);
        pay(user, b);
        Instant latest = jdbc.queryForObject("select max(created_at) from commerce_payment_attempts", java.sql.Timestamp.class).toInstant();
        Instant baseline = Instant.parse("2200-01-01T00:00:00Z");
        Instant start = (latest.isAfter(baseline) ? latest : baseline).plusSeconds(86400);
        Instant end = start.plusSeconds(86400);
        jdbc.update("update commerce_payment_attempts set created_at=? where order_id=UUID_TO_BIN(?)", java.sql.Timestamp.from(start), a.id().toString());
        jdbc.update("update commerce_payment_attempts set created_at=? where order_id=UUID_TO_BIN(?)", java.sql.Timestamp.from(end), b.id().toString());
        assertThat(work.run(() -> metrics.read(start, end)).netPaidFen()).isEqualTo("100");
        commands.cancel(
                user, a.id(), UUID.randomUUID(), new Cancel(paid.version(), "跨区间退款"), false);
        assertThat(work.run(() -> metrics.read(start, end)).netPaidFen()).isEqualTo("0");
        assertThat(work.run(() -> metrics.read(end, end.plusSeconds(86400))).netPaidFen())
                .isEqualTo("100");
    }
}
