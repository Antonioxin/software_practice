package wemove.content.assets;

import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wemove.catalog.platform.CatalogPort;
import wemove.identity.domain.BaseRole;
import wemove.platform.*;
import wemove.platform.api.ApiException;
import wemove.platform.idempotency.IdempotencyExecutor;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import static wemove.content.assets.AssetDtos.*;

@Service
public class AssetService implements ContentAssetsPort {
    @PersistenceContext private EntityManager em;
    private final IdentityPort identity;
    private final DealerIdentityPort dealers;
    private final CatalogPort catalog;
    private final AuditPort audit;
    private final IdempotencyExecutor executor;
    public AssetService(IdentityPort identity, DealerIdentityPort dealers, CatalogPort catalog, AuditPort audit, IdempotencyExecutor executor) {
        this.identity=identity; this.dealers=dealers; this.catalog=catalog; this.audit=audit; this.executor=executor;
    }
    public UUID admin(Authentication authentication) {
        var actor=identity.requireActiveActor(authentication);
        if(actor.baseRole()!=BaseRole.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","您无权访问管理后台。");
        return actor.actorId();
    }
    private ActorContext viewer(Authentication authentication) {
        return authentication==null || authentication instanceof AnonymousAuthenticationToken ? null : identity.requireActiveActor(authentication);
    }
    @Transactional(readOnly=true)
    public Page<FileView> list(Authentication auth, boolean administration,String keyword,String type, UUID productId,String status,String visibility,int page,int pageSize) {
        pageCheck(page,pageSize);
        ActorContext actor=viewer(auth);
        if(administration) admin(auth);
        Set<String> allowed=allowed(actor);
        List<FileView> result=em.createQuery("select f from DownloadFile f order by f.updatedAt desc, f.id",DownloadFile.class).getResultList().stream()
                .filter(f->administration||("PUBLISHED".equals(f.status)&&allowed.contains(f.visibility)))
                .filter(f->keyword==null||f.title.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT)))
                .filter(f->type==null||type.isBlank()||f.type.equals(type))
                .filter(f->productId==null||products(f).contains(productId))
                .filter(f->status==null||status.isBlank()||f.status.equals(status))
                .filter(f->visibility==null||visibility.isBlank()||f.visibility.equals(visibility))
                .map(this::view).toList();
        return page(result,page,pageSize);
    }
    @Transactional(readOnly=true)
    public FileView detail(Authentication auth, UUID id,boolean administration) {
        if(administration) admin(auth);
        DownloadFile file=file(id,false);
        if(!administration) readable(file,viewer(auth));
        return view(file);
    }
    @Transactional(readOnly=true)
    public Download download(Authentication auth,UUID id,UUID downloadId) {
        DownloadFile file=file(id,false);
        if(!file.downloadId.equals(downloadId)) throw AssetRules.missing();
        ActorContext actor=viewer(auth);
        readable(file,actor);
        ContentBinary binary=em.find(ContentBinary.class,file.downloadId);
        if(binary==null) throw AssetRules.missing();
        return new Download(binary.bytes,file.filename,"application/pdf");
    }
    private void readable(DownloadFile file,ActorContext actor) {
        if(!"PUBLISHED".equals(file.status)||!allowed(actor).contains(file.visibility)) throw AssetRules.missing();
    }
    private Set<String> allowed(ActorContext actor) {
        if(actor==null) return Set.of("PUBLIC");
        if(actor.baseRole()==BaseRole.ADMIN) return Set.of("PUBLIC","DEALER","INTERNAL");
        return "DEALER".equals(dealers.derivedIdentity(actor.actorId())) ? Set.of("PUBLIC","DEALER") : Set.of("PUBLIC");
    }
    @Transactional
    public FileView create(UUID actor,MultipartFile upload,FileMetadata metadata) {
        lockAdmin(actor);
        var data=AssetRules.validate(upload,false);
        DownloadFile file=new DownloadFile(); file.id=UUID.randomUUID(); file.downloadId=UUID.randomUUID();
        metadata(file,metadata); file.filename=data.filename(); file.sizeBytes=data.bytes().length;
        file.createdAt=Instant.now(); file.updatedAt=file.createdAt;
        em.persist(new ContentBinary(file.downloadId,data.bytes())); em.persist(file);
        log(actor,"FILE_CREATED","FILE",file.id,"visibility="+file.visibility);
        return view(file);
    }
    @Transactional
    public FileView update(UUID actor,UUID id,FileUpdate request) {
        lockAdmin(actor); DownloadFile file=file(id,true); AssetRules.version(file.version,request.expectedVersion());
        String previous=file.visibility; metadata(file,request.metadata()); file.version++; file.updatedAt=Instant.now();
        log(actor,previous.equals(file.visibility)?"FILE_UPDATED":"FILE_VISIBILITY_CHANGED","FILE",id,"visibility: "+previous+" → "+file.visibility);
        return view(file);
    }
    @Transactional
    public FileView replace(UUID actor,UUID id,MultipartFile upload,Long expectedVersion,String versionNote) {
        lockAdmin(actor); DownloadFile file=file(id,true); AssetRules.version(file.version,expectedVersion);
        var data=AssetRules.validate(upload,false);
        String note=AssetRules.text(versionNote,"versionNote",1,500);
        UUID old=file.downloadId; file.downloadId=UUID.randomUUID(); file.filename=data.filename(); file.sizeBytes=data.bytes().length;
        file.versionNote=note; file.version++; file.updatedAt=Instant.now();
        em.persist(new ContentBinary(file.downloadId,data.bytes()));
        ContentBinary previous=em.find(ContentBinary.class,old); if(previous!=null) em.remove(previous);
        log(actor,"FILE_REPLACED","FILE",id,"当前下载标识已更新；旧标识失效。");
        return view(file);
    }
    public IdempotencyExecutor.Result<FileView> publication(UUID actor,UUID id,UUID key,VersionCommand request,boolean publish) {
        return executor.execute(actor,"content.file."+(publish?"publish":"unpublish"),key,"/api/v1/admin/files/"+id+(publish?"/publish":"/unpublish"),request,FileView.class,200,null,()->{
            lockAdmin(actor); DownloadFile file=file(id,true); AssetRules.version(file.version,request.expectedVersion());
            if(publish=="PUBLISHED".equals(file.status)) throw new ApiException(HttpStatus.CONFLICT,"STATE_CONFLICT","资料已处于目标状态。");
            file.status=publish?"PUBLISHED":"OFFLINE"; file.version++; file.updatedAt=Instant.now();
            log(actor,publish?"FILE_PUBLISHED":"FILE_UNPUBLISHED","FILE",id,"status="+file.status);
            return view(file);
        });
    }
    @Transactional(readOnly=true)
    public Page<MediaView> media(int page,int pageSize) {
        pageCheck(page,pageSize);
        var result=em.createQuery("select a from MediaAsset a order by a.createdAt desc, a.id",MediaAsset.class).getResultList().stream().map(this::view).toList();
        return page(result,page,pageSize);
    }
    @Transactional
    public MediaView upload(UUID actor,MultipartFile upload,String altText) {
        lockAdmin(actor); var data=AssetRules.validate(upload,true);
        MediaAsset asset=new MediaAsset(); asset.id=UUID.randomUUID(); asset.altText=AssetRules.text(altText,"altText",0,200);
        asset.filename=data.filename(); asset.mimeType=data.mimeType(); asset.width=data.width(); asset.height=data.height();
        asset.sizeBytes=data.bytes().length; asset.createdAt=Instant.now();
        em.persist(new ContentBinary(asset.id,data.bytes())); em.persist(asset);
        log(actor,"MEDIA_UPLOADED","MEDIA",asset.id,"mimeType="+asset.mimeType);
        return view(asset);
    }
    @Transactional(readOnly=true)
    public Download image(Authentication auth,UUID id) {
        MediaAsset asset=em.find(MediaAsset.class,id); if(asset==null) throw AssetRules.missing();
        ActorContext actor=viewer(auth);
        if((actor==null||actor.baseRole()!=BaseRole.ADMIN)&&referenceCount(id,true)==0) throw AssetRules.missing();
        ContentBinary bytes=em.find(ContentBinary.class,id); if(bytes==null) throw AssetRules.missing();
        return new Download(bytes.bytes,asset.filename,asset.mimeType);
    }
    @Transactional
    public void delete(UUID actor,UUID id,Long expectedVersion) {
        lockAdmin(actor); MediaAsset asset=em.find(MediaAsset.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(asset==null) throw AssetRules.missing(); AssetRules.version(asset.version,expectedVersion);
        if(referenceCount(id,false)>0) throw new ApiException(HttpStatus.CONFLICT,"ASSET_IN_USE","图片仍被商品或内容引用，请先更换或移除引用。");
        em.remove(asset); ContentBinary binary=em.find(ContentBinary.class,id); if(binary!=null) em.remove(binary);
        log(actor,"MEDIA_DELETED","MEDIA",id,"已移除未被引用的图片。");
    }
    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void replaceReferences(String sourceType,UUID sourceId,Collection<UUID> ids,boolean active) {
        if(!Set.of("ARTICLE","BANNER","SITE_SETTINGS","PRODUCT").contains(sourceType)||sourceId==null) throw new IllegalArgumentException("Unsupported asset source");
        if(ids==null||ids.stream().anyMatch(Objects::isNull)||ids.size()>50) throw AssetRules.invalid("mediaIds","图片引用无效或过多。");
        var old=em.createQuery("select r from AssetReference r where r.sourceType=:type and r.sourceId=:id",AssetReference.class).setParameter("type",sourceType).setParameter("id",sourceId).getResultList();
        Set<UUID> all=new TreeSet<>(Comparator.comparing(UUID::toString)); all.addAll(ids); old.forEach(r->all.add(r.assetId));
        for(UUID id:all) if(em.find(MediaAsset.class,id,LockModeType.PESSIMISTIC_WRITE)==null) throw AssetRules.invalid("mediaIds","引用图片不存在，请重新选择。");
        old.forEach(em::remove); em.flush();
        for(UUID id:new LinkedHashSet<>(ids)) { AssetReference r=new AssetReference(); r.id=UUID.randomUUID(); r.assetId=id; r.sourceType=sourceType; r.sourceId=sourceId; r.active=active; em.persist(r); }
    }
    private void metadata(DownloadFile file,FileMetadata value) {
        if(value==null) throw AssetRules.invalid("metadata","请填写资料信息。");
        file.title=AssetRules.text(value.title(),"title",2,100); file.type=AssetRules.text(value.type(),"type",1,50);
        file.versionNote=AssetRules.text(value.versionNote(),"versionNote",1,500);
        if(value.visibility()==null||!Set.of("PUBLIC","DEALER","INTERNAL").contains(value.visibility())) throw AssetRules.invalid("visibility","请选择正确可见范围。");
        file.visibility=value.visibility();
        List<UUID> ids=value.productIds()==null?List.of():value.productIds();
        if(ids.size()>20||ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size()) throw AssetRules.invalid("productIds","请选择不重复的有效商品（最多 20 个）。");
        if(!ids.isEmpty()&&catalog.getRetailSnapshot(ids.stream().map(id->new CatalogPort.RequestedItem(id,1)).toList()).size()!=ids.size()) throw AssetRules.invalid("productIds","关联商品不存在。");
        file.productIds=ids.stream().map(UUID::toString).collect(Collectors.joining(","));
    }
    private void lockAdmin(UUID id) {
        if(identity.lockActiveActor(id).baseRole()!=BaseRole.ADMIN) throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","您无权访问管理后台。");
    }
    private DownloadFile file(UUID id,boolean lock) {
        DownloadFile f=lock?em.find(DownloadFile.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(DownloadFile.class,id);
        if(f==null) throw AssetRules.missing(); return f;
    }
    private List<UUID> products(DownloadFile file) { return file.productIds.isBlank()?List.of():Arrays.stream(file.productIds.split(",")).map(UUID::fromString).toList(); }
    private FileView view(DownloadFile f) { return new FileView(f.id,f.title,f.type,f.versionNote,products(f),f.visibility,f.status,f.version,f.downloadId,"/api/v1/files/"+f.id+"/versions/"+f.downloadId+"/content",f.filename,"application/pdf",f.sizeBytes,f.updatedAt,f.createdAt); }
    private MediaView view(MediaAsset a) { return new MediaView(a.id,"/api/v1/media/"+a.id+"/content",a.altText,a.mimeType,a.width,a.height,a.filename,a.sizeBytes,a.version,referenceCount(a.id,false),referenceCount(a.id,true),a.createdAt); }
    private long referenceCount(UUID id,boolean active) { return em.createQuery("select count(r) from AssetReference r where r.assetId=:id"+(active?" and r.active=true":""),Long.class).setParameter("id",id).getSingleResult(); }
    private void log(UUID actor,String action,String type,UUID id,String summary) { audit.append(new AuditPort.AuditEvent(actor,action,type,id,"SUCCESS",null,Instant.now(),null,summary)); }
    private void pageCheck(int page,int size) { if(page<1||size<1||size>50) throw AssetRules.invalid("page","页码应大于 0，每页 1—50 条。"); }
    private <T> Page<T> page(List<T> items,int page,int size) { int start=(int)Math.min((long)(page-1)*size,items.size()); return new Page<>(items.subList(start,Math.min(start+size,items.size())),new PageMeta(page,size,items.size(),(items.size()+size-1)/size)); }
}
