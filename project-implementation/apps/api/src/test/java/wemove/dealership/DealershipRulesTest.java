package wemove.dealership;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import wemove.dealership.service.DealershipRules;
import wemove.platform.api.ApiException;

class DealershipRulesTest {
    @Test
    void normalizesPhoneAndRejectsUnsafeWebsite() {
        assertThat(DealershipRules.phone("+86 (138) 0000-0000")).isEqualTo("+8613800000000");
        assertThatThrownBy(() -> DealershipRules.website("javascript:alert(1)"))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void appliesCodePointLimitsAndBusinessTypeWhitelist() {
        assertThat(DealershipRules.text("companyName", "😀😀", 2, 100)).isEqualTo("😀😀");
        assertThat(DealershipRules.businessType("retail")).isEqualTo("RETAIL");
        assertThatThrownBy(() -> DealershipRules.text("companyName", "😀", 2, 100))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> DealershipRules.businessType("ADMIN"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void versionConflictUsesStableCode() {
        assertThatThrownBy(() -> DealershipRules.version(1, 2))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo("VERSION_CONFLICT");
    }
}
