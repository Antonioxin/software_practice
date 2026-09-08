package wemove.content.assets;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wemove.platform.api.ApiEnvelope;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static wemove.content.assets.AssetDtos.*;

@RestController
@RequestMapping("/api/v1")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service) { this.service=service; }
    @GetMapping("/files")
    public ResponseEntity<ApiEnvelope<List<FileView>>> files(Authentication auth,
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String type,
            @RequestParam(required=false) UUID productId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int pageSize) {
        return page(service.list(auth,false,keyword,type,productId,null,null,page,pageSize));
    }
    @GetMapping("/admin/files")
    public ResponseEntity<ApiEnvelope<List<FileView>>> adminFiles(Authentication auth,
            @RequestParam(required=false) String keyword,@RequestParam(required=false) String type,@RequestParam(required=false) UUID productId,
            @RequestParam(required=false) String status,@RequestParam(required=false) String visibility,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int pageSize) {
        return page(service.list(auth,true,keyword,type,productId,status,visibility,page,pageSize));
    }
    @GetMapping("/files/{id}") public ResponseEntity<ApiEnvelope<FileView>> detail(Authentication auth,@PathVariable UUID id) { return ok(service.detail(auth,id,false)); }
    @GetMapping("/admin/files/{id}") public ResponseEntity<ApiEnvelope<FileView>> adminDetail(Authentication auth,@PathVariable UUID id) { return ok(service.detail(auth,id,true)); }
    @GetMapping("/files/{id}/versions/{downloadId}/content")
    public ResponseEntity<byte[]> download(Authentication auth,@PathVariable UUID id,@PathVariable UUID downloadId) { return binary(service.download(auth,id,downloadId),true); }
    @PostMapping(value="/admin/files",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<FileView>> create(Authentication auth,@RequestPart("file") MultipartFile file,@RequestPart("metadata") FileMetadata metadata) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(service.create(service.admin(auth),file,metadata)));
    }
    @PatchMapping("/admin/files/{id}") public ResponseEntity<ApiEnvelope<FileView>> update(Authentication auth,@PathVariable UUID id,@RequestBody FileUpdate request) { return ok(service.update(service.admin(auth),id,request)); }
    @PostMapping(value="/admin/files/{id}/replace",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<FileView>> replace(Authentication auth,@PathVariable UUID id,@RequestPart("file") MultipartFile file,
            @RequestParam Long expectedVersion,@RequestParam String versionNote) { return ok(service.replace(service.admin(auth),id,file,expectedVersion,versionNote)); }
    @PostMapping("/admin/files/{id}/publish") public ResponseEntity<ApiEnvelope<FileView>> publish(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestBody VersionCommand request) { return publication(auth,id,key,request,true); }
    @PostMapping("/admin/files/{id}/unpublish") public ResponseEntity<ApiEnvelope<FileView>> unpublish(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key") UUID key,@RequestBody VersionCommand request) { return publication(auth,id,key,request,false); }
    private ResponseEntity<ApiEnvelope<FileView>> publication(Authentication auth,UUID id,UUID key,VersionCommand request,boolean publish) {
        var result=service.publication(service.admin(auth),id,key,request,publish);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Idempotency-Replayed",Boolean.toString(result.replayed())).body(ApiEnvelope.of(result.value()));
    }
    @GetMapping("/admin/media") public ResponseEntity<ApiEnvelope<List<MediaView>>> media(Authentication auth,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="24") int pageSize) { service.admin(auth); return page(service.media(page,pageSize)); }
    @PostMapping(value="/admin/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<MediaView>> upload(Authentication auth,@RequestPart("file") MultipartFile file,@RequestParam(defaultValue="") String altText) { return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(service.upload(service.admin(auth),file,altText))); }
    @DeleteMapping("/admin/media/{id}") public ResponseEntity<Void> delete(Authentication auth,@PathVariable UUID id,@RequestParam Long expectedVersion) { service.delete(service.admin(auth),id,expectedVersion); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build(); }
    @GetMapping("/media/{id}/content") public ResponseEntity<byte[]> image(Authentication auth,@PathVariable UUID id) { return binary(service.image(auth,id),false); }
    private ResponseEntity<byte[]> binary(Download data,boolean attachment) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(data.mimeType())).contentLength(data.bytes().length)
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .header("Content-Security-Policy","default-src 'none'; sandbox")
                .header(HttpHeaders.CONTENT_DISPOSITION,(attachment?ContentDisposition.attachment():ContentDisposition.inline()).filename(data.filename(),StandardCharsets.UTF_8).build().toString()).body(data.bytes());
    }
    private <T> ResponseEntity<ApiEnvelope<T>> ok(T result) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.of(result)); }
    private <T> ResponseEntity<ApiEnvelope<List<T>>> page(Page<T> result) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiEnvelope.page(result.items(),result.meta())); }
}
