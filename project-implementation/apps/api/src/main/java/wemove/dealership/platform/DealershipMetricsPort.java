package wemove.dealership.platform;

/** F reads this port inside the dashboard's read-only snapshot. */
public interface DealershipMetricsPort {
    Metrics read();

    record Metrics(long pendingApplicationCount, long pendingInquiryCount) {}
}
