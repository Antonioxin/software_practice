package wemove.content.assets;
import org.apache.pdfbox.Loader;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import wemove.platform.api.ApiException;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
public final class AssetRules {
    private AssetRules() {}
    public record Validated(byte[] bytes, String filename, String mimeType, int width, int height) {}
    public static Validated validate(MultipartFile file, boolean image) {
        if(file==null||file.isEmpty()) throw invalid("file", "请选择非空文件。");
        if(file.getSize() > (image ? 5L : 10L)*1024*1024)
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"FILE_TOO_LARGE",image ? "图片不可超过 5 MiB。" : "PDF 不可超过 10 MiB。");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        name=name.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        name=name.replaceAll("[\\p{Cntrl}<>:\"|?*]", "_");
        if(name.isBlank()) name="document";
        if(name.length()>180) name=name.substring(name.length()-180);
        String ext=name.contains(".") ? name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT) : "";
        String declared=Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        String expected=image ? switch(ext) { case "jpg","jpeg" -> "image/jpeg"; case "png" -> "image/png"; case "webp" -> "image/webp"; default -> ""; } : (ext.equals("pdf") ? "application/pdf" : "");
        if(expected.isEmpty()||!expected.equals(declared)) throw invalid("file","扩展名与文件类型不匹配，仅接受 JPEG、PNG、WebP 图片或 PDF 资料。");
        try {
            byte[] data=file.getBytes();
            if(image) {
                try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                    var readers=ImageIO.getImageReaders(input);
                    if(!readers.hasNext()) throw new IOException();
                    var reader=readers.next();
                    try {
                        reader.setInput(input,true,true);
                        String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                        if(!(expected.equals("image/jpeg") && (format.equals("jpeg")||format.equals("jpg")) || expected.equals("image/png")&&format.equals("png") || expected.equals("image/webp")&&format.equals("webp"))) throw new IOException();
                        int width=reader.getWidth(0), height=reader.getHeight(0);
                        if(width<1||height<1||(long)width*height>40_000_000L) throw invalid("file","图片尺寸无效或超过 4000 万像素。");
                        if(reader.read(0)==null) throw new IOException();
                        return new Validated(data,name,expected,width,height);
                    } finally { reader.dispose(); }
                }
            }
            if(data.length<5||!new String(data,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) throw new IOException();
            try(var pdf=Loader.loadPDF(data)) { if(pdf.isEncrypted()||pdf.getNumberOfPages()<1) throw new IOException(); }
            return new Validated(data,name,expected,0,0);
        } catch(IOException | IllegalArgumentException ex) { throw invalid("file","文件内容无法识别或已损坏，请上传有效文件。"); }
    }
    static String text(String value,String field,int min,int max) {
        String result=value==null?"":value.trim();
        int length=result.codePointCount(0,result.length());
        if(length<min||length>max) throw invalid(field,"请输入 "+min+"—"+max+" 个字符。");
        return result;
    }
    static void version(long actual,Long expected) {
        if(expected==null||expected<1) throw invalid("expectedVersion","请提供当前版本。");
        if(actual!=expected) throw new ApiException(HttpStatus.CONFLICT,"VERSION_CONFLICT","记录已更新，请刷新后重新编辑。");
    }
    public static ApiException invalid(String field,String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"VALIDATION_ERROR",message,List.of(new ApiException.FieldViolation(field,"INVALID",message)));
    }
    static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","资料不存在或当前不可访问。"); }
}
