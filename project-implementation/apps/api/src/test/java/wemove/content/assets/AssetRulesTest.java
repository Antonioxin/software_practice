package wemove.content.assets;

import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import wemove.platform.api.ApiException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;

class AssetRulesTest {
    static byte[] pdf() throws Exception { try(var doc=new PDDocument();var out=new ByteArrayOutputStream()) { doc.addPage(new PDPage()); doc.save(out); return out.toByteArray(); } }
    static byte[] png() throws Exception { var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(3,2,BufferedImage.TYPE_INT_RGB),"png",out); return out.toByteArray(); }
    @Test void titleLengthCountsUnicodeCodePoints() {
        String title="😀".repeat(100);
        assertThat(AssetRules.text(title,"title",2,100)).isEqualTo(title);
        assertThatThrownBy(()->AssetRules.text("😀".repeat(101),"title",2,100))
                .isInstanceOfSatisfying(ApiException.class,error->assertThat(error.getCode()).isEqualTo("VALIDATION_ERROR"));
    }
    @Test void acceptsParsedPdfAndStripsUnsafeFilename() throws Exception {
        var result=AssetRules.validate(new MockMultipartFile("file","../../guide\r\n.pdf","application/pdf",pdf()),false);
        assertThat(result.filename()).isEqualTo("guide__.pdf");
        assertThat(result.mimeType()).isEqualTo("application/pdf");
    }
    @Test void rejectsForgedMimeAndCorruptMagicOnlyPdf() throws Exception {
        assertThatThrownBy(()->AssetRules.validate(new MockMultipartFile("file","guide.pdf","application/pdf","%PDF-1.7 fake".getBytes()),false)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->AssetRules.validate(new MockMultipartFile("file","guide.png","image/png",pdf()),true)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->AssetRules.validate(new MockMultipartFile("file","guide.jpg","image/jpeg",png()),true)).isInstanceOf(ApiException.class);
    }
    @Test void acceptsWebPWithInstalledDecoder() throws Exception {
        byte[] webp=java.nio.file.Files.readAllBytes(java.nio.file.Path.of("../web/public/assets/sketch/plus.webp"));
        var result=AssetRules.validate(new MockMultipartFile("file","plus.webp","image/webp",webp),true);
        assertThat(result.mimeType()).isEqualTo("image/webp");
        assertThat(result.width()).isPositive();
    }
    @Test void acceptsDecodedImageAndRejectsOversize() throws Exception {
        var result=AssetRules.validate(new MockMultipartFile("file","guide.PNG","image/png",png()),true);
        assertThat(result.width()).isEqualTo(3); assertThat(result.height()).isEqualTo(2);
        assertThatThrownBy(()->AssetRules.validate(new MockMultipartFile("file","guide.png","image/png",new byte[5*1024*1024+1]),true)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getCode()).isEqualTo("FILE_TOO_LARGE"));
    }
}
