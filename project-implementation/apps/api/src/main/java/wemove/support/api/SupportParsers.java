package wemove.support.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import wemove.support.domain.TicketRules;
import wemove.support.domain.TicketStatus;
import wemove.support.domain.TicketType;

import java.util.Locale;

/** 工单接口的查询参数解析（枚举与分页，非法值统一 422）。 */
final class SupportParsers {
    private SupportParsers() {}

    static PageRequest pageable(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50)
            throw TicketRules.invalid("page", "分页参数不合法。");
        return PageRequest.of(
                page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
    }

    static TicketType type(String value) {
        return enumValue(value, TicketType.class, "type");
    }

    static TicketStatus status(String value) {
        return enumValue(value, TicketStatus.class, "status");
    }

    private static <E extends Enum<E>> E enumValue(String value, Class<E> type, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw TicketRules.invalid(field, field + " 不合法。");
        }
    }
}
