package wemove.content;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpStatus;
import wemove.platform.api.ApiException;

public final class ContentRules {
    private ContentRules() {}
    // Common rich text stays image-free: FAQ and settings do not register body asset references.
    private static final Safelist HTML = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "h2", "h3", "h4",
                    "ul", "ol", "li", "blockquote", "a", "code", "pre", "hr")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https")
            .preserveRelativeLinks(true)
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");
    private static final Safelist ARTICLE_HTML = new Safelist(HTML)
            .addTags("section", "figure", "figcaption", "img", "div")
            .addAttributes("section", "data-layout")
            .addAttributes("img", "src", "alt");
    private static final Set<String> GUIDE_IMAGES = Set.of(
            "/assets/products/guides/balance-stones-guide.png", "/assets/products/guides/rainbow-arch-guide.png",
            "/assets/products/guides/ring-toss-guide.png", "/assets/products/guides/team-board-guide.png",
            "/assets/products/guides/forest-kit-guide.png", "/assets/products/guides/skip-rope-guide.png");
    private static final Pattern MEDIA_IMAGE = Pattern.compile(
            "^/api/v1/media/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/content$");
    public static String html(String value, String field, boolean required) {
        return html(value, field, required, false);
    }
    private static String html(String value, String field, boolean required, boolean article) {
        var dirty = Jsoup.parseBodyFragment(value == null ? "" : value.strip(), "https://wemove.invalid");
        // Do not insert indentation before cleaning; a second save/publish must
        // preserve the already-sanitized body byte for byte.
        dirty.outputSettings().prettyPrint(false);
        dirty.select("a[href]").forEach(link -> {
            try { target(link.attr("href")); }
            catch (ApiException ignored) { link.removeAttr("href"); }
        });
        if (article) {
            // Check the actual attribute, never an URL resolved against the document's base URI.
            dirty.select("img").forEach(image -> {
                String source = image.attr("src");
                if (!GUIDE_IMAGES.contains(source) && !MEDIA_IMAGE.matcher(source).matches()) image.remove();
            });
            dirty.select("section").forEach(section -> {
                if (!Set.of("image-left", "image-right").contains(section.attr("data-layout"))) section.unwrap();
            });
        }
        String cleaned = Jsoup.clean(dirty.body().html(), "https://wemove.invalid", article ? ARTICLE_HTML : HTML,
                new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));
        if ((required || value != null && !value.isEmpty()) && Jsoup.parseBodyFragment(cleaned).text().isBlank())
            throw invalid(field, "清洗后正文为空，请填写有效文本。");
        return cleaned;
    }
    public static String text(String value) { return value == null ? "" : value.strip(); }
    public static String target(String value) {
        String target = bounded(value, "targetUrl", 1, 2048, false);
        if (target.isBlank()) return "";
        if (target.chars().anyMatch(c -> Character.isISOControl(c) || c == '\\'))
            throw invalid("targetUrl", "链接包含无效字符。");
        try {
            URI uri = URI.create(target);
            if (target.startsWith("/") && !target.startsWith("//") && uri.getAuthority() == null) return target;
            if (Set.of("http", "https").contains(Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT))
                    && uri.getHost() != null && uri.getUserInfo() == null) return target;
        } catch (IllegalArgumentException ignored) {}
        throw invalid("targetUrl", "仅允许站内绝对路径或 HTTP/HTTPS 链接。");
    }
    static String bounded(String value, String field, int min, int max, boolean required) {
        String normalized = text(value);
        if (normalized.isEmpty()) {
            if (required || value != null && !value.isEmpty()) throw invalid(field, "请填写有效内容。");
            return "";
        }
        int length = normalized.codePointCount(0, normalized.length());
        if (length < min || length > max) throw invalid(field, "长度须为 " + min + "—" + max + " 个 Unicode 字符。");
        return normalized;
    }
    static String body(String value, String field, int max, boolean required) {
        return body(value, field, max, required, false);
    }
    static String articleBody(String value, boolean required) {
        return body(value, "body", 20000, required, true);
    }
    private static String body(String value, String field, int max, boolean required, boolean article) {
        String cleaned = html(value, field, required, article);
        String visible = Jsoup.parseBodyFragment(cleaned).text();
        if (visible.codePointCount(0, visible.length()) > max) throw invalid(field, "正文最多 " + max + " 个 Unicode 字符。");
        return cleaned;
    }
    /** Explicit media order keeps the cover stable; body images are appended in reading order. */
    static List<UUID> articleMedia(List<UUID> explicitIds, String cleanedBody) {
        var merged = new LinkedHashSet<>(unique(explicitIds, "mediaIds"));
        Jsoup.parseBodyFragment(cleanedBody).select("img[src]").forEach(image -> {
            var match = MEDIA_IMAGE.matcher(image.attr("src"));
            if (match.matches()) merged.add(UUID.fromString(match.group(1)));
        });
        if (merged.size() > 20) throw invalid("mediaIds", "封面与正文图片合计最多关联 20 张图片。");
        return List.copyOf(merged);
    }
    static List<UUID> unique(List<UUID> values, String field) {
        if (values == null) return List.of();
        if (values.stream().anyMatch(Objects::isNull) || new HashSet<>(values).size() != values.size())
            throw invalid(field, "关联项目不能重复或为空。");
        return List.copyOf(values);
    }
    static void version(long actual, Long expected) {
        if (expected == null || expected < 0) throw invalid("expectedVersion", "必须提交当前数据版本。");
        if (actual != expected) throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "内容已被修改，请刷新后重试。");
    }
    static ApiException invalid(String field, String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", detail,
                List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", detail)));
    }
}
