package wemove.content;

import static wemove.content.ContentDtos.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import wemove.platform.api.*;
import wemove.platform.idempotency.IdempotencyExecutor;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminContentController {
    private final ContentService content;
    private final IdentityPort identity;
    public AdminContentController(ContentService content,IdentityPort identity) {this.content=content;this.identity=identity;}

    @GetMapping("/articles")
    public ResponseEntity<ApiEnvelope<List<ArticleView>>> articles(
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String status,
            @RequestParam(required=false) String category,@RequestParam(required=false) UUID productId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize,
            Authentication authentication) {
        admin(authentication); return page(content.articles(true,keyword,status,category,productId,page,pageSize),page,pageSize);
    }
    @GetMapping("/articles/{id}")
    public ResponseEntity<ApiEnvelope<ArticleView>> article(@PathVariable UUID id,Authentication authentication) {
        admin(authentication); return ok(content.article(id,true));
    }
    @PostMapping("/articles")
    public ResponseEntity<ApiEnvelope<ArticleView>> createArticle(
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody ArticleRequest request,
            Authentication authentication) {
        var result=content.createArticle(admin(authentication),key,request);
        return result(result,"/api/v1/admin/articles/"+result.value().id());
    }
    @PatchMapping("/articles/{id}")
    public ResponseEntity<ApiEnvelope<ArticleView>> updateArticle(@PathVariable UUID id,
            @Valid @RequestBody ArticleRequest request,Authentication authentication) {
        return ok(content.updateArticle(admin(authentication),id,request));
    }
    @PostMapping("/articles/{id}/publish")
    public ResponseEntity<ApiEnvelope<ArticleView>> publishArticle(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody VersionCommand request,
            Authentication authentication) {
        return result(content.publication(admin(authentication),id,key,"ARTICLE",request,true,ArticleView.class),null);
    }
    @PostMapping("/articles/{id}/unpublish")
    public ResponseEntity<ApiEnvelope<ArticleView>> unpublishArticle(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody VersionCommand request,
            Authentication authentication) {
        return result(content.publication(admin(authentication),id,key,"ARTICLE",request,false,ArticleView.class),null);
    }

    @GetMapping("/faqs")
    public ResponseEntity<ApiEnvelope<List<FaqView>>> faqs(
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String status,
            @RequestParam(required=false) String category,@RequestParam(required=false) UUID productId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize,
            Authentication authentication) {
        admin(authentication); return page(content.faqs(true,keyword,status,category,productId,page,pageSize),page,pageSize);
    }
    @GetMapping("/faqs/{id}")
    public ResponseEntity<ApiEnvelope<FaqView>> faq(@PathVariable UUID id,Authentication authentication) {
        admin(authentication); return ok(content.faq(id,true));
    }
    @PostMapping("/faqs")
    public ResponseEntity<ApiEnvelope<FaqView>> createFaq(
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody FaqRequest request,
            Authentication authentication) {
        var result=content.createFaq(admin(authentication),key,request);
        return result(result,"/api/v1/admin/faqs/"+result.value().id());
    }
    @PatchMapping("/faqs/{id}")
    public ResponseEntity<ApiEnvelope<FaqView>> updateFaq(@PathVariable UUID id,
            @Valid @RequestBody FaqRequest request,Authentication authentication) {
        return ok(content.updateFaq(admin(authentication),id,request));
    }
    @PostMapping("/faqs/{id}/publish")
    public ResponseEntity<ApiEnvelope<FaqView>> publishFaq(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody VersionCommand request,
            Authentication authentication) {
        return result(content.publication(admin(authentication),id,key,"FAQ",request,true,FaqView.class),null);
    }
    @PostMapping("/faqs/{id}/unpublish")
    public ResponseEntity<ApiEnvelope<FaqView>> unpublishFaq(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody VersionCommand request,
            Authentication authentication) {
        return result(content.publication(admin(authentication),id,key,"FAQ",request,false,FaqView.class),null);
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiEnvelope<List<BannerView>>> banners(
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String status,
            @RequestParam(required=false) String category,@RequestParam(required=false) UUID productId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize,
            Authentication authentication) {
        admin(authentication); return page(content.banners(keyword,status,page,pageSize),page,pageSize);
    }
    @GetMapping("/banners/{id}")
    public ResponseEntity<ApiEnvelope<BannerView>> banner(@PathVariable UUID id,Authentication authentication) {
        admin(authentication); return ok(content.banner(id));
    }
    @PostMapping("/banners")
    public ResponseEntity<ApiEnvelope<BannerView>> createBanner(
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody BannerRequest request,
            Authentication authentication) {
        var result=content.createBanner(admin(authentication),key,request);
        return result(result,"/api/v1/admin/banners/"+result.value().id());
    }
    @PatchMapping("/banners/{id}")
    public ResponseEntity<ApiEnvelope<BannerView>> updateBanner(@PathVariable UUID id,
            @Valid @RequestBody BannerRequest request,Authentication authentication) {
        return ok(content.updateBanner(admin(authentication),id,request));
    }
    @PostMapping("/banners/{id}/publish")
    public ResponseEntity<ApiEnvelope<BannerView>> publishBanner(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody VersionCommand request,
            Authentication authentication) {
        return result(content.publication(admin(authentication),id,key,"BANNER",request,true,BannerView.class),null);
    }
    @PostMapping("/banners/{id}/unpublish")
    public ResponseEntity<ApiEnvelope<BannerView>> unpublishBanner(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody VersionCommand request,
            Authentication authentication) {
        return result(content.publication(admin(authentication),id,key,"BANNER",request,false,BannerView.class),null);
    }

    @GetMapping("/site-settings")
    public ResponseEntity<ApiEnvelope<SettingsView>> settings(Authentication authentication) {
        admin(authentication); return ok(content.settings());
    }
    @PatchMapping("/site-settings")
    public ResponseEntity<ApiEnvelope<SettingsView>> updateSettings(@Valid @RequestBody SettingsRequest request,Authentication authentication) {
        return ok(content.updateSettings(admin(authentication),request));
    }
    @GetMapping("/home")
    public ResponseEntity<ApiEnvelope<HomeAdminView>> home(Authentication authentication) {
        admin(authentication); return ok(content.homeAdmin());
    }
    @PatchMapping("/home")
    public ResponseEntity<ApiEnvelope<HomeAdminView>> updateHome(@Valid @RequestBody HomeRequest request,Authentication authentication) {
        return ok(content.updateHome(admin(authentication),request));
    }
    private UUID admin(Authentication authentication) {
        var actor=identity.requireActiveActor(authentication);
        if(actor.baseRole()!=BaseRole.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","仅管理员可管理内容。");
        return actor.actorId();
    }
    private <T> ResponseEntity<ApiEnvelope<T>> ok(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(body));
    }
    private <T> ResponseEntity<ApiEnvelope<List<T>>> page(Page<T> page,int number,int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.page(page.getContent(),
                new PageMeta(number,size,page.getTotalElements(),page.getTotalPages())));
    }
    private <T> ResponseEntity<ApiEnvelope<T>> result(IdempotencyExecutor.Result<T> result,String location) {
        var builder=location!=null&&!result.replayed()?ResponseEntity.created(URI.create(location)):ResponseEntity.ok();
        if(result.replayed()) builder.header("Idempotency-Replayed","true");
        return builder.cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(result.value()));
    }
}
