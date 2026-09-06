package wemove.commerce.domain;

import org.springframework.http.HttpStatus;

import wemove.commerce.api.CommerceDtos.*;
import wemove.platform.api.ApiException;

import java.util.*;

public final class CommerceRules {
    private CommerceRules() {}

    public static long subtotal(long price, int quantity) {
        if (price < 1 || price > 99999999 || quantity < 1 || quantity > 99)
            throw invalid("items", "商品价格或数量无效。");
        return Math.multiplyExact(price, quantity);
    }

    public static String text(String field, String value, int min, int max) {
        String v = value == null ? "" : value.strip();
        int n = v.codePointCount(0, v.length());
        if (n < min || n > max) throw invalid(field, "字符数需为 " + min + "—" + max + "。");
        return v;
    }

    public static ShippingAddress address(ShippingAddress a) {
        if (a == null) throw invalid("shippingAddress", "请填写收货地址。");
        String phone = a.phone() == null ? "" : a.phone().replaceAll("[\\s\\-()]", "");
        if (!phone.matches("^\\+?[0-9]{6,20}$"))
            throw invalid("shippingAddress.phone", "请输入6—20位联系电话。");
        // Region is optional for all regions in this baseline; free text supports countries without
        // provinces.
        return new ShippingAddress(
                text("shippingAddress.recipient", a.recipient(), 2, 50),
                phone,
                text("shippingAddress.countryOrRegion", a.countryOrRegion(), 2, 100),
                a.region() == null || a.region().isBlank()
                        ? null
                        : text("shippingAddress.region", a.region(), 2, 100),
                text("shippingAddress.city", a.city(), 2, 100),
                text("shippingAddress.addressLine", a.addressLine(), 5, 200));
    }

    public static void version(long actual, long expected) {
        if (actual != expected) throw conflict("VERSION_CONFLICT", "记录已更新，请刷新后重新确认。");
    }

    public static void state(String actual, String... allowed) {
        if (!List.of(allowed).contains(actual)) throw conflict("STATE_CONFLICT", "当前订单状态不允许此操作。");
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException invalid(String field, String message) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_ERROR",
                "请检查标记字段。",
                List.of(new ApiException.FieldViolation(field, "INVALID_VALUE", message)));
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "记录不存在或不可访问。");
    }
}
