package wemove.dealership.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class DealershipDtos {
    private DealershipDtos() {}

    public record ApplicationRequest(
            @NotBlank String companyName,
            @NotBlank String businessType,
            @NotBlank String countryOrRegion,
            @NotBlank String city,
            @NotBlank String contactName,
            @NotBlank String phone,
            @NotBlank String cooperationEmail,
            @NotBlank String businessChannels,
            String website,
            @NotBlank String cooperationIntent,
            Boolean publicChannelConsent) {}

    public record ResubmitRequest(
            @Positive long applicationVersion,
            @NotBlank String companyName,
            @NotBlank String businessType,
            @NotBlank String countryOrRegion,
            @NotBlank String city,
            @NotBlank String contactName,
            @NotBlank String phone,
            @NotBlank String cooperationEmail,
            @NotBlank String businessChannels,
            String website,
            @NotBlank String cooperationIntent,
            Boolean publicChannelConsent) {}

    public record ReviewRequest(
            @Positive int applicationVersion,
            @NotBlank String decision,
            String publicReason,
            @Size(max = 2000) String internalNote,
            UUID existingCompanyId) {}

    public record ApplicationVersionView(
            int contentVersion,
            String companyName,
            String businessType,
            String countryOrRegion,
            String city,
            String contactName,
            String phone,
            String cooperationEmail,
            String businessChannels,
            String website,
            String cooperationIntent,
            boolean publicChannelConsent,
            Instant submittedAt) {}

    public record ReviewView(
            int contentVersion,
            String decision,
            String publicReason,
            String internalNote,
            UUID reviewerId,
            Instant createdAt) {}

    public record ApplicationView(
            UUID id,
            String applicationNumber,
            UUID userId,
            String status,
            int currentContentVersion,
            long version,
            String publicReason,
            String internalNote,
            boolean suspectedDuplicate,
            List<ApplicationVersionView> versions,
            List<ReviewView> reviews,
            Instant createdAt,
            Instant updatedAt) {}

    public record DealerProductView(
            UUID id,
            String sku,
            String name,
            long retailUnitPriceFen,
            long referenceUnitPriceFen,
            String currency,
            int minInquiryQuantity,
            int availableQuantity,
            String leadTimeText,
            String priceNotice) {}

    public record InquiryLineRequest(@NotNull UUID productId, @Min(1) @Max(9999) int quantity) {}

    public record InquiryRequest(
            @NotEmpty @Size(max = 20) List<@Valid InquiryLineRequest> items,
            LocalDate expectedDeliveryDate,
            @Size(max = 2000) String deliveryNotes,
            @Size(max = 2000) String purpose,
            @Size(max = 2000) String remark) {}

    public record InquiryReplyLine(
            @NotNull UUID itemId,
            @Positive @Max(99_999_999) Long referenceUnitPriceFen,
            @Size(max = 500) String leadTimeText) {}

    public record InquiryReplyRequest(
            @Positive long expectedVersion,
            @NotBlank @Size(max = 2000) String body,
            @Size(max = 20) List<@Valid InquiryReplyLine> items) {}

    public record VersionCommand(
            @Positive long expectedVersion,
            @NotBlank @Size(min = 2, max = 500) String reason) {}

    public record VersionOnly(@Positive long expectedVersion) {}

    public record InquiryItemView(
            UUID id,
            UUID productId,
            String sku,
            String name,
            long referenceUnitPriceFenSnapshot,
            int minInquiryQuantitySnapshot,
            int quantity,
            Long replyReferenceUnitPriceFen,
            String replyLeadTimeText) {}

    public record InquiryHistoryView(
            String action,
            String fromStatus,
            String toStatus,
            long inquiryVersion,
            UUID actorId,
            String reason,
            Instant createdAt) {}

    public record InquiryView(
            UUID id,
            String inquiryNumber,
            UUID companyId,
            UUID userId,
            String status,
            LocalDate expectedDeliveryDate,
            String deliveryNotes,
            String purpose,
            String remark,
            String publicReply,
            String closeReason,
            long version,
            List<InquiryItemView> items,
            List<InquiryHistoryView> history,
            Instant createdAt,
            Instant updatedAt) {}

    public record CompanyUpdateRequest(
            @Positive long expectedVersion,
            @NotBlank String companyName,
            @NotBlank String businessType,
            @NotBlank String countryOrRegion,
            @NotBlank String city,
            @NotBlank String contactName,
            @NotBlank String phone,
            @NotBlank String cooperationEmail,
            String website,
            @Size(max = 2000) String internalNote,
            @NotNull UUID basisTicketId,
            @NotBlank @Size(min = 2, max = 500) String reason) {}

    public record CompanyView(
            UUID id,
            UUID ownerUserId,
            UUID sourceApplicationId,
            boolean sourcePublicConsent,
            String companyName,
            String businessType,
            String countryOrRegion,
            String city,
            String contactName,
            String phone,
            String cooperationEmail,
            String website,
            String cooperationStatus,
            String internalNote,
            long version,
            Instant createdAt,
            Instant updatedAt) {}

    public record ChannelRequest(
            Long expectedVersion,
            @NotBlank String name,
            @NotBlank String countryOrRegion,
            @NotBlank String city,
            @NotBlank String address,
            @NotBlank String phone,
            String website,
            UUID companyId) {}

    public record ChannelView(
            UUID id,
            String name,
            String countryOrRegion,
            String city,
            String address,
            String phone,
            String website,
            UUID companyId,
            boolean published,
            long version,
            Instant updatedAt) {}

    public record PageResult<T>(List<T> items, int page, int pageSize, long total) {}
}
