package wemove.content.assets;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:content-assets;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.hikari.connection-init-sql=SELECT 1","spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","wemove.bootstrap.admin-email=assets-admin@example.test","wemove.bootstrap.admin-password=AssetsAdminPassword123!"})
@AutoConfigureMockMvc
@Sql(scripts="classpath:org/springframework/session/jdbc/schema-h2.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AssetsWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ContentAssetsPort assets;
    @Autowired TransactionTemplate transactions;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean wemove.operations.audit.DatabaseAuditAdapter audit;

    @Test void visibilityReplacementAndOffliningAreCheckedOnEveryDownload() throws Exception {
        Cookie admin=login("assets-admin@example.test","AssetsAdminPassword123!");
        Cookie user=register("files-user@example.test");
        mvc.perform(get("/api/v1/files?pageSize=100")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/admin/media?pageSize=100").cookie(admin)).andExpect(status().isUnprocessableEntity());
        JsonNode publicFile=upload(admin,"公开资料测试","PUBLIC");
        mvc.perform(get(publicFile.path("downloadUrl").asText())).andExpect(status().isNotFound());
        UUID publishKey=UUID.randomUUID();
        String publishPath="/api/v1/admin/files/"+publicFile.path("id").asText()+"/publish";
        JsonNode published=data(json(post(publishPath).header("Idempotency-Key",publishKey),admin,"{\"expectedVersion\":1}").andExpect(status().isOk()).andReturn());
        json(post(publishPath).header("Idempotency-Key",publishKey),admin,"{\"expectedVersion\":1}").andExpect(status().isOk()).andExpect(header().string("Idempotency-Replayed","true"));
        mvc.perform(get(published.path("downloadUrl").asText())).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        JsonNode dealerFile=publish(admin,upload(admin,"经销保密资料测试","DEALER"));
        JsonNode internal=publish(admin,upload(admin,"内部保密资料测试","INTERNAL"));
        for(JsonNode f:List.of(dealerFile,internal)) {
            mvc.perform(get(f.path("downloadUrl").asText())).andExpect(status().isNotFound());
            mvc.perform(get(f.path("downloadUrl").asText()).cookie(user)).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/files/"+f.path("id").asText())).andExpect(status().isNotFound());
            mvc.perform(get(f.path("downloadUrl").asText()).cookie(admin)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/files?keyword=保密资料测试")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        json(post("/api/v1/dealer-applications").header("Idempotency-Key",UUID.randomUUID()),user,"""
                {"companyName":"资料权限测试商行","businessType":"RETAIL","countryOrRegion":"中国","city":"上海市","contactName":"资料测试员","phone":"13800000000","cooperationEmail":"files-user@example.test","businessChannels":"线下门店经营测试","website":null,"cooperationIntent":"希望采购运动产品开展社区体验活动","publicChannelConsent":false}
                """).andExpect(status().isCreated());
        JsonNode application=data(mvc.perform(get("/api/v1/dealer-applications").cookie(user)).andExpect(status().isOk()).andReturn()).path("items").get(0);
        json(post("/api/v1/admin/dealer-applications/"+application.path("id").asText()+"/review").header("Idempotency-Key",UUID.randomUUID()),admin,"{\"applicationVersion\":1,\"decision\":\"APPROVE\",\"publicReason\":null,\"internalNote\":\"资料权限测试\",\"existingCompanyId\":null}").andExpect(status().isOk());
        mvc.perform(get(dealerFile.path("downloadUrl").asText()).cookie(user)).andExpect(status().isOk());
        JsonNode company=data(mvc.perform(get("/api/v1/admin/companies").cookie(admin)).andExpect(status().isOk()).andReturn()).path("items").get(0);
        json(post("/api/v1/admin/companies/"+company.path("id").asText()+"/suspend").header("Idempotency-Key",UUID.randomUUID()),admin,"{\"expectedVersion\":"+company.path("version").asLong()+",\"reason\":\"测试暂停合作\"}").andExpect(status().isOk());
        mvc.perform(get(dealerFile.path("downloadUrl").asText()).cookie(user)).andExpect(status().isNotFound());
        String replace="/api/v1/admin/files/"+published.path("id").asText()+"/replace";
        JsonNode replaced=data(mvc.perform(multipart(replace).file(new MockMultipartFile("file","new.pdf","application/pdf",AssetRulesTest.pdf())).param("expectedVersion","2").param("versionNote","第二版").cookie(admin).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isOk()).andReturn());
        assertThat(replaced.path("downloadId").asText()).isNotEqualTo(published.path("downloadId").asText());
        mvc.perform(get(published.path("downloadUrl").asText())).andExpect(status().isNotFound());
        mvc.perform(get(replaced.path("downloadUrl").asText())).andExpect(status().isOk());
        json(post("/api/v1/admin/files/"+replaced.path("id").asText()+"/unpublish").header("Idempotency-Key",UUID.randomUUID()),admin,"{\"expectedVersion\":3}").andExpect(status().isOk());
        mvc.perform(get(replaced.path("downloadUrl").asText())).andExpect(status().isNotFound());
        mvc.perform(get(replaced.path("downloadUrl").asText()).cookie(admin)).andExpect(status().isNotFound());
    }

    @Test void failedReplacementKeepsOldBinaryAndPermissionChangesAreAudited() throws Exception {
        Cookie admin=login("assets-admin@example.test","AssetsAdminPassword123!");
        JsonNode file=publish(admin,upload(admin,"替换失败与权限测试","PUBLIC"));
        String path="/api/v1/admin/files/"+file.path("id").asText();
        mvc.perform(multipart(path+"/replace").file(new MockMultipartFile("file","broken.pdf","application/pdf","%PDF-broken".getBytes())).param("expectedVersion","2").param("versionNote","无效替换").cookie(admin).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get(file.path("downloadUrl").asText())).andExpect(status().isOk());
        mvc.perform(get(path).cookie(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2)).andExpect(jsonPath("$.data.downloadId").value(file.path("downloadId").asText()));
        String metadata=mapper.writeValueAsString(Map.of("title","替换失败与权限测试","type","使用说明","versionNote","权限变更","productIds",List.of(),"visibility","INTERNAL","expectedVersion",2));
        json(patch(path),admin,metadata).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(3));
        mvc.perform(get(file.path("downloadUrl").asText())).andExpect(status().isNotFound());
        mvc.perform(get(file.path("downloadUrl").asText()).cookie(admin)).andExpect(status().isOk());
        json(patch(path),admin,metadata).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from operations_audit_records where action='FILE_VISIBILITY_CHANGED'",Long.class)).isGreaterThan(0);
        long before=jdbc.queryForObject("select count(*) from content_assets",Long.class);
        mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file","forged.png","image/png",AssetRulesTest.pdf())).cookie(admin).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("select count(*) from content_assets",Long.class)).isEqualTo(before);
    }

    @Test void failedAuditRollsBackBinaryReplacementWithMetadata() throws Exception {
        Cookie admin=login("assets-admin@example.test","AssetsAdminPassword123!");
        JsonNode file=publish(admin,upload(admin,"审计原子性测试","PUBLIC"));
        long binaryCount=jdbc.queryForObject("select count(*) from content_binaries",Long.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("simulated audit failure"))
                .when((wemove.operations.audit.DatabaseAuditAdapter) org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit)).append(org.mockito.ArgumentMatchers.argThat(event->"FILE_REPLACED".equals(event.action())));
        mvc.perform(multipart("/api/v1/admin/files/"+file.path("id").asText()+"/replace")
                .file(new MockMultipartFile("file","replacement.pdf","application/pdf",AssetRulesTest.pdf()))
                .param("expectedVersion","2").param("versionNote","这版应回滚").cookie(admin).with(csrf())
                .header("Origin","http://localhost:5173")).andExpect(status().isInternalServerError());
        mvc.perform(get(file.path("downloadUrl").asText())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/files/"+file.path("id").asText()).cookie(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.downloadId").value(file.path("downloadId").asText()));
        assertThat(jdbc.queryForObject("select count(*) from content_binaries",Long.class)).isEqualTo(binaryCount);
    }

    @Test void mediaNeedsPublicReferenceAndAllReferencesPreventDeletion() throws Exception {
        Cookie admin=login("assets-admin@example.test","AssetsAdminPassword123!");
        var upload=new MockMultipartFile("file","asset.png","image/png",AssetRulesTest.png());
        mvc.perform(multipart("/api/v1/admin/media").file(upload).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isUnauthorized());
        JsonNode media=data(mvc.perform(multipart("/api/v1/admin/media").file(upload).param("altText","安全测试图").cookie(admin).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isCreated()).andReturn());
        String url=media.path("url").asText(); UUID id=UUID.fromString(media.path("id").asText()); UUID article=UUID.randomUUID();
        mvc.perform(get(url)).andExpect(status().isNotFound()); mvc.perform(get(url).cookie(admin)).andExpect(status().isOk());
        transactions.executeWithoutResult(tx->assets.replaceReferences("ARTICLE",article,List.of(id),true));
        mvc.perform(get(url)).andExpect(status().isOk());
        json(delete("/api/v1/admin/media/"+id+"?expectedVersion=1"),admin,"").andExpect(status().isConflict());
        transactions.executeWithoutResult(tx->assets.replaceReferences("ARTICLE",article,List.of(id),false));
        mvc.perform(get(url)).andExpect(status().isNotFound());
        json(delete("/api/v1/admin/media/"+id+"?expectedVersion=1"),admin,"").andExpect(status().isConflict());
        transactions.executeWithoutResult(tx->assets.replaceReferences("ARTICLE",article,List.of(),false));
        JsonNode product=data(json(post("/api/v1/admin/products").header("Idempotency-Key",UUID.randomUUID()),admin,"{\"name\":\"图片引用商品\",\"mainImageId\":\""+id+"\",\"imageIds\":[],\"initialStock\":0}").andExpect(status().isCreated()).andReturn());
        json(delete("/api/v1/admin/media/"+id+"?expectedVersion=1"),admin,"").andExpect(status().isConflict());
        json(patch("/api/v1/admin/products/"+product.path("id").asText()),admin,"{\"expectedVersion\":"+product.path("version").asLong()+",\"name\":\"图片引用商品\",\"mainImageId\":null,\"imageIds\":[]}").andExpect(status().isOk());
        json(delete("/api/v1/admin/media/"+id+"?expectedVersion=1"),admin,"").andExpect(status().isNoContent());
        mvc.perform(get(url).cookie(admin)).andExpect(status().isNotFound());
        json(post("/api/v1/admin/products").header("Idempotency-Key",UUID.randomUUID()),admin,"{\"name\":\"伪造图片商品\",\"mainImageId\":\"arbitrary-image\",\"imageIds\":[],\"initialStock\":0}").andExpect(status().isUnprocessableEntity());
    }
    private JsonNode upload(Cookie admin,String title,String visibility) throws Exception {
        String metadata=mapper.writeValueAsString(Map.of("title",title,"type","使用说明","versionNote","第一版","visibility",visibility,"productIds",List.of()));
        return data(mvc.perform(multipart("/api/v1/admin/files").file(new MockMultipartFile("file","guide.pdf","application/pdf",AssetRulesTest.pdf())).file(new MockMultipartFile("metadata","","application/json",metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8))).cookie(admin).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isCreated()).andReturn());
    }
    private JsonNode publish(Cookie admin,JsonNode file) throws Exception { return data(json(post("/api/v1/admin/files/"+file.path("id").asText()+"/publish").header("Idempotency-Key",UUID.randomUUID()),admin,"{\"expectedVersion\":1}").andExpect(status().isOk()).andReturn()); }
    private Cookie register(String email) throws Exception {
        json(post("/api/v1/auth/register"),null,"{\"email\":\""+email+"\",\"nickname\":\"资料测试员\",\"password\":\"AssetsUserPassword123!\",\"confirmPassword\":\"AssetsUserPassword123!\",\"adultConfirmed\":true,\"termsVersion\":\"2026-09-05\",\"privacyVersion\":\"2026-09-05\",\"termsAccepted\":true,\"privacyAccepted\":true}").andExpect(status().isCreated());
        return login(email,"AssetsUserPassword123!");
    }
    private Cookie login(String email,String password) throws Exception { return json(post("/api/v1/auth/login"),null,"{\"email\":\""+email+"\",\"password\":\""+password+"\"}").andExpect(status().isOk()).andReturn().getResponse().getCookies()[0]; }
    private JsonNode data(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()).path("data"); }
    private ResultActions json(MockHttpServletRequestBuilder request,Cookie cookie,String body) throws Exception {
        request.with(csrf()).header("Origin","http://localhost:5173").contentType(MediaType.APPLICATION_JSON).content(body);
        if(cookie!=null) request.cookie(cookie); return mvc.perform(request);
    }
}
