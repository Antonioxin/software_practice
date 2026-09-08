package wemove.content;

import static wemove.content.ContentDtos.*;
import static wemove.content.ContentRules.*;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import wemove.catalog.platform.CatalogPort;
import wemove.config.WemoveProperties;
import wemove.content.assets.ContentAssetsPort;
import wemove.content.platform.ContentSettingsPort;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import wemove.platform.api.ApiException;
import wemove.platform.idempotency.IdempotencyExecutor;

@Service
public class ContentService implements ContentSettingsPort, ApplicationRunner {
    private final ContentEntryRepository entries;
    private final SiteSettingsRepository settings;
    private final HomeSettingsRepository homes;
    private final ContentAssetsPort assets;
    private final CatalogPort catalog;
    private final IdentityPort identity;
    private final AuditPort audit;
    private final UnitOfWork work;
    private final IdempotencyExecutor idempotency;
    private final WemoveProperties properties;

    public ContentService(ContentEntryRepository entries, SiteSettingsRepository settings,
            HomeSettingsRepository homes, ContentAssetsPort assets, CatalogPort catalog,
            IdentityPort identity, AuditPort audit, UnitOfWork work,
            IdempotencyExecutor idempotency, WemoveProperties properties) {
        this.entries=entries; this.settings=settings; this.homes=homes; this.assets=assets;
        this.catalog=catalog; this.identity=identity; this.audit=audit; this.work=work;
        this.idempotency=idempotency; this.properties=properties;
    }
    @Override public void run(ApplicationArguments args) {
        // Production singleton rows are supplied by Flyway; also support empty embedded test schemas.
        work.run(() -> {
            if (!settings.existsById(SiteSettings.ID)) {
                var initial = new SiteSettings();
                initial.termsVersion = properties.registration().termsVersion();
                initial.privacyVersion = properties.registration().privacyVersion();
                settings.saveAndFlush(initial);
            }
            if (!homes.existsById(HomeSettings.ID)) homes.saveAndFlush(new HomeSettings());
            return null;
        });
    }
    @Transactional(readOnly=true)
    public Page<ArticleView> articles(boolean admin, String keyword, String status, String category,
            UUID productId, int page, int pageSize) {
        return query("ARTICLE",admin,keyword,status,category,productId,page,pageSize).map(this::article);
    }
    @Transactional(readOnly=true)
    public ArticleView article(UUID id, boolean admin) { return article(read(id,"ARTICLE",admin)); }
    @Transactional(readOnly=true)
    public Page<FaqView> faqs(boolean admin, String keyword, String status, String category,
            UUID productId, int page, int pageSize) {
        return query("FAQ",admin,keyword,status,category,productId,page,pageSize).map(this::faq);
    }
    @Transactional(readOnly=true)
    public FaqView faq(UUID id, boolean admin) { return faq(read(id,"FAQ",admin)); }
    @Transactional(readOnly=true)
    public Page<BannerView> banners(String keyword, String status, int page, int pageSize) {
        return query("BANNER",true,keyword,status,null,null,page,pageSize).map(this::banner);
    }
    @Transactional(readOnly=true)
    public BannerView banner(UUID id) { return banner(read(id,"BANNER",true)); }

