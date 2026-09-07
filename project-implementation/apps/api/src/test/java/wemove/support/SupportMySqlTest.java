package wemove.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import wemove.identity.domain.*;
import wemove.identity.repository.UserRepository;
import wemove.identity.security.UserPrincipal;
import wemove.platform.*;
import wemove.platform.api.ApiException;
import wemove.support.api.SupportDtos.*;
import wemove.support.service.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Opt-in only: MYSQL_TEST_URL 必须指向专用 support_test 库（真实 MySQL + Flyway + 真
 * DatabaseAuditAdapter）。覆盖 FR-19/20/21/32 的关键链路与 TC-25 状态机。
 */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql:.*support_test.*")
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
class SupportMySqlTest {
    @Autowired TicketCommandService commands;
    @Autowired TicketQueryService queries;
    @Autowired DashboardService dashboard;
    @Autowired UserRepository users;
    @Autowired UnitOfWork work;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoSpyBean TransactionProbe probe;

    UUID user;
    UUID other;
    UUID admin;

    static final CreateTicketRequest GENERAL =
            new CreateTicketRequest("GENERAL", "配送范围咨询", "请问儿童平衡车可以配送至外地吗？", null, null, null);

    @BeforeEach
    void setup() {
        reset(probe);
        // 工单四表随测试清空（子表先于主表），保证 dashboard 待办数等断言与用例执行顺序无关。
        jdbc.update("delete from support_ticket_history");
        jdbc.update("delete from support_internal_notes");
        jdbc.update("delete from support_messages");
        jdbc.update("delete from support_tickets");
        user = user(BaseRole.USER);
        other = user(BaseRole.USER);
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

    UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into"
                    + " catalog_products(id,sku,name,category_id,summary,age_min,play_type,scene,main_image_id,retail_unit_price_fen,dealer_enabled,status,display_order,version,created_at,updated_at)"
                    + " values(UUID_TO_BIN(?),?,'测试商品',UUID_TO_BIN('10000000-0000-0000-0000-000000000102'),'支持模块测试商品',4,'THROWING','BOTH','seed-img-ring-toss',19900,false,'PUBLISHED',0,0,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))",
                id.toString(),
                "S-" + id.toString().substring(0, 30));
        return id;
    }

    long auditCount(UUID objectId) {
        return jdbc.queryForObject(
                "select count(*) from operations_audit_records where object_id=UUID_TO_BIN(?) and action like 'TICKET_%'",
                Long.class,
                objectId.toString());
    }

    TicketDetail ticket(CreateTicketRequest request) {
        return commands.create(user, UUID.randomUUID(), request).value();
    }

    @Test
    void createTicketTypesAndAudits() {
        UUID productId = product();
        TicketDetail general = ticket(GENERAL);
        assertThat(general.status()).isEqualTo("NEW");
        assertThat(general.version()).isEqualTo(1);
        assertThat(general.ticketNumber()).startsWith("TK");
        assertThat(general.allowedActions()).containsExactly("FOLLOW_UP", "CLOSE");
        assertThat(auditCount(general.id())).isEqualTo(1);

        TicketDetail product_ =
                ticket(
                        new CreateTicketRequest(
                                "PRODUCT",
                                "商品尺寸咨询",
                                "这款商品的适用年龄是多少？包装尺寸有多大？",
                                "13800000000",
                                productId,
                                null));
        assertThat(product_.product().name()).isEqualTo("测试商品");
        assertThat(product_.order()).isNull();

        // 用户响应中永远没有内部备注
        assertThat(general.internalNotes()).isEmpty();
    }

    @Test
    void createValidatesAssociationsAndLengths() {
        UUID productId = product();
        assertThatThrownBy(
                        () ->
                                commands.create(
                                        user,
                                        UUID.randomUUID(),
                                        new CreateTicketRequest(
                                                "GENERAL", "主题", "内容不足十个字符", null, productId, null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                commands.create(
                                        user,
                                        UUID.randomUUID(),
                                        new CreateTicketRequest(
                                                "PRODUCT", "商品咨询", "这是一个足够长的商品咨询内容。", null, null, null)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(
                        () ->
                                commands.create(
                                        user,
                                        UUID.randomUUID(),
                                        new CreateTicketRequest(
                                                "PRODUCT",
                                                "商品咨询",
                                                "这是一个足够长的商品咨询内容。",
                                                null,
                                                UUID.randomUUID(),
                                                null)))
                .hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
        assertThatThrownBy(
                        () ->
                                commands.create(
                                        user,
                                        UUID.randomUUID(),
                                        new CreateTicketRequest(
                                                "GENERAL", "x".repeat(101), "a".repeat(10), null, null, null)))
                .isInstanceOf(ApiException.class);
        // 售后必须关联本人订单：不存在/他人订单统一 404
        assertThatThrownBy(
                        () ->
                                commands.create(
                                        user,
                                        UUID.randomUUID(),
                                        new CreateTicketRequest(
                                                "AFTER_SALES",
                                                "售后咨询",
                                                "订单商品有破损需要处理，请跟进。",
                                                null,
                                                null,
                                                UUID.randomUUID())))
                .hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
    }

    @Test
    void adminCannotCreateUserTickets() {
        assertThatThrownBy(
                        () -> commands.create(admin, UUID.randomUUID(), GENERAL))
                .hasFieldOrPropertyWithValue("code", "FORBIDDEN");
    }

    @Test
    void fullLifecycleStateMachineAndIsolation() {
        // T-I-05：NEW→PROCESSING→REPLIED→PROCESSING→REPLIED→CLOSED
        TicketDetail t = ticket(GENERAL);
        UUID id = t.id();
        assertThat(commands.start(admin, id, UUID.randomUUID(), new StartRequest(t.version())).value()
                        .status())
                .isEqualTo("PROCESSING");
        TicketDetail replied =
                commands.reply(
                                admin,
                                id,
                                UUID.randomUUID(),
                                new ReplyRequest(2, "您好，已收到您的咨询，正在为您核实配送范围。"))
                        .value();
        assertThat(replied.status()).isEqualTo("REPLIED");
        assertThat(replied.messages()).hasSize(1);
        assertThat(replied.messages().get(0).kind()).isEqualTo("PUBLIC_REPLY");

        TicketDetail noted =
                commands.addNote(admin, id, UUID.randomUUID(), new NoteRequest("内部记录：已电话回访确认。"))
                        .value();
        // 内部备注不改变状态与版本
        assertThat(noted.status()).isEqualTo("REPLIED");
        assertThat(noted.version()).isEqualTo(3);
        assertThat(noted.internalNotes()).hasSize(1);

        // 用户补充：已回复 → 处理中
        TicketDetail followed =
                commands.followUp(user, id, UUID.randomUUID(), new FollowUpRequest(3, "还想确认一下偏远地区是否加价。"))
                        .value();
        assertThat(followed.status()).isEqualTo("PROCESSING");
        assertThat(followed.messages()).hasSize(2);
        // 用户视图看不到内部备注
        assertThat(queries.ownedDetail(user, id).internalNotes()).isEmpty();
        assertThat(queries.ownedDetail(user, id).messages()).hasSize(2);

        assertThat(commands.reply(
                                admin,
                                id,
                                UUID.randomUUID(),
                                new ReplyRequest(4, "偏远地区会按实际运费补差，下单前客服会先与您确认。"))
                        .value()
                        .status())
                .isEqualTo("REPLIED");

        TicketDetail closed =
                commands.closeByAdmin(
                                admin,
                                id,
                                UUID.randomUUID(),
                                new AdminCloseRequest(5, "问题已解答完毕。"))
                        .value();
        assertThat(closed.status()).isEqualTo("CLOSED");
        assertThat(closed.closeReason()).isEqualTo("问题已解答完毕。");

        // 关闭后所有写入拒绝
        assertThatThrownBy(() -> commands.followUp(user, id, UUID.randomUUID(), new FollowUpRequest(6, "追加内容")))
                .hasFieldOrPropertyWithValue("code", "STATE_CONFLICT");
        assertThatThrownBy(() -> commands.reply(admin, id, UUID.randomUUID(), new ReplyRequest(6, "再回复")))
                .hasFieldOrPropertyWithValue("code", "STATE_CONFLICT");
        assertThatThrownBy(() -> commands.addNote(admin, id, UUID.randomUUID(), new NoteRequest("再备注")))
                .hasFieldOrPropertyWithValue("code", "STATE_CONFLICT");
        assertThatThrownBy(() -> commands.start(admin, id, UUID.randomUUID(), new StartRequest(6)))
                .hasFieldOrPropertyWithValue("code", "STATE_CONFLICT");

        // 历史与审计完整
        List<HistoryView> history =
                queries.detail(queries.ticket(id).orElseThrow(), true).history();
        assertThat(history)
                .extracting(HistoryView::action)
                .containsExactly("CREATED", "STARTED", "REPLIED", "FOLLOWED_UP", "REPLIED", "CLOSED");
        // 审计含内部备注（NOTE_ADDED），比状态历史多一条
        assertThat(auditCount(id)).isEqualTo(7);
    }

    @Test
    void ownershipAndVersionConflicts() {
        TicketDetail t = ticket(GENERAL);
        // 他人读取/补充统一 404
        assertThatThrownBy(() -> queries.ownedDetail(other, t.id()))
                .hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
        assertThatThrownBy(
                        () -> commands.followUp(other, t.id(), UUID.randomUUID(), new FollowUpRequest(1, "越权补充")))
                .hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
        // 版本冲突
        commands.reply(admin, t.id(), UUID.randomUUID(), new ReplyRequest(1, "首次回复内容。"));
        assertThatThrownBy(
                        () -> commands.followUp(user, t.id(), UUID.randomUUID(), new FollowUpRequest(1, "过期版本补充")))
                .hasFieldOrPropertyWithValue("code", "VERSION_CONFLICT");
    }

    @Test
    void idempotentReplayDoesNotDuplicate() {
        UUID key = UUID.randomUUID();
        TicketDetail first = commands.create(user, key, GENERAL).value();
        var second = commands.create(user, key, GENERAL);
        assertThat(second.replayed()).isTrue();
        assertThat(second.value().id()).isEqualTo(first.id());
        assertThat(auditCount(first.id())).isEqualTo(1);

        UUID noteKey = UUID.randomUUID();
        commands.addNote(admin, first.id(), noteKey, new NoteRequest("首条内部备注。"));
        assertThat(commands.addNote(admin, first.id(), noteKey, new NoteRequest("首条内部备注。"))
                        .replayed())
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from support_internal_notes where ticket_id=UUID_TO_BIN(?)",
                                Long.class,
                                first.id().toString()))
                .isEqualTo(1);
    }

    @Test
    void auditWriteFailureRollsBackBusiness() throws Exception {
        TicketDetail t = ticket(GENERAL);
        doThrow(new IllegalStateException("audit down"))
                .when(probe)
                .hit("ticket.audit.created");
        assertThatThrownBy(
                        () ->
                                commands.start(
                                        admin, t.id(), UUID.randomUUID(), new StartRequest(t.version())))
                .isInstanceOf(Exception.class);
        reset(probe);
        // 业务一并回滚：仍是 NEW，幂等键未占用，可重试成功
        TicketDetail reloaded = queries.ownedDetail(user, t.id());
        assertThat(reloaded.status()).isEqualTo("NEW");
        assertThat(commands.start(admin, t.id(), UUID.randomUUID(), new StartRequest(1)).value()
                        .status())
                .isEqualTo("PROCESSING");
    }

    @Test
    void userCloseRequiresConfirmReasonDefaults() {
        TicketDetail t = ticket(GENERAL);
        TicketDetail closed =
                commands.closeByUser(user, t.id(), UUID.randomUUID(), new UserCloseRequest(1, null))
                        .value();
        assertThat(closed.status()).isEqualTo("CLOSED");
        assertThat(closed.closeReason()).isEqualTo("用户确认关闭");
    }

    @Test
    void dashboardAggregatesSingleSnapshot() {
        jdbc.update("update catalog_products set status='UNLISTED' where status='PUBLISHED'");
        UUID p1 = product();
        UUID p2 = product();
        Instant start = Instant.parse("2026-09-05T00:00:00Z");
        Instant end = Instant.parse("2026-09-07T00:00:00Z");
        var snapshot = dashboard.read(start, end);
        assertThat(snapshot.publishedProductCount()).isEqualTo(2);
        assertThat(snapshot.activeUserCount()).isGreaterThanOrEqualTo(3);
        assertThat(snapshot.pendingApplicationCount()).isZero();
        assertThat(snapshot.pendingInquiryCount()).isZero();
        assertThat(snapshot.pendingShipmentCount()).isZero();
        assertThat(snapshot.createdOrderCount()).isZero();
        assertThat(snapshot.netPaidFen()).isEqualTo("0");
        assertThat(snapshot.asOf()).isNotNull();

        TicketDetail t = ticket(GENERAL);
        assertThat(dashboard.read(start, end).pendingTicketCount()).isEqualTo(1);
        commands.start(admin, t.id(), UUID.randomUUID(), new StartRequest(1));
        // 处理中仍计入待办
        assertThat(dashboard.read(start, end).pendingTicketCount()).isEqualTo(1);
        commands.closeByAdmin(admin, t.id(), UUID.randomUUID(), new AdminCloseRequest(2, "处理完毕。"));
        assertThat(dashboard.read(start, end).pendingTicketCount()).isZero();

        assertThatThrownBy(() -> dashboard.read(end, start))
                .hasFieldOrPropertyWithValue("code", "VALIDATION_ERROR");
        // 两个商品由 fixture 插入，不影响其他断言
        jdbc.update(
                "update catalog_products set status='UNLISTED' where id in (UUID_TO_BIN(?),UUID_TO_BIN(?))",
                p1.toString(),
                p2.toString());
    }

    @Test
    void httpSecurityAndStrictJson() throws Exception {
        // 授权过滤器读取的是令牌自身的 authorities（不是 principal.getAuthorities()），
        // 因此按角色显式提供，保持与 UserPrincipal.getAuthorities() 一致。
        var userAuth =
                UsernamePasswordAuthenticationToken.authenticated(
                        new UserPrincipal(
                                user, "test@example.test", "unused", "测试账户",
                                BaseRole.USER, AccountStatus.ACTIVE, 0),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var adminAuth =
                UsernamePasswordAuthenticationToken.authenticated(
                        new UserPrincipal(
                                admin, "admin@example.test", "unused", "管理员",
                                BaseRole.ADMIN, AccountStatus.ACTIVE, 0),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        // 未登录 401
        mvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
        // 用户访问后台 403
        mvc.perform(get("/api/v1/admin/tickets").with(authentication(userAuth)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/audit-logs").with(authentication(userAuth)).with(csrf()))
                .andExpect(status().isForbidden());
        // 管理员创建工单 403（FR-19 仅普通用户/经销商）
        mvc.perform(
                        post("/api/v1/tickets")
                                .with(authentication(adminAuth))
                                .with(csrf())
                                .header("Origin", "http://localhost:5173")
                                .contentType("application/json")
                                .header("Idempotency-Key", UUID.randomUUID())
                                .content(
                                        "{\"type\":\"GENERAL\",\"subject\":\"管理员咨询\","
                                                + "\"body\":\"管理员不应发起个人咨询。\"}"))
                .andExpect(status().isForbidden());
        // 未知字段 422（严格 JSON）
        mvc.perform(
                        post("/api/v1/tickets")
                                .with(authentication(userAuth))
                                .with(csrf())
                                .header("Origin", "http://localhost:5173")
                                .contentType("application/json")
                                .header("Idempotency-Key", UUID.randomUUID())
                                .content(
                                        "{\"type\":\"GENERAL\",\"subject\":\"配送咨询\","
                                                + "\"body\":\"请说明可以配送的城市范围。\",\"extra\":1}"))
                .andExpect(status().isUnprocessableEntity());
        // 管理员读取审计列表（含工单审计）
        TicketDetail t = ticket(GENERAL);
        mvc.perform(
                        get("/api/v1/admin/audit-logs")
                                .with(authentication(adminAuth))
                                .with(csrf())
                                .param("objectType", "TICKET")
                                .param("objectId", t.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("TICKET_CREATED"));
        // 用户分页参数非法 422
        mvc.perform(
                        get("/api/v1/tickets")
                                .with(authentication(userAuth))
                                .with(csrf())
                                .param("page", "0"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rateLimitSharedBucketForContactWrites() {
        TicketDetail t = ticket(GENERAL);
        // 桶容量 20：创建 1 次 + 补充 19 次 = 20 次全部成功；第 21 次（第 20 次补充）触发 429
        for (int i = 0; i < 19; i++) {
            TicketDetail current = queries.ownedDetail(user, t.id());
            commands.followUp(
                    user,
                    t.id(),
                    UUID.randomUUID(),
                    new FollowUpRequest(current.version(), "补充内容第 " + i + " 条。"));
        }
        TicketDetail current = queries.ownedDetail(user, t.id());
        assertThatThrownBy(
                        () ->
                                commands.followUp(
                                        user,
                                        t.id(),
                                        UUID.randomUUID(),
                                        new FollowUpRequest(current.version(), "应当被限流。")))
                .hasFieldOrPropertyWithValue("code", "RATE_LIMITED");
        // 限流被拒后不写业务数据
        assertThat(queries.ownedDetail(user, t.id()).version()).isEqualTo(current.version());
    }
}
