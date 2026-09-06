package wemove.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
public class UserConsentEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "adult_confirmed_at", nullable = false)
    private Instant adultConfirmedAt;
    @Column(name = "terms_version", nullable = false, length = 32)
    private String termsVersion;
    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;
    @Column(name = "privacy_version", nullable = false, length = 32)
    private String privacyVersion;
    @Column(name = "privacy_accepted_at", nullable = false)
    private Instant privacyAcceptedAt;

    protected UserConsentEntity() {}

    public UserConsentEntity(UUID userId, String termsVersion, String privacyVersion, Instant now) {
        this.userId = userId;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        this.adultConfirmedAt = now;
        this.termsAcceptedAt = now;
        this.privacyAcceptedAt = now;
    }

    public UUID getUserId() { return userId; }
    public String getTermsVersion() { return termsVersion; }
    public String getPrivacyVersion() { return privacyVersion; }
    public Instant getAdultConfirmedAt() { return adultConfirmedAt; }
}
