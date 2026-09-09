package wemove.commerce.platform;

import java.time.Instant;

/**
 * 面向 F 模块的交易只读指标端口。
 *
 * <p>调用方在完成后台权限校验并建立只读事务后，按 UTC 半开时间区间读取待发货订单数、创建订单数和模拟支付净额。
 *
 * @author junjie.xia
 * @since 2026-09-06
 */
public interface CommerceMetricsPort {
    Metrics read(Instant start, Instant end);

    record Metrics(long pendingShipmentCount, long createdOrderCount, String netPaidFen) {}
}
