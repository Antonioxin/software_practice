package wemove.content;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import wemove.platform.api.ApiException;

class ContentRulesTest {
    private static final String GUIDE="/assets/products/guides/balance-stones-guide.png";
    private static final String IMAGE="/api/v1/media/e1000000-0000-4000-8000-000000000001/content";

    @Test void articleLayoutsSurviveCleaningAndRepeatedEdits() {
        String body="<h2>平衡小游戏</h2><section data-layout='image-left'><figure><img src='"+GUIDE+"' alt='踏石与卡通伙伴'>"
                +"<figcaption>从一小步开始</figcaption></figure><div><h3>排成小路</h3><p>扶稳后<strong>再出发</strong>。</p></div></section>"
                +"<section data-layout='image-right'><figure><img src='"+IMAGE+"' alt='游戏实拍'></figure><div><p>左右脚轮流前进。</p></div></section>"
                +"<figure><img src='"+GUIDE+"'><figcaption>全家一起玩</figcaption></figure>";
        String cleaned=ContentRules.articleBody(body,true);
        assertThat(cleaned).contains("data-layout=\"image-left\"","data-layout=\"image-right\"","src=\""+GUIDE+"\"",
                "src=\""+IMAGE+"\"","<figcaption>从一小步开始</figcaption>","<strong>再出发</strong>","alt=\"踏石与卡通伙伴\"");
        assertThat(ContentRules.articleBody(cleaned,true)).isEqualTo(cleaned);
        assertThat(ContentRules.body(body,"answer",5000,true)).doesNotContain("<img","<figure","data-layout");
        assertThat(ContentRules.html(body,"brandDescription",true)).doesNotContain("<img","<figure","data-layout");
    }
    @Test void articleImagesOnlyUseApprovedSourcesAndLayoutAttributes() {
        for (String source:List.of("https://evil.test/image.png","//evil.test/x","data:image/png;base64,a","javascript:attack()",
                "/assets/products/guides/other-guide.png",GUIDE+"?track=1",GUIDE+"#fragment",GUIDE+"/../other.png",
                "/api/v1/media/1-1-1-1-1/content",IMAGE.toUpperCase(Locale.ROOT),IMAGE+"?download=true"," "+GUIDE)) {
            assertThat(ContentRules.articleBody("<p>正文</p><img src='"+source+"'>",true)).as(source).doesNotContain("<img");
        }
        String cleaned=ContentRules.articleBody("<section data-layout='fixed' style='position:fixed' class='popup'><p>正文</p></section>"
                +"<section data-layout='image-left' id='body'><figure onclick='attack()'><img src='"+GUIDE+"'"
                +" srcset='https://evil.test/x 2x' onerror='attack()' style='display:none' width='9999' class='hidden'></figure>"
                +"<div contenteditable='true'><p>继续玩</p></div></section><svg onload='attack()'></svg><iframe src='https://evil.test'></iframe>",true);
        assertThat(cleaned).contains("data-layout=\"image-left\"","src=\""+GUIDE+"\"")
                .doesNotContain("fixed","style=","class=","id=","onclick","srcset","onerror","width=","contenteditable","<svg","<iframe","attack()");
    }
    @Test void mergesBodyImagesWithExplicitCoverAndEnforcesCombinedLimit() {
        UUID cover=UUID.randomUUID(), first=UUID.randomUUID(), second=UUID.randomUUID();
        String body="<p>正文</p><img src='/api/v1/media/"+first+"/content'><img src='/api/v1/media/"+cover+"/content'>"
                +"<img src='/api/v1/media/"+second+"/content'><img src='/api/v1/media/"+first+"/content'><img src='"+GUIDE+"'>";
        assertThat(ContentRules.articleMedia(List.of(cover),ContentRules.articleBody(body,true))).containsExactly(cover,first,second);
        var twenty=new ArrayList<UUID>(); for(int i=0;i<20;i++) twenty.add(UUID.randomUUID());
        assertThat(ContentRules.articleMedia(twenty,"<p>正文</p>")).hasSize(20);
        assertThatThrownBy(()->ContentRules.articleMedia(twenty,body)).isInstanceOf(ApiException.class).hasMessageContaining("20");
        assertThatThrownBy(()->ContentRules.articleMedia(List.of(cover,cover),body)).isInstanceOf(ApiException.class);
    }

    @Test void removesExecutableMarkupAndPreservesAllowedFormatting() {
        String result=ContentRules.html("<h2>玩法</h2><p onclick='alert(1)'>一起<strong>运动</strong>"
                +"<script>alert(1)</script><svg onload='alert(1)'></svg><img src=x onerror='alert(1)'>"
                +"<a href='jav&#x61;script:alert(1)' style='color:red'>危险链接</a>"
                +"<a href='https://example.test/guide'>说明</a></p>","body",true);
        assertThat(result).contains("<h2>玩法</h2>","<strong>运动</strong>","https://example.test/guide")
                .doesNotContain("onclick","script:","<script","<svg","<img","style=","alert(1)");
    }
    @Test void preservesSiteLinksAndCountsUnicodeCodePoints() {
        assertThat(ContentRules.html("<p><a href='/articles'>玩法</a><a href='//evil.test'>其他</a></p>","body",true))
                .contains("href=\"/articles\"").doesNotContain("//evil.test");
        assertThat(ContentRules.bounded("😀".repeat(100),"title",2,100,true)).hasSize(200);
        assertThatThrownBy(()->ContentRules.bounded("😀".repeat(101),"title",2,100,true)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->ContentRules.bounded("测","title",2,100,true)).isInstanceOf(ApiException.class);
        assertThat(ContentRules.body("<p>"+"😀".repeat(20000)+"</p>","body",20000,true)).contains("😀");
        assertThatThrownBy(()->ContentRules.body("测".repeat(20001),"body",20000,true)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->ContentRules.body("😀".repeat(5001),"answer",5000,true)).isInstanceOf(ApiException.class);
        assertThat(ContentRules.body("","body",20000,false)).isEmpty();
        assertThatThrownBy(()->ContentRules.body("","body",20000,true)).isInstanceOf(ApiException.class);
        String url="https://example.test/"+"a".repeat(2027);
        assertThat(ContentRules.target(url)).hasSize(2048);
        assertThatThrownBy(()->ContentRules.target(url+"a")).isInstanceOf(ApiException.class);
    }
    @Test void rejectsBodiesThatBecomeEmptyAndUnsafeBannerLinks() {
        assertThatThrownBy(()->ContentRules.html("<script>attack()</script>","body",true)).isInstanceOf(ApiException.class);
        for(String url:new String[]{"javascript:alert(1)","//evil.test","/\\evil.test","data:text/html,a","https://user@evil.test"})
            assertThatThrownBy(()->ContentRules.target(url)).as(url).isInstanceOf(ApiException.class);
        assertThat(ContentRules.target("/products?category=play")).isEqualTo("/products?category=play");
        assertThat(ContentRules.target("https://example.test/guide")).isEqualTo("https://example.test/guide");
    }
}