    public IdempotencyExecutor.Result<ArticleView> createArticle(UUID actor, UUID key, ArticleRequest req) {
        return create(actor,key,"ARTICLE",req,ArticleView.class,e->{apply(e,req); return article(e);});
    }
    public IdempotencyExecutor.Result<FaqView> createFaq(UUID actor, UUID key, FaqRequest req) {
        return create(actor,key,"FAQ",req,FaqView.class,e->{apply(e,req); return faq(e);});
    }
    public IdempotencyExecutor.Result<BannerView> createBanner(UUID actor, UUID key, BannerRequest req) {
        return create(actor,key,"BANNER",req,BannerView.class,e->{apply(e,req); return banner(e);});
    }
    private <T> IdempotencyExecutor.Result<T> create(UUID actor, UUID key, String kind, Object req,
            Class<T> type, Function<ContentEntry,T> fill) {
        return idempotency.execute(actor,"content.create"+kind,key,"/admin/"+plural(kind),req,type,201,null,()->{
            requireAdmin(actor);
            var entry = ContentEntry.create(kind); fill.apply(entry);
            entries.saveAndFlush(entry); replaceAssets(entry); log(actor, "CREATE",entry.id,kind);
            return view(entry,type);
        });
    }
    public ArticleView updateArticle(UUID actor, UUID id, ArticleRequest req) {
        return update(actor,id,"ARTICLE",req.expectedVersion(),e->{apply(e,req); return article(e);},ArticleView.class);
    }
    public FaqView updateFaq(UUID actor, UUID id, FaqRequest req) {
        return update(actor,id,"FAQ",req.expectedVersion(),e->{apply(e,req); return faq(e);},FaqView.class);
    }
    public BannerView updateBanner(UUID actor, UUID id, BannerRequest req) {
        return update(actor,id,"BANNER",req.expectedVersion(),e->{apply(e,req); return banner(e);},BannerView.class);
    }
    private <T> T update(UUID actor, UUID id, String kind, Long expected,
            Function<ContentEntry,T> fill, Class<T> type) {
        return work.run(()->{
            requireAdmin(actor); var entry=locked(id,kind); version(entry.version,expected);
            fill.apply(entry); if (entry.published()) publishable(entry);
            entry.updatedAt=Instant.now(); entries.flush(); replaceAssets(entry);
            log(actor,"UPDATE",id,kind); return view(entry,type);
        });
    }
    public <T> IdempotencyExecutor.Result<T> publication(UUID actor, UUID id, UUID key,
            String kind, VersionCommand req, boolean publish, Class<T> type) {
        String operation=publish?"publish":"unpublish";
        return idempotency.execute(actor,"content."+operation+kind,key,
                "/admin/"+plural(kind)+"/"+id+"/"+operation,req,type,200,null,()->{
            requireAdmin(actor); var entry=locked(id,kind); version(entry.version,req.expectedVersion());
            if (publish && entry.published() || !publish && !entry.published())
                throw new ApiException(HttpStatus.CONFLICT,"INVALID_STATE_TRANSITION","当前发布状态不支持该操作。");
            if (publish) { publishable(entry); entry.publishedAt=Instant.now(); }
            entry.status=publish?"PUBLISHED":"OFFLINE"; entry.updatedAt=Instant.now();
            entries.flush(); replaceAssets(entry); log(actor,operation.toUpperCase(Locale.ROOT),id,kind);
            return view(entry,type);
        });
    }
    @Transactional(readOnly=true)
    public SettingsView settings() { return settingsView(settings.findById(SiteSettings.ID).orElseThrow()); }
    public SettingsView updateSettings(UUID actor, SettingsRequest req) {
        return work.run(()->{
            requireAdmin(actor); var s=settings.lock(SiteSettings.ID).orElseThrow(); version(s.version,req.expectedVersion());
            String terms=html(req.termsText(),"termsText",true), privacy=html(req.privacyText(),"privacyText",true);
            String termsVersion=text(req.termsVersion()), privacyVersion=text(req.privacyVersion());
            if (!terms.equals(s.termsText) && termsVersion.equals(s.termsVersion))
                throw invalid("termsVersion","服务条款修改时必须更新条款版本。");
            if (!privacy.equals(s.privacyText) && privacyVersion.equals(s.privacyVersion))
                throw invalid("privacyVersion","隐私说明修改时必须更新说明版本。");
            s.brandName=text(req.brandName()); s.brandDescription=html(req.brandDescription(),"brandDescription",false);
            s.contactEmail=text(req.contactEmail()); s.contactPhone=text(req.contactPhone());
            s.contactAddress=text(req.contactAddress()); s.logoMediaId=req.logoMediaId();
            s.termsText=terms; s.privacyText=privacy; s.termsVersion=termsVersion; s.privacyVersion=privacyVersion;
            assets.replaceReferences("SITE_SETTINGS",s.id,s.logoMediaId==null?List.of():List.of(s.logoMediaId),true);
            settings.flush(); log(actor,"UPDATE",s.id,"SITE_SETTINGS"); return settingsView(s);
        });
    }
    @Override @Transactional(readOnly=true)
    public DocumentVersions currentDocumentVersions() {
        var s=settings.findById(SiteSettings.ID).orElse(null);
        return s==null ? new DocumentVersions(properties.registration().termsVersion(),properties.registration().privacyVersion())
                : new DocumentVersions(s.termsVersion,s.privacyVersion);
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public DocumentVersions lockDocumentVersions() {
        var s=settings.lock(SiteSettings.ID).orElseThrow();
        return new DocumentVersions(s.termsVersion,s.privacyVersion);
    }
    @Transactional(readOnly=true)
    public HomeAdminView homeAdmin() { return homeAdmin(homes.findById(HomeSettings.ID).orElseThrow()); }
    public HomeAdminView updateHome(UUID actor, HomeRequest req) {
        return work.run(()->{
            requireAdmin(actor); var h=homes.lock(HomeSettings.ID).orElseThrow(); version(h.version,req.expectedVersion());
            var products=unique(req.recommendedProductIds(),"recommendedProductIds");
            var articles=unique(req.featuredArticleIds(),"featuredArticleIds");
            // Content locks precede product locks and are obtained in deterministic order.
            for (UUID id:articles.stream().sorted().toList()) {
                if (!locked(id,"ARTICLE").published()) throw invalid("featuredArticleIds","推荐文章必须已发布。");
            }
            validateProducts(products);
            h.recommendedProductIds=ContentEntry.pack(products); h.featuredArticleIds=ContentEntry.pack(articles);
            homes.flush(); log(actor,"UPDATE",h.id,"HOME_SETTINGS"); return homeAdmin(h);
        });
    }
    @Transactional(readOnly=true)
    public HomeView home() {
        var h=homes.findById(HomeSettings.ID).orElseThrow();
        var ids=ContentEntry.ids(h.recommendedProductIds);
        var indexed=new HashMap<UUID,CatalogPort.PublicProductProjection>();
        catalog.getPublicProducts(ids).forEach(p->indexed.put(p.id(),p));
        var products=ids.stream().map(indexed::get).filter(Objects::nonNull).toList();
        var featured=ContentEntry.ids(h.featuredArticleIds).stream()
                .map(id->entries.findByIdAndKind(id,"ARTICLE")).flatMap(Optional::stream)
                .filter(ContentEntry::published).map(this::article).toList();
        return new HomeView(entries.findByKindAndStatusOrderBySortOrderAscCreatedAtDesc("BANNER","PUBLISHED")
                .stream().map(this::banner).toList(),products,featured);
    }
    private Page<ContentEntry> query(String kind, boolean admin, String keyword, String status,
            String category, UUID productId, int page, int pageSize) {
        if(page<1||pageSize<1||pageSize>50) throw invalid("page","分页范围为每页 1—50 条，页码从 1 开始。");
        if(keyword!=null&&keyword.length()>100) throw invalid("keyword","关键词最多 100 字。");
        String effective=admin?text(status).toUpperCase(Locale.ROOT):"PUBLISHED";
        if(!effective.isBlank()&&!Set.of("DRAFT","PUBLISHED","OFFLINE").contains(effective)) throw invalid("status","状态无效。");
        Specification<ContentEntry> spec=(root,query,cb)->{
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("kind"),kind));
            if(!effective.isBlank()) predicates.add(cb.equal(root.get("status"),effective));
            if(!text(category).isBlank()) predicates.add(cb.equal(root.get("category"),text(category)));
            if(!text(keyword).isBlank()) {
                String term="%"+text(keyword).toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")),term,'\\'),cb.like(cb.lower(root.get("summary")),term,'\\')));
            }
            if(productId!=null) predicates.add(cb.like(root.get("productIds"),"%"+productId+"%"));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return entries.findAll(spec,PageRequest.of(page-1,pageSize,Sort.by("sortOrder").and(Sort.by(Sort.Direction.DESC,"createdAt")).and(Sort.by("id"))));
    }
    private void apply(ContentEntry e, ArticleRequest r) {
        e.title=bounded(r.title(),"title",2,100,e.published()); e.summary=text(r.summary()); e.body=articleBody(r.body(),e.published());
        e.category=text(r.category()); e.pageDescription=text(r.pageDescription());
        e.productIds=ContentEntry.pack(unique(r.productIds(),"productIds"));
        e.mediaIds=ContentEntry.pack(articleMedia(r.mediaIds(),e.body)); e.sortOrder=r.sortOrder()==null?0:r.sortOrder();
        validateProducts(ContentEntry.ids(e.productIds));
    }
    private void apply(ContentEntry e, FaqRequest r) {
        e.title=bounded(r.question(),"question",2,200,e.published()); e.body=body(r.answer(),"answer",5000,e.published()); e.category=bounded(r.category(),"category",1,80,e.published());
        e.productIds=ContentEntry.pack(unique(r.productIds(),"productIds")); e.sortOrder=r.sortOrder()==null?0:r.sortOrder();
        validateProducts(ContentEntry.ids(e.productIds));
    }
    private void apply(ContentEntry e, BannerRequest r) {
        e.title=bounded(r.title(),"title",1,100,e.published()); e.imageId=r.imageId(); e.buttonText=text(r.buttonText());
        e.targetUrl=target(r.targetUrl()); e.sortOrder=r.sortOrder()==null?0:r.sortOrder();
    }
    private void publishable(ContentEntry e) {
        if(e.title.isBlank()) throw invalid("title","发布前请填写标题。");
        if(e.kind.equals("BANNER")) {
            if(e.imageId==null) throw invalid("imageId","发布 Banner 前必须选择图片。");
            if(e.targetUrl.isBlank()) throw invalid("targetUrl","按钮须填写跳转链接。");
        } else {
            if (e.kind.equals("ARTICLE")) {
                e.body=articleBody(e.body,true);
                e.mediaIds=ContentEntry.pack(articleMedia(ContentEntry.ids(e.mediaIds),e.body));
            } else e.body=body(e.body,"answer",5000,true);
            if(e.kind.equals("FAQ")&&e.category.isBlank()) throw invalid("category","发布 FAQ 前必须选择分类。");
            validateProducts(ContentEntry.ids(e.productIds));
        }
    }
    private void validateProducts(List<UUID> ids) {
        var snapshots=catalog.lockRetailSnapshot(ids.stream().map(id->new CatalogPort.RequestedItem(id,1)).toList());
        if(snapshots.size()!=ids.size()||snapshots.stream().anyMatch(p->!p.published()))
            throw invalid("productIds","关联或推荐商品必须存在且已发布。");
    }
    private void replaceAssets(ContentEntry e) {
        if (e.kind.equals("FAQ")) return;
        assets.replaceReferences(e.kind,e.id,e.kind.equals("BANNER")
                ? e.imageId==null?List.of():List.of(e.imageId):ContentEntry.ids(e.mediaIds),e.published());
    }
    private ContentEntry read(UUID id,String kind,boolean admin) {
        return entries.findByIdAndKind(id,kind).filter(e->admin||e.published()).orElseThrow(this::missing);
    }
    private ContentEntry locked(UUID id,String kind) { return entries.lock(id,kind).orElseThrow(this::missing); }
    private ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","内容不存在或不可访问。"); }
    private void requireAdmin(UUID actor) {
        if(identity.lockActiveActor(actor).baseRole()!=BaseRole.ADMIN)
            throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","仅管理员可管理内容。");
    }
    private void log(UUID actor,String action,UUID id,String kind) {
        audit.append(new AuditPort.AuditEvent(actor,"CONTENT_"+action,kind,id,"SUCCESS",null,Instant.now()));
    }
    private ArticleView article(ContentEntry e) {
        return new ArticleView(e.id,e.title,e.summary,e.body,e.category,e.pageDescription.isBlank()?e.summary:e.pageDescription,
                ContentEntry.ids(e.productIds),ContentEntry.ids(e.mediaIds),e.status,e.sortOrder,e.version,e.createdAt,e.updatedAt,e.publishedAt);
    }
    private FaqView faq(ContentEntry e) {
        return new FaqView(e.id,e.title,e.body,e.category,ContentEntry.ids(e.productIds),e.status,e.sortOrder,e.version,e.createdAt,e.updatedAt,e.publishedAt);
    }
    private BannerView banner(ContentEntry e) {
        return new BannerView(e.id,e.title,e.imageId,e.buttonText,e.targetUrl,e.sortOrder,e.status,e.version,e.createdAt,e.updatedAt,e.publishedAt);
    }
    private <T> T view(ContentEntry e,Class<T> type) {
        return type.cast(switch(e.kind) {case "ARTICLE"->article(e); case "FAQ"->faq(e); case "BANNER"->banner(e); default->throw new IllegalArgumentException();});
    }
    private SettingsView settingsView(SiteSettings s) {
        return new SettingsView(s.brandName,s.brandDescription,s.contactEmail,s.contactPhone,s.contactAddress,s.logoMediaId,
                s.termsText,s.termsVersion,s.privacyText,s.privacyVersion,s.version);
    }
    private HomeAdminView homeAdmin(HomeSettings h) {
        return new HomeAdminView(ContentEntry.ids(h.recommendedProductIds),ContentEntry.ids(h.featuredArticleIds),h.version);
    }
    private String plural(String kind) { return switch(kind){case "ARTICLE"->"articles";case "FAQ"->"faqs";case "BANNER"->"banners";default->throw new IllegalArgumentException();}; }
}
