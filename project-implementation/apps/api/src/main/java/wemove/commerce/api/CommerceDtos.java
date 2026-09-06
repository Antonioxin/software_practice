package wemove.commerce.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public final class CommerceDtos {
    private CommerceDtos() {}

    public record AddItem(@NotNull UUID productId, @NotNull @Min(1) @Max(99) Integer quantity) {}

    public record UpdateItem(
            @NotNull @Min(1) @Max(99) Integer quantity, @NotNull @Min(1) Long cartVersion) {}

    public record ShippingAddress(
            @NotBlank String recipient,
            @NotBlank String phone,
            @NotBlank String countryOrRegion,
            String region,
            @NotBlank String city,
            @NotBlank String addressLine) {}

    public record CreateOrder(
            @NotBlank @Size(max = 200) String previewToken,
            @NotNull @Min(1) Long cartVersion,
            @NotNull @Valid ShippingAddress shippingAddress,
            @Size(max = 2000) String remark,
            @Min(0) Long clientTotalFen) {}

    public record Payment(@NotNull @Min(1) Long expectedVersion, @NotNull Outcome outcome) {}

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    public record Cancel(@NotNull @Min(1) Long expectedVersion, @NotBlank String reason) {}

    public record Receipt(@NotNull @Min(1) Long expectedVersion) {}

    public record Shipment(
            @NotNull @Min(1) Long expectedVersion,
            @NotBlank String logisticsName,
            @NotBlank String trackingNumber) {}

    public record Line(
            UUID productId,
            String sku,
            String name,
            long unitPriceFen,
            int quantity,
            long subtotalFen,
            boolean valid,
            String reason,
            boolean priceChanged,
            long previousUnitPriceFen) {}

    public record CartView(
            long cartVersion,
            List<Line> items,
            long totalFen,
            boolean canCheckout,
            String currency) {}

    public record PreviewView(
            String previewToken,
            long cartVersion,
            Instant expiresAt,
            String currency,
            List<Line> items,
            long subtotalFen,
            long shippingFen,
            long taxFen,
            long discountFen,
            long totalFen) {}

    public record Summary(
            UUID id,
            String orderNumber,
            String status,
            long version,
            String currency,
            long totalFen,
            String mode,
            Instant createdAt) {}

    public record Attempt(
            UUID id,
            String outcome,
            long amountFen,
            String simulationReference,
            Instant createdAt,
            String mode) {}

    public record RefundView(
            UUID id,
            long amountFen,
            String simulationReference,
            Instant createdAt,
            String reason,
            String mode) {}

    public record History(
            String action,
            String fromStatus,
            String toStatus,
            long version,
            String reason,
            Instant createdAt) {}

    public record Detail(
            UUID id,
            String orderNumber,
            String status,
            long version,
            String currency,
            long totalFen,
            String mode,
            Instant createdAt,
            long subtotalFen,
            long shippingFen,
            long taxFen,
            long discountFen,
            ShippingAddress shippingAddress,
            String remark,
            List<Line> items,
            List<Attempt> paymentAttempts,
            List<RefundView> refunds,
            List<History> history,
            List<String> allowedActions,
            String logisticsName,
            String trackingNumber,
            Instant paidAt,
            Instant shippedAt,
            Instant completedAt) {}

    public record OrderPage(List<Summary> items, int page, int pageSize, long total) {}
}
