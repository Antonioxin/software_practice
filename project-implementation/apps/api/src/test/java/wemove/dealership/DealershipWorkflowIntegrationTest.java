package wemove.dealership;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.UUID;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dealership-flow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.hikari.connection-init-sql=SELECT 1",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "wemove.bootstrap.admin-email=dealer-admin@example.test",
        "wemove.bootstrap.admin-password=DealerAdminPassword123!"
})
@AutoConfigureMockMvc
class DealershipWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    @Sql("classpath:org/springframework/session/jdbc/schema-h2.sql")
    void applicationApprovalActivatesDealerAndPublicChannelRequiresExplicitPublish() throws Exception {
        json(post("/api/v1/auth/register"), null, """
                {"email":"dealer-user@example.test","nickname":"测试申请人","password":"DealerUserPassword123!","confirmPassword":"DealerUserPassword123!","adultConfirmed":true,"termsVersion":"2026-09-05","privacyVersion":"2026-09-05","termsAccepted":true,"privacyAccepted":true}
                """).andExpect(status().isCreated());
        Cookie user = login("dealer-user@example.test", "DealerUserPassword123!");

        MvcResult created = json(post("/api/v1/dealer-applications")
                        .header("Idempotency-Key", UUID.randomUUID()), user, """
                {"companyName":"测试星河体育用品商行","businessType":"RETAIL","countryOrRegion":"中国","city":"上海市","contactName":"测试联系人","phone":"+86 (138) 0000-0000","cooperationEmail":"dealer.contact@example.test","businessChannels":"测试线下门店与网上销售","website":null,"cooperationIntent":"希望采购测试运动产品并开展线下体验活动","publicChannelConsent":true}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.versions[0].phone").value("+8613800000000"))
                .andReturn();
        JsonNode application = mapper.readTree(created.getResponse().getContentAsString()).path("data");

        Cookie admin = login("dealer-admin@example.test", "DealerAdminPassword123!");
        json(post("/api/v1/admin/dealer-applications/" + application.path("id").asText() + "/review")
                        .header("Idempotency-Key", UUID.randomUUID()), admin,
                "{\"applicationVersion\":1,\"decision\":\"APPROVE\",\"publicReason\":null,\"internalNote\":\"测试审核通过\",\"existingCompanyId\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mvc.perform(get("/api/v1/auth/me").cookie(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.derivedIdentity").value("DEALER"))
                .andExpect(jsonPath("$.data.capabilities").value(org.hamcrest.Matchers.hasItem("DEALER_CATALOG_READ")));

        MvcResult categoryCreated = json(post("/api/v1/admin/categories")
                        .header("Idempotency-Key", UUID.randomUUID()), admin,
                "{\"name\":\"测试经销分类\",\"description\":\"D 集成测试\",\"sortOrder\":1,\"enabled\":true}")
                .andExpect(status().isCreated()).andReturn();
        String categoryId = mapper.readTree(categoryCreated.getResponse().getContentAsString()).path("data").path("id").asText();
        String productBody = """
                {"sku":"TEST-D-001","name":"测试经销商品一","categoryId":"%s","summary":"测试经销商品简述","description":"用于 D 模块集成测试的经销商品。","ageMin":5,"ageMax":12,"playType":"BALANCE","scene":"BOTH","material":"测试安全材料","dimensions":"20 x 20 cm","packageContents":"测试组件一套","instructions":"请按测试说明使用","safetyNotes":"测试时需成人看护","mainImageId":"test-dealer-image","imageIds":[],"retailUnitPriceFen":2990,"dealerEnabled":true,"dealerReferenceUnitPriceFen":2000,"minInquiryQuantity":12,"leadTimeText":"测试 7 天","displayOrder":1,"initialStock":10}
                """.formatted(categoryId);
        MvcResult productCreated = json(post("/api/v1/admin/products")
                        .header("Idempotency-Key", UUID.randomUUID()), admin, productBody)
                .andExpect(status().isCreated()).andReturn();
        JsonNode product = mapper.readTree(productCreated.getResponse().getContentAsString()).path("data");
        json(post("/api/v1/admin/products/" + product.path("id").asText() + "/publish")
                        .header("Idempotency-Key", UUID.randomUUID()), admin,
                "{\"expectedVersion\":" + product.path("version").asLong() + "}")
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/dealer/catalog").cookie(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].referenceUnitPriceFen").value(2000))
                .andExpect(jsonPath("$.data[0].availableQuantity").value(10));
        MvcResult inquiryCreated = json(post("/api/v1/inquiries")
                        .header("Idempotency-Key", UUID.randomUUID()), user,
                "{\"items\":[{\"productId\":\"" + product.path("id").asText() + "\",\"quantity\":500}],\"expectedDeliveryDate\":null,\"deliveryNotes\":null,\"purpose\":\"测试活动采购\",\"remark\":null}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].quantity").value(500))
                .andExpect(jsonPath("$.data.items[0].referenceUnitPriceFenSnapshot").value(2000))
                .andReturn();
        JsonNode inquiry = mapper.readTree(inquiryCreated.getResponse().getContentAsString()).path("data");
        json(post("/api/v1/admin/inquiries/" + inquiry.path("id").asText() + "/replies")
                        .header("Idempotency-Key", UUID.randomUUID()), admin,
                "{\"expectedVersion\":" + inquiry.path("version").asLong() + ",\"body\":\"测试回复：可安排分批备货\",\"items\":[{\"itemId\":\"" + inquiry.path("items").get(0).path("id").asText() + "\",\"referenceUnitPriceFen\":1950,\"leadTimeText\":\"测试 10 天\"}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REPLIED"));
        mvc.perform(get("/api/v1/inquiries/" + inquiry.path("id").asText()).cookie(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicReply").value("测试回复：可安排分批备货"))
                .andExpect(jsonPath("$.data.items[0].replyReferenceUnitPriceFen").value(1950));

        MvcResult channelCreated = json(post("/api/v1/admin/channels")
                        .header("Idempotency-Key", UUID.randomUUID()), admin, """
                {"expectedVersion":null,"name":"测试独立渠道","countryOrRegion":"中国","city":"上海市","address":"上海市测试路 18 号","phone":"+86 21 0000 0018","website":null,"companyId":null}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.published").value(false))
                .andReturn();
        JsonNode channel = mapper.readTree(channelCreated.getResponse().getContentAsString()).path("data");
        mvc.perform(get("/api/v1/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        json(post("/api/v1/admin/channels/" + channel.path("id").asText() + "/publish")
                        .header("Idempotency-Key", UUID.randomUUID()), admin,
                "{\"expectedVersion\":" + channel.path("version").asLong() + ",\"reason\":\"测试发布渠道\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(true));
        mvc.perform(get("/api/v1/channels?countryOrRegion=中国&city=上海市"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("测试独立渠道"));
    }

    private Cookie login(String email, String password) throws Exception {
        MvcResult result = json(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookies()[0];
    }

    private ResultActions json(MockHttpServletRequestBuilder request, Cookie cookie, String body) throws Exception {
        request.with(csrf()).header("Origin", "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (cookie != null) request.cookie(cookie);
        return mvc.perform(request);
    }
}
