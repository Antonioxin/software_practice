package wemove.content;

import static wemove.content.ContentDtos.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import wemove.platform.api.ApiEnvelope;

@RestController
@RequestMapping("/api/v1")
public class PublicContentController {
    private final ContentService content;
    public PublicContentController(ContentService content) {this.content=content;}
    @GetMapping("/articles")
    public ResponseEntity<ApiEnvelope<List<ArticleView>>> articles(
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String category,
            @RequestParam(required=false) UUID productId,@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="12") int pageSize) {
        return page(content.articles(false,keyword,null,category,productId,page,pageSize),page,pageSize);
    }
    @GetMapping("/articles/{id}")
    public ResponseEntity<ApiEnvelope<ArticleView>> article(@PathVariable UUID id) {return ok(content.article(id,false));}
    @GetMapping("/faqs")
    public ResponseEntity<ApiEnvelope<List<FaqView>>> faqs(
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String category,
            @RequestParam(required=false) UUID productId,@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int pageSize) {
        return page(content.faqs(false,keyword,null,category,productId,page,pageSize),page,pageSize);
    }
    @GetMapping("/faqs/{id}")
    public ResponseEntity<ApiEnvelope<FaqView>> faq(@PathVariable UUID id) {return ok(content.faq(id,false));}
    @GetMapping("/site-settings")
    public ResponseEntity<ApiEnvelope<SettingsView>> settings() {return ok(content.settings());}
    @GetMapping("/home")
    public ResponseEntity<ApiEnvelope<HomeView>> home() {return ok(content.home());}
    private <T> ResponseEntity<ApiEnvelope<T>> ok(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(body));
    }
    private <T> ResponseEntity<ApiEnvelope<List<T>>> page(Page<T> page,int number,int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.page(page.getContent(),
                new PageMeta(number,size,page.getTotalElements(),page.getTotalPages())));
    }
}
