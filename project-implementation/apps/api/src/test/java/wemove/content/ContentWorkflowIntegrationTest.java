package wemove.content;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:content-workflow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa",
        "spring.datasource.password=","spring.datasource.hikari.connection-init-sql=SELECT 1",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
        "wemove.bootstrap.admin-email=content-admin@example.test",
        "wemove.bootstrap.admin-password=ContentTestPassword123!"})
@AutoConfigureMockMvc
class ContentWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    Cookie[] cookies;
    private static final String BASE="/api/v1";

    @Test
    @Sql("classpath:org/springframework/session/jdbc/schema-h2.sql")
    void contentWorkflowEnforcesPublicProjectionVersionsIdempotencyAssetsAndConsent() throws Exception {
        mvc.perform(get(BASE+"/admin/articles")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE+"/admin/articles").with(user("member").roles("USER"))).andExpect(status().isForbidden());
        var login=mvc.perform(post(BASE+"/auth/login").with(csrf()).header("Origin","http://localhost:5173")
                .contentType("application/json").content("{\"email\":\"content-admin@example.test\",\"password\":\"ContentTestPassword123!\"}"))
                .andExpect(status().isOk()).andReturn();
        cookies=login.getResponse().getCookies();

        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var uploaded=mvc.perform(multipart(BASE+"/admin/media").file(new MockMultipartFile("file","guide.png","image/png",bytes.toByteArray()))
                .cookie(cookies).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isCreated()).andReturn();
        String media=data(uploaded).path("id").asText();
        var inlineUpload=mvc.perform(multipart(BASE+"/admin/media").file(new MockMultipartFile("file","inline.png","image/png",bytes.toByteArray()))
                .cookie(cookies).with(csrf()).header("Origin","http://localhost:5173")).andExpect(status().isCreated()).andReturn();
        String inlineMedia=data(inlineUpload).path("id").asText();
        String key=UUID.randomUUID().toString();
        var body=new LinkedHashMap<String,Object>();
        body.put("title","家里的平衡探索"); body.put("summary","每天十分钟的亲子运动");
        body.put("body","<h2>一起玩</h2><p onclick='attack()'>正文<script>attack()</script><a href='javascript:attack()'>链接</a></p>"
                +"<section data-layout='image-left'><figure><img src='/api/v1/media/"+inlineMedia+"/content' alt='亲子游戏'>"
                +"<figcaption>扶稳再出发</figcaption></figure><div><h3>轮流练习</h3><p>一起完成今天的小挑战。</p></div></section>");
        body.put("category","亲子玩法"); body.put("mediaIds",List.of(media)); body.put("productIds",List.of()); body.put("sortOrder",10);
        var created=send(post(BASE+"/admin/articles").header("Idempotency-Key",key),body).andExpect(status().isCreated()).andReturn();
        JsonNode article=data(created); String id=article.path("id").asText(); long version=article.path("version").asLong();
        assertThat(article.path("body").asText()).contains("<h2>一起玩</h2>").doesNotContain("script","onclick","attack()");
        assertThat(article.path("body").asText()).contains("data-layout=\"image-left\"","/api/v1/media/"+inlineMedia+"/content","<figcaption>扶稳再出发</figcaption>");
        assertThat(mapper.convertValue(article.path("mediaIds"),String[].class)).containsExactly(media,inlineMedia);
        assertThat(article.path("pageDescription").asText()).isEqualTo("每天十分钟的亲子运动");
        send(post(BASE+"/admin/articles").header("Idempotency-Key",key),body).andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed","true")).andExpect(jsonPath("$.data.id").value(id));
        var altered=new LinkedHashMap<>(body); altered.put("title","不同请求");
        send(post(BASE+"/admin/articles").header("Idempotency-Key",key),altered).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        mvc.perform(get(BASE+"/articles/"+id)).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/media/"+media+"/content")).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/media/"+inlineMedia+"/content")).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/media/"+media+"/content").cookie(cookies)).andExpect(status().isOk());
        send(delete(BASE+"/admin/media/"+media).param("expectedVersion",data(uploaded).path("version").asText()),Map.of()).andExpect(status().isConflict());
        send(delete(BASE+"/admin/media/"+inlineMedia).param("expectedVersion",data(inlineUpload).path("version").asText()),Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ASSET_IN_USE"));

        var stale=new LinkedHashMap<>(body); stale.put("expectedVersion",version+7);
        send(patch(BASE+"/admin/articles/"+id),stale).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        String publishKey=UUID.randomUUID().toString();
        var published=send(post(BASE+"/admin/articles/"+id+"/publish").header("Idempotency-Key",publishKey),Map.of("expectedVersion",version))
                .andExpect(status().isOk()).andReturn();
        long publishedVersion=data(published).path("version").asLong();
        assertThat(publishedVersion).isGreaterThan(version);
        send(post(BASE+"/admin/articles/"+id+"/publish").header("Idempotency-Key",publishKey),Map.of("expectedVersion",version))
                .andExpect(status().isOk()).andExpect(header().string("Idempotency-Replayed","true"));
        var publicArticle=data(mvc.perform(get(BASE+"/articles/"+id)).andExpect(status().isOk()).andReturn());
        assertThat(publicArticle.path("body")).isEqualTo(article.path("body"));
        assertThat(publicArticle.path("mediaIds")).isEqualTo(article.path("mediaIds"));
        mvc.perform(get(BASE+"/articles").param("category","亲子玩法")).andExpect(jsonPath("$.data[0].id").value(id)).andExpect(jsonPath("$.meta.totalItems").value(1));
        mvc.perform(get(BASE+"/media/"+media+"/content")).andExpect(status().isOk());
        mvc.perform(get(BASE+"/media/"+inlineMedia+"/content")).andExpect(status().isOk());
        send(patch(BASE+"/admin/home"),Map.of("expectedVersion",0,"recommendedProductIds",List.of(),"featuredArticleIds",List.of(id)))
                .andExpect(status().isOk());
        mvc.perform(get(BASE+"/home")).andExpect(jsonPath("$.data.featuredArticles[0].id").value(id));
        var offline=data(send(post(BASE+"/admin/articles/"+id+"/unpublish").header("Idempotency-Key",UUID.randomUUID()),Map.of("expectedVersion",publishedVersion))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(get(BASE+"/articles/"+id)).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/media/"+media+"/content")).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/media/"+inlineMedia+"/content")).andExpect(status().isNotFound());
        mvc.perform(get(BASE+"/home")).andExpect(jsonPath("$.data.featuredArticles").isEmpty());
        var withoutInline=new LinkedHashMap<>(body); withoutInline.put("expectedVersion",offline.path("version").asLong());
        withoutInline.put("body","<h2>一起玩</h2><p>收起图片后的正文。</p>");
        var revised=data(send(patch(BASE+"/admin/articles/"+id),withoutInline).andExpect(status().isOk()).andReturn());
        assertThat(mapper.convertValue(revised.path("mediaIds"),String[].class)).containsExactly(media);
        send(delete(BASE+"/admin/media/"+inlineMedia).param("expectedVersion",data(inlineUpload).path("version").asText()),Map.of())
                .andExpect(status().isNoContent());
        var missingInline=new LinkedHashMap<>(body);
        missingInline.put("body","<p>正文</p><img src='/api/v1/media/"+UUID.randomUUID()+"/content'>");
        send(post(BASE+"/admin/articles").header("Idempotency-Key",UUID.randomUUID()),missingInline)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors[0].field").value("mediaIds"));

        var draft=data(send(post(BASE+"/admin/articles").header("Idempotency-Key",UUID.randomUUID()),Map.of())
                .andExpect(status().isCreated()).andReturn());
        send(post(BASE+"/admin/articles/"+draft.path("id").asText()+"/publish").header("Idempotency-Key",UUID.randomUUID()),Map.of("expectedVersion",0))
                .andExpect(status().isUnprocessableEntity());
        send(post(BASE+"/admin/articles").header("Idempotency-Key",UUID.randomUUID()),Map.of("title","😀".repeat(100)))
                .andExpect(status().isCreated());
        send(post(BASE+"/admin/articles").header("Idempotency-Key",UUID.randomUUID()),Map.of("title","😀".repeat(101)))
                .andExpect(status().isUnprocessableEntity());
        var invalid=new LinkedHashMap<>(body); invalid.put("body","<script>attack()</script>");
        send(post(BASE+"/admin/articles").header("Idempotency-Key",UUID.randomUUID()),invalid).andExpect(status().isUnprocessableEntity());
        invalid=new LinkedHashMap<>(body); invalid.put("productIds",List.of(UUID.randomUUID()));
        send(post(BASE+"/admin/articles").header("Idempotency-Key",UUID.randomUUID()),invalid).andExpect(status().isUnprocessableEntity());
        send(post(BASE+"/admin/banners").header("Idempotency-Key",UUID.randomUUID()),Map.of("title","危险跳转","targetUrl","javascript:attack()"))
                .andExpect(status().isUnprocessableEntity());
        var banner=data(send(post(BASE+"/admin/banners").header("Idempotency-Key",UUID.randomUUID()),Map.of("title","等候图片","targetUrl","/articles"))
                .andExpect(status().isCreated()).andReturn());
        send(post(BASE+"/admin/banners/"+banner.path("id").asText()+"/publish").header("Idempotency-Key",UUID.randomUUID()),Map.of("expectedVersion",banner.path("version").asLong()))
                .andExpect(status().isUnprocessableEntity());
        var faq=data(send(post(BASE+"/admin/faqs").header("Idempotency-Key",UUID.randomUUID()),Map.of("question","怎样开始？","answer","<p>一起尝试。</p>","category","玩法"))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(get(BASE+"/faqs/"+faq.path("id").asText())).andExpect(status().isNotFound());
        send(post(BASE+"/admin/faqs/"+faq.path("id").asText()+"/publish").header("Idempotency-Key",UUID.randomUUID()),Map.of("expectedVersion",faq.path("version").asLong()))
                .andExpect(status().isOk());
        mvc.perform(get(BASE+"/faqs").param("category","玩法")).andExpect(jsonPath("$.data[0].question").value("怎样开始？"));

        var settings=data(mvc.perform(get(BASE+"/admin/site-settings").cookie(cookies)).andExpect(status().isOk()).andReturn());
        var settingsBody=mapper.convertValue(settings,new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String,Object>>(){});
        settingsBody.put("expectedVersion",settingsBody.remove("version")); settingsBody.put("termsText","<p>已经更新的服务条款。</p>");
        send(patch(BASE+"/admin/site-settings"),settingsBody).andExpect(status().isUnprocessableEntity());
        String oldTerms=settings.path("termsVersion").asText();
        settingsBody.put("termsVersion","2026-09-08-r2");
        send(patch(BASE+"/admin/site-settings"),settingsBody).andExpect(status().isOk());
        mvc.perform(get(BASE+"/auth/registration-policy")).andExpect(jsonPath("$.data.termsVersion").value("2026-09-08-r2"));
        Map<String,Object> registration=new LinkedHashMap<>(Map.of("email","stale-consent@example.test","nickname","测试用户",
                "password","MemberTestPassword123!","confirmPassword","MemberTestPassword123!","adultConfirmed",true,
                "termsAccepted",true,"privacyAccepted",true,"termsVersion",oldTerms,"privacyVersion",settings.path("privacyVersion").asText()));
        mvc.perform(post(BASE+"/auth/register").with(csrf()).header("Origin","http://localhost:5173").contentType("application/json").content(mapper.writeValueAsString(registration)))
                .andExpect(status().isUnprocessableEntity());
    }
    private ResultActions send(MockHttpServletRequestBuilder request,Object body) throws Exception {
        return mvc.perform(request.cookie(cookies).with(csrf()).header("Origin","http://localhost:5173")
                .contentType("application/json").content(mapper.writeValueAsString(body)));
    }
    private JsonNode data(MvcResult result) throws Exception {return mapper.readTree(result.getResponse().getContentAsString()).path("data");}
}
