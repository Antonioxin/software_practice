package wemove.identity.api;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record RegistrationPolicy(
        String adultStatement, String termsVersion, String termsPath,
        String privacyVersion, String privacyPath) {}

    public record RegisterRequest(
        @NotBlank @Size(max = 254) @Email String email,
        @NotBlank String nickname,
        @NotNull String password,
        @NotNull String confirmPassword,
        @AssertTrue boolean adultConfirmed,
        @NotBlank String termsVersion,
        @NotBlank String privacyVersion,
        @AssertTrue boolean termsAccepted,
        @AssertTrue boolean privacyAccepted) {}

    public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotNull @Size(max = 64) String password) {}

    public record ProfileRequest(
        @NotBlank String nickname,
        @Size(max = 40) String phone) {}

    public record AccountCommand(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank @Size(min = 2, max = 500) String reason) {}

    public record Actor(
        UUID id, String email, String nickname, String phone, String baseRole,
        String accountStatus, String derivedIdentity, Set<String> capabilities, long version) {}

    public record UserSummary(
        UUID id, String email, String nickname, String phone, String baseRole,
        String accountStatus, String derivedIdentity, long version,
        Instant createdAt, Instant updatedAt) {}

    public record UserDetail(UserSummary account, List<StatusHistory> statusHistory) {}

    public record StatusHistory(String action, String previousStatus, String newStatus,
                                String reason, Instant createdAt) {}

    public record PageMeta(int page, int pageSize, long totalItems, int totalPages) {}

    public record CsrfPayload(String token, String headerName) {}
}
