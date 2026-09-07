package wemove.support.domain;

import org.springframework.http.HttpStatus;
import wemove.platform.api.ApiException;

import java.util.List;
import java.util.UUID;

/** BR-05 工单状态机与文本规则；长度按 Unicode 码点计数。 */
public final class TicketRules {
    private TicketRules() {}

    public static String text(String field, String value, int min, int max) {
        String v = value == null ? "" : value.strip();
        int n = v.codePointCount(0, v.length());
        if (n < min || n > max) throw invalid(field, "字符数需为 " + min + "—" + max + "。");
        return v;
    }

    /** 可选电话：空白视为未填写，填写时与收货电话同规则。 */
    public static String phone(String value) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) return null;
        String digits = v.replaceAll("[\\s\\-()]", "");
        if (!digits.matches("^\\+?[0-9]{6,20}$")) throw invalid("phone", "请输入6—20位联系电话。");
        return digits;
    }

    /** 咨询类型与业务关联的对应关系（GENERAL 无关联 / PRODUCT 商品 / AFTER_SALES 本人订单）。 */
    public static void association(TicketType type, UUID productId, UUID orderId) {
        switch (type) {
            case GENERAL -> {
                if (productId != null) throw invalid("productId", "一般咨询不关联商品。");
                if (orderId != null) throw invalid("orderId", "一般咨询不关联订单。");
            }
            case PRODUCT -> {
                if (productId == null) throw invalid("productId", "产品咨询必须关联一个商品。");
                if (orderId != null) throw invalid("orderId", "产品咨询不关联订单。");
            }
            case AFTER_SALES -> {
                if (orderId == null) throw invalid("orderId", "售后咨询必须关联一个本人订单。");
                if (productId != null) throw invalid("productId", "售后咨询不关联商品。");
            }
        }
    }

    /** 用户补充：已回复回处理中，其余未关闭状态保持不变。 */
    public static TicketStatus afterFollowUp(TicketStatus current) {
        return switch (current) {
            case CLOSED -> throw state();
            case REPLIED -> TicketStatus.PROCESSING;
            case NEW, PROCESSING -> current;
        };
    }

    /** 管理员非空公开回复：未关闭状态统一转为已回复。 */
    public static TicketStatus afterReply(TicketStatus current) {
        return switch (current) {
            case CLOSED -> throw state();
            default -> TicketStatus.REPLIED;
        };
    }

    public static void requireStartable(TicketStatus current) {
        if (current != TicketStatus.NEW) throw state();
    }

    public static void requireOpen(TicketStatus current) {
        if (current == TicketStatus.CLOSED) throw state();
    }

    /** 前端动作按钮的服务端权威推导。 */
    public static List<String> userActions(TicketStatus status) {
        return status == TicketStatus.CLOSED ? List.of() : List.of("FOLLOW_UP", "CLOSE");
    }

    public static List<String> adminActions(TicketStatus status) {
        if (status == TicketStatus.CLOSED) return List.of();
        return status == TicketStatus.NEW
                ? List.of("START", "REPLY", "NOTE", "CLOSE")
                : List.of("REPLY", "NOTE", "CLOSE");
    }

    public static void version(long actual, long expected) {
        if (actual != expected) throw conflict("VERSION_CONFLICT", "工单已更新，请刷新后重新确认。");
    }

    public static ApiException state() {
        return conflict("STATE_CONFLICT", "当前工单状态不允许此操作。");
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
