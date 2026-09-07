package wemove.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import wemove.platform.api.ApiException;
import wemove.support.domain.TicketRules;
import wemove.support.domain.TicketStatus;
import wemove.support.domain.TicketType;

import java.util.List;
import java.util.UUID;

/** BR-05 状态机与文本规则（码点计数）的纯函数测试。 */
class TicketRulesTest {
    @Test
    void subjectLengthBoundsByCodePoints() {
        assertThat(TicketRules.text("subject", "  ab  ", 2, 100)).isEqualTo("ab");
        assertThat(TicketRules.text("subject", "a".repeat(100), 2, 100)).hasSize(100);
        assertThat(TicketRules.text("subject", "您好".repeat(50), 2, 100)).hasSize(100);
        assertThatThrownBy(() -> TicketRules.text("subject", "a", 2, 100))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.text("subject", "a".repeat(101), 2, 100))
                .isInstanceOf(ApiException.class);
        // 码点计数：100 个 emoji（每个 2 个 char）仍是 100 码点
        assertThat(TicketRules.text("subject", "😀".repeat(100), 2, 100)).hasSize(200);
        assertThatThrownBy(() -> TicketRules.text("subject", "😀".repeat(101), 2, 100))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void bodyLengthBounds() {
        assertThat(TicketRules.text("body", "a".repeat(10), 10, 2000)).hasSize(10);
        assertThat(TicketRules.text("body", "a".repeat(2000), 10, 2000)).hasSize(2000);
        assertThatThrownBy(() -> TicketRules.text("body", "a".repeat(9), 10, 2000))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.text("body", "a".repeat(2001), 10, 2000))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void optionalPhoneNormalizesOrRejects() {
        assertThat(TicketRules.phone(null)).isNull();
        assertThat(TicketRules.phone("   ")).isNull();
        assertThat(TicketRules.phone("+86 138-0000-0000")).isEqualTo("+8613800000000");
        assertThat(TicketRules.phone("13800000000")).isEqualTo("13800000000");
        assertThatThrownBy(() -> TicketRules.phone("abc")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.phone("123")).isInstanceOf(ApiException.class);
    }

    @Test
    void typeAssociations() {
        UUID id = UUID.randomUUID();
        assertThatCode(() -> TicketRules.association(TicketType.GENERAL, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> TicketRules.association(TicketType.PRODUCT, id, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> TicketRules.association(TicketType.AFTER_SALES, null, id))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TicketRules.association(TicketType.GENERAL, id, null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.association(TicketType.GENERAL, null, id))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.association(TicketType.PRODUCT, null, null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.association(TicketType.PRODUCT, id, id))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.association(TicketType.AFTER_SALES, null, null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TicketRules.association(TicketType.AFTER_SALES, id, id))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void followUpTransitions() {
        assertThat(TicketRules.afterFollowUp(TicketStatus.NEW)).isEqualTo(TicketStatus.NEW);
        assertThat(TicketRules.afterFollowUp(TicketStatus.PROCESSING))
                .isEqualTo(TicketStatus.PROCESSING);
        assertThat(TicketRules.afterFollowUp(TicketStatus.REPLIED))
                .isEqualTo(TicketStatus.PROCESSING);
        assertThatThrownBy(() -> TicketRules.afterFollowUp(TicketStatus.CLOSED))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void replyTransitions() {
        for (TicketStatus status : List.of(TicketStatus.NEW, TicketStatus.PROCESSING, TicketStatus.REPLIED))
            assertThat(TicketRules.afterReply(status)).isEqualTo(TicketStatus.REPLIED);
        assertThatThrownBy(() -> TicketRules.afterReply(TicketStatus.CLOSED))
                .isInstanceOf(ApiException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEW", "PROCESSING", "REPLIED"})
    void openStatesAcceptCloseButOnlyNewAcceptsStart(String status) {
        TicketRules.requireOpen(TicketStatus.valueOf(status));
        TicketRules.requireStartable(TicketStatus.NEW);
        assertThatThrownBy(() -> TicketRules.requireOpen(TicketStatus.CLOSED))
                .isInstanceOf(ApiException.class);
        for (TicketStatus other : List.of(TicketStatus.PROCESSING, TicketStatus.REPLIED, TicketStatus.CLOSED))
            assertThatThrownBy(() -> TicketRules.requireStartable(other))
                    .isInstanceOf(ApiException.class);
    }

    @Test
    void allowedActions() {
        assertThat(TicketRules.userActions(TicketStatus.CLOSED)).isEmpty();
        assertThat(TicketRules.userActions(TicketStatus.REPLIED))
                .containsExactly("FOLLOW_UP", "CLOSE");
        assertThat(TicketRules.adminActions(TicketStatus.CLOSED)).isEmpty();
        assertThat(TicketRules.adminActions(TicketStatus.NEW))
                .containsExactly("START", "REPLY", "NOTE", "CLOSE");
        assertThat(TicketRules.adminActions(TicketStatus.PROCESSING))
                .containsExactly("REPLY", "NOTE", "CLOSE");
        assertThat(TicketRules.adminActions(TicketStatus.REPLIED))
                .containsExactly("REPLY", "NOTE", "CLOSE");
    }

    @Test
    void versionMismatchConflicts() {
        assertThatCode(() -> TicketRules.version(3, 3)).doesNotThrowAnyException();
        ApiException ex = catchThrowableOfType(() -> TicketRules.version(3, 2), ApiException.class);
        assertThat(ex.getCode()).isEqualTo("VERSION_CONFLICT");
    }
}
