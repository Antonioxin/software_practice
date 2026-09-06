package wemove.commerce.service;

import static wemove.commerce.domain.CommerceRules.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import wemove.commerce.platform.*;
import wemove.commerce.repository.CommerceRepository;
import wemove.platform.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class CommerceIntegrationService implements OrdersPort, CommerceMetricsPort {
    private final CommerceRepository db;
    private final IdentityPort identity;
    private final JdbcTemplate jdbc;

    public CommerceIntegrationService(
            CommerceRepository db, IdentityPort identity, JdbcTemplate jdbc) {
        this.db = db;
        this.identity = identity;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Reference requireOwnedReference(ActorContext actor, UUID orderId) {
        identity.lockActiveActor(actor.actorId());
        var o = db.order(orderId, false);
        CommerceAccess.owned(o, actor.actorId(), false);
        return new Reference(o.id, o.orderNumber, o.status);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Metrics read(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end))
            throw invalid("start", "请提供有效的UTC半开时间区间。");
        long pending =
                jdbc.queryForObject(
                        "select count(*) from commerce_orders where status='PAID'", Long.class);
        long created =
                jdbc.queryForObject(
                        "select count(*) from commerce_orders where created_at>=? and created_at<?",
                        Long.class,
                        Timestamp.from(start),
                        Timestamp.from(end));
        java.math.BigDecimal net =
                jdbc.queryForObject(
                        "select coalesce(sum(cast(p.amount_fen as"
                                + " decimal(65,0))-coalesce(r.amount_fen,0)),0) from"
                                + " commerce_payment_attempts p left join commerce_refunds r on"
                                + " r.payment_attempt_id=p.id where p.outcome='SUCCESS' and"
                                + " p.created_at>=? and p.created_at<?",
                        java.math.BigDecimal.class,
                        Timestamp.from(start),
                        Timestamp.from(end));
        return new Metrics(pending, created, net.toBigIntegerExact().toString());
    }
}
