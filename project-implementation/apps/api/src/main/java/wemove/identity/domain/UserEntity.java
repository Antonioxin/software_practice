package wemove.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "email_normalized", nullable = false, unique = true, length = 254)
    private String emailNormalized;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 30)
    private String nickname;
    @Column(length = 21)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(name = "base_role", nullable = false, length = 16)
    private BaseRole baseRole;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 16)
    private AccountStatus accountStatus;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {}

    public static UserEntity create(String email, String emailNormalized, String passwordHash,
                                    String nickname, BaseRole role, Instant now) {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID();
        user.email = email;
        user.emailNormalized = emailNormalized;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.baseRole = role;
        user.accountStatus = AccountStatus.ACTIVE;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public void updateProfile(String nickname, String phone, Instant now) {
        this.nickname = nickname;
        this.phone = phone;
        this.updatedAt = now;
    }

    public AccountStatus changeStatus(AccountStatus next, Instant now) {
        AccountStatus previous = accountStatus;
        accountStatus = next;
        updatedAt = now;
        return previous;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getEmailNormalized() { return emailNormalized; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getPhone() { return phone; }
    public BaseRole getBaseRole() { return baseRole; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
