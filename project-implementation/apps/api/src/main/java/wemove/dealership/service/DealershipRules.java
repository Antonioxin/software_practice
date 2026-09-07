package wemove.dealership.service;

import org.springframework.http.HttpStatus;
import wemove.platform.api.ApiException;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

public final class DealershipRules {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{6,20}$");
    private static final Set<String> TYPES = Set.of("RETAIL", "WHOLESALE", "IMPORT", "EDUCATION_ACTIVITY", "OTHER");

    private DealershipRules() {}

    public static String text(String field, String value, int min, int max) {
        String normalized = value == null ? "" : value.strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < min || length > max)
            throw invalid(field, "请填写 " + min + "—" + max + " 个字符。");
        return normalized;
    }

    public static String optional(String field, String value, int max) {
        if (value == null || value.isBlank()) return null;
        return text(field, value, 1, max);
    }

    public static String businessType(String value) {
        String normalized = text("businessType", value, 1, 32).toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) throw invalid("businessType", "请选择有效的业务类型。");
        return normalized;
    }

    public static String phone(String value) {
        String normalized = text("phone", value, 1, 40).replaceAll("[\\s()\\-]", "");
        if (!PHONE.matcher(normalized).matches()) throw invalid("phone", "请输入有效联系电话，可包含国家区号。");
        return normalized;
    }

    public static String email(String value) {
        String normalized = text("cooperationEmail", value, 3, 254).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) throw invalid("cooperationEmail", "请输入有效邮箱。");
        return normalized;
    }

    public static String website(String value) {
        String normalized = optional("website", value, 2048);
        if (normalized == null) return null;
        try {
            URI uri = URI.create(normalized);
            if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null)
                throw new IllegalArgumentException();
            return normalized;
        } catch (RuntimeException ex) {
            throw invalid("website", "请输入有效的 HTTP 或 HTTPS 网页地址。");
        }
    }

    public static void version(long expected, long actual) {
        if (expected != actual)
            throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "记录已被更新，请刷新后重试。");
    }

    public static ApiException invalid(String field, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。",
                List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", message)));
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在或不可访问。");
    }

    public static ApiException state(String message) {
        return new ApiException(HttpStatus.CONFLICT, "STATE_CONFLICT", message);
    }
}
