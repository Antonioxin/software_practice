package wemove.commerce;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import wemove.commerce.api.CommerceDtos.ItemSummary;
import wemove.commerce.domain.Order;
import wemove.commerce.domain.OrderItem;
import wemove.commerce.repository.CommerceRepository;
import wemove.commerce.service.OrderQueryService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

class OrderQueryServiceTest {
    private final CommerceRepository db = mock(CommerceRepository.class);
    private final OrderQueryService queries = new OrderQueryService(db);

    @Test
    void listLoadsOnlyTheAuthorizedPageInOneBatchAndKeepsOrderSnapshots() {
        UUID actor = UUID.randomUUID();
        Instant start = Instant.parse("2026-09-01T00:00:00Z");
        Instant end = Instant.parse("2026-10-01T00:00:00Z");
        Order first = order(actor);
        Order second = order(actor);
        Order withoutItems = order(actor);
        OrderItem firstItem = item(first, "OLD-SKU", "下单时的平衡石名称", 2);
        OrderItem otherItem = item(first, "RING-SET", "投环组", 3);
        OrderItem secondItem = item(second, "RAINBOW", "彩虹积木", 1);
        when(db.list(actor, "PAID", start, end, 3, 3))
                .thenReturn(List.of(first, second, withoutItems));
        when(db.itemsForOrders(List.of(first.id, second.id, withoutItems.id)))
                .thenReturn(List.of(secondItem, firstItem, otherItem));
        when(db.count(actor, "PAID", start, end)).thenReturn(8L);

        var page = queries.list(actor, false, "PAID", start, end, 2, 3);

        assertThat(page.items()).extracting(o -> o.id())
                .containsExactly(first.id, second.id, withoutItems.id);
        assertThat(page.items().getFirst().items())
                .containsExactly(
                        new ItemSummary(firstItem.productId, "OLD-SKU", "下单时的平衡石名称", 2),
                        new ItemSummary(otherItem.productId, "RING-SET", "投环组", 3));
        assertThat(page.items().get(1).items())
                .containsExactly(new ItemSummary(secondItem.productId, "RAINBOW", "彩虹积木", 1));
        assertThat(page.items().get(2).items()).isEmpty();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(3);
        assertThat(page.total()).isEqualTo(8);
        verify(db).list(actor, "PAID", start, end, 3, 3);
        verify(db).itemsForOrders(List.of(first.id, second.id, withoutItems.id));
        verify(db).count(actor, "PAID", start, end);
        verifyNoMoreInteractions(db);
    }

    @Test
    void emptyPageDoesNotQueryOrderItems() {
        UUID actor = UUID.randomUUID();
        when(db.list(actor, null, null, null, 10, 10)).thenReturn(List.of());
        when(db.count(actor, null, null, null)).thenReturn(5L);

        var page = queries.list(actor, false, null, null, null, 2, 10);

        assertThat(page.items()).isEmpty();
        assertThat(page.total()).isEqualTo(5);
        verify(db).list(actor, null, null, null, 10, 10);
        verify(db).count(actor, null, null, null);
        verifyNoMoreInteractions(db);
    }

    @Test
    void adminPageIncludesSnapshotsForItsPageAcrossOwners() {
        UUID admin = UUID.randomUUID();
        Order first = order(UUID.randomUUID());
        Order second = order(UUID.randomUUID());
        OrderItem firstItem = item(first, "FIRST", "商品一", 4);
        OrderItem secondItem = item(second, "SECOND", "商品二", 5);
        when(db.list(null, null, null, null, 0, 10)).thenReturn(List.of(first, second));
        when(db.itemsForOrders(List.of(first.id, second.id)))
                .thenReturn(List.of(firstItem, secondItem));
        when(db.count(null, null, null, null)).thenReturn(2L);

        var page = queries.list(admin, true, null, null, null, 1, 10);

        assertThat(page.items().getFirst().items())
                .containsExactly(new ItemSummary(firstItem.productId, "FIRST", "商品一", 4));
        assertThat(page.items().getLast().items())
                .containsExactly(new ItemSummary(secondItem.productId, "SECOND", "商品二", 5));
        verify(db).list(null, null, null, null, 0, 10);
        verify(db).itemsForOrders(List.of(first.id, second.id));
        verify(db).count(null, null, null, null);
        verifyNoMoreInteractions(db);
    }

    @Test
    void creationSummaryIncludesTheSavedOrderItems() {
        Order order = order(UUID.randomUUID());
        OrderItem item = item(order, "BALANCE", "平衡石套装", 6);
        when(db.items(order.id)).thenReturn(List.of(item));

        var summary = queries.summary(order);

        assertThat(summary.id()).isEqualTo(order.id);
        assertThat(summary.orderNumber()).isEqualTo(order.orderNumber);
        assertThat(summary.status()).isEqualTo(order.status);
        assertThat(summary.totalFen()).isEqualTo(order.totalFen);
        assertThat(summary.items())
                .containsExactly(new ItemSummary(item.productId, "BALANCE", "平衡石套装", 6));
        verify(db).items(order.id);
        verifyNoMoreInteractions(db);
    }

    private Order order(UUID owner) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.orderNumber = "WM" + order.id.toString().replace("-", "");
        order.userId = owner;
        order.status = "PAID";
        order.totalFen = 45000;
        order.createdAt = Instant.parse("2026-09-07T12:00:00Z");
        return order;
    }

    private OrderItem item(Order order, String sku, String name, int quantity) {
        OrderItem item = new OrderItem();
        item.id = UUID.randomUUID();
        item.orderId = order.id;
        item.productId = UUID.randomUUID();
        item.sku = sku;
        item.name = name;
        item.quantity = quantity;
        return item;
    }
}
