package wemove.support.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wemove.commerce.platform.CommerceMetricsPort;
import wemove.dealership.platform.DealershipMetricsPort;
import wemove.support.api.SupportDtos.DashboardSnapshot;
import wemove.support.domain.TicketRules;

import java.time.Clock;
import java.time.Instant;

/**
 * FR-32 运营总览。F 在单一只读数据库快照内调用各域聚合，返回共同 asOf；
 * 前六项为当前总量，后两项按 [start,end) 半开区间。
 */
@Service
public class DashboardService {
    private final CommerceMetricsPort commerce;
    private final DealershipMetricsPort dealership;
    private final JdbcTemplate jdbc;
    private final Clock clock = Clock.systemUTC();

    public DashboardService(CommerceMetricsPort commerce, DealershipMetricsPort dealership, JdbcTemplate jdbc) {
        this.commerce = commerce;
        this.dealership = dealership;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot read(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end))
            throw TicketRules.invalid("start", "请提供有效的UTC半开时间区间（start 早于 end）。");
        CommerceMetricsPort.Metrics metrics = commerce.read(start, end);
        DealershipMetricsPort.Metrics dealershipMetrics = dealership.read();
        return new DashboardSnapshot(
                count("select count(*) from catalog_products where status = 'PUBLISHED'"),
                // 契约口径：启用用户数含管理员。
                count("select count(*) from users where account_status = 'ACTIVE'"),
                dealershipMetrics.pendingApplicationCount(),
                dealershipMetrics.pendingInquiryCount(),
                count("select count(*) from support_tickets where status in ('NEW','PROCESSING')"),
                metrics.pendingShipmentCount(),
                metrics.createdOrderCount(),
                metrics.netPaidFen(),
                clock.instant(),
                start,
                end);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }
}
