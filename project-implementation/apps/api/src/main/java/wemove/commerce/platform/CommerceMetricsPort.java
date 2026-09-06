package wemove.commerce.platform;

import java.time.Instant;

/** F calls inside its single read-only database snapshot after dashboard authorization. */
public interface CommerceMetricsPort {
    Metrics read(Instant start, Instant end);

    record Metrics(long pendingShipmentCount, long createdOrderCount, String netPaidFen) {}
}
