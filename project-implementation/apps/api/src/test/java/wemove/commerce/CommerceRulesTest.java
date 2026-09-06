package wemove.commerce;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import wemove.commerce.api.CommerceDtos.ShippingAddress;
import wemove.commerce.domain.CommerceRules;

class CommerceRulesTest {
    @Test
    void integerAmountsCoverReferenceAndMaximumCart() {
        assertThat(CommerceRules.subtotal(7435, 2)).isEqualTo(14870);
        assertThat(Math.multiplyExact(CommerceRules.subtotal(99999999, 99), 20))
                .isEqualTo(197999998020L);
        assertThatThrownBy(() -> CommerceRules.subtotal(100, 100))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void addressUsesCodePointsAndNormalizesPhone() {
        var a =
                CommerceRules.address(
                        new ShippingAddress(
                                "测试收件人", "+86 (138) 0000-0000", "中国", null, "上海", "测试地址123号"));
        assertThat(a.phone()).isEqualTo("+8613800000000");
        assertThat(a.region()).isNull();
        assertThat(CommerceRules.text("recipient", "😀".repeat(50), 2, 50)).hasSize(100);
        assertThatThrownBy(() -> CommerceRules.text("recipient", "😀".repeat(51), 2, 50))
                .isInstanceOf(RuntimeException.class);
    }
}
