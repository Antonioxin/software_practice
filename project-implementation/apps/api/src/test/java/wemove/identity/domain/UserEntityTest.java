package wemove.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserEntityTest {
    @Test
    void newAdultAccountStartsActiveAndKeepsNormalizedIdentity() {
        Instant now = Instant.parse("2026-09-05T00:00:00Z");
        UserEntity user = UserEntity.create(
            "Adult@Example.test", "adult@example.test", "hash", "测试用户", BaseRole.USER, now);

        assertThat(user.getBaseRole()).isEqualTo(BaseRole.USER);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getEmailNormalized()).isEqualTo("adult@example.test");
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void statusChangeReturnsPreviousStateAndUpdatesTimestamp() {
        Instant created = Instant.parse("2026-09-05T00:00:00Z");
        Instant changed = created.plusSeconds(60);
        UserEntity user = UserEntity.create(
            "adult@example.test", "adult@example.test", "hash", "测试用户", BaseRole.USER, created);

        AccountStatus previous = user.changeStatus(AccountStatus.DISABLED, changed);

        assertThat(previous).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(user.getUpdatedAt()).isEqualTo(changed);
    }
}
