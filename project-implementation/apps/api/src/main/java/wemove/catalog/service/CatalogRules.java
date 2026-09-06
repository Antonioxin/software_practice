package wemove.catalog.service;

import wemove.platform.api.ApiException;
import wemove.catalog.api.CatalogDtos;
import java.util.*;
import org.springframework.http.HttpStatus;

public final class CatalogRules {
    public static final long MAX_UNIT_PRICE_FEN = 99_999_999L;
    private CatalogRules() {}

    public static void validateDraft(CatalogDtos.CreateProductRequest input) {
        validateFields(input.sku(), input.name(), input.summary(), input.description(), input.ageMin(),
            input.ageMax(), input.material(), input.dimensions(), input.packageContents(), input.instructions(),
            input.safetyNotes(), input.mainImageId(), input.imageIds(), input.retailUnitPriceFen(),
            Boolean.TRUE.equals(input.dealerEnabled()), input.dealerReferenceUnitPriceFen(),
            input.minInquiryQuantity(), input.leadTimeText());
    }

    public static void validateDraft(CatalogDtos.UpdateProductRequest input) {
        validateFields(input.sku(), input.name(), input.summary(), input.description(), input.ageMin(),
            input.ageMax(), input.material(), input.dimensions(), input.packageContents(), input.instructions(),
            input.safetyNotes(), input.mainImageId(), input.imageIds(), input.retailUnitPriceFen(),
            Boolean.TRUE.equals(input.dealerEnabled()), input.dealerReferenceUnitPriceFen(),
            input.minInquiryQuantity(), input.leadTimeText());
    }

    private static void validateFields(String sku, String name, String summary, String description,
                                       Integer ageMin, Integer ageMax, String material, String dimensions,
                                       String packageContents, String instructions, String safetyNotes,
                                       String mainImageId, List<String> imageIds, Long retailPrice,
                                       boolean dealerEnabled, Long dealerPrice, Integer minInquiry,
                                       String leadTime) {
        if (sku != null && !sku.isBlank() && !sku.matches("[A-Z0-9-]{3,40}")) {
            fail("sku", "INVALID_SKU", "SKU 需为 3—40 个大写字母、数字或连字符。");
        }
        lengthIfPresent("name", name, 2, 100, "商品名称需为 2—100 个字符。");
        lengthIfPresent("summary", summary, 1, 200, "商品简述最多 200 个字符。");
        lengthIfPresent("description", description, 0, 10_000, "详细说明最多 10000 个字符。");
        lengthIfPresent("material", material, 1, 2_000, "材质说明最多 2000 个字符。");
        lengthIfPresent("dimensions", dimensions, 1, 2_000, "规格与单位最多 2000 个字符。");
        lengthIfPresent("packageContents", packageContents, 1, 2_000, "包装内容最多 2000 个字符。");
        lengthIfPresent("instructions", instructions, 1, 2_000, "玩法说明最多 2000 个字符。");
        lengthIfPresent("safetyNotes", safetyNotes, 1, 2_000, "安全提示最多 2000 个字符。");
        lengthIfPresent("leadTimeText", leadTime, 1, 500, "参考交期最多 500 个字符。");
        if (ageMin != null && (ageMin < 0 || ageMin > 18)) fail("ageMin", "INVALID_AGE", "年龄下限需为 0—18 的整数。");
        if (ageMax != null && (ageMax < 0 || ageMax > 18 || ageMin == null || ageMax < ageMin)) {
            fail("ageMax", "INVALID_AGE_RANGE", "年龄上限不能小于下限，且不能超过 18。");
        }
        price("retailUnitPriceFen", retailPrice);
        price("dealerReferenceUnitPriceFen", dealerPrice);
        if (minInquiry != null && (minInquiry < 1 || minInquiry > 9_999)) {
            fail("minInquiryQuantity", "INVALID_QUANTITY", "最小询价数量需为 1—9999 的整数。");
        }
        if (mainImageId != null && !mainImageId.isBlank() && mainImageId.codePointCount(0, mainImageId.length()) > 64) {
            fail("mainImageId", "INVALID_IMAGE_ID", "主图标识最多 64 个字符。");
        }
        if (imageIds != null) {
            if (imageIds.size() > 12 || new HashSet<>(imageIds).size() != imageIds.size() || imageIds.stream().anyMatch(id -> id == null || id.isBlank() || id.length() > 64)) {
                fail("imageIds", "INVALID_IMAGE_IDS", "附图最多 12 张，标识不能为空或重复。");
            }
        }
        if (!dealerEnabled && (dealerPrice != null || minInquiry != null || present(leadTime))) {
            fail("dealerEnabled", "DEALER_FIELDS_DISABLED", "未启用经销业务时请清空经销价格、最小量和交期。");
        }
    }

    public static void requirePublishable(String sku, String name, Object category, String summary,
                                          Integer ageMin, Object playType, Object scene, String material,
                                          String dimensions, String packageContents, String instructions,
                                          String safetyNotes, String mainImageId, Long retailPrice,
                                          boolean dealerEnabled, Long dealerPrice, Integer minInquiry,
                                          String leadTime) {
        Map<String, Object> required = new LinkedHashMap<>();
        required.put("sku", sku); required.put("name", name); required.put("categoryId", category);
        required.put("summary", summary); required.put("ageMin", ageMin); required.put("playType", playType);
        required.put("scene", scene); required.put("material", material); required.put("dimensions", dimensions);
        required.put("packageContents", packageContents); required.put("instructions", instructions);
        required.put("safetyNotes", safetyNotes); required.put("mainImageId", mainImageId);
        required.put("retailUnitPriceFen", retailPrice);
        for (Map.Entry<String, Object> entry : required.entrySet()) {
            if (entry.getValue() == null || entry.getValue() instanceof String value && value.isBlank()) {
                fail(entry.getKey(), "PUBLISH_FIELD_REQUIRED", "发布前请补充该字段。");
            }
        }
        if (dealerEnabled) {
            if (dealerPrice == null) fail("dealerReferenceUnitPriceFen", "PUBLISH_FIELD_REQUIRED", "启用经销业务时需填写经销参考价。");
            if (minInquiry == null) fail("minInquiryQuantity", "PUBLISH_FIELD_REQUIRED", "启用经销业务时需填写最小询价数量。");
            if (!present(leadTime)) fail("leadTimeText", "PUBLISH_FIELD_REQUIRED", "启用经销业务时需填写参考交期。");
        }
    }

    public static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void price(String field, Long value) {
        if (value != null && (value <= 0 || value > MAX_UNIT_PRICE_FEN)) {
            fail(field, "INVALID_PRICE", "价格必须大于 0，且不超过 999999.99 元。");
        }
    }

    private static void lengthIfPresent(String field, String value, int min, int max, String message) {
        if (value == null) return;
        String trimmed = value.strip();
        if (trimmed.isEmpty() && min > 0) return;
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < min || length > max) fail(field, "INVALID_LENGTH", message);
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }

    private static void fail(String field, String code, String message) {
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。",
            List.of(new ApiException.FieldViolation(field, code, message)));
    }
}
