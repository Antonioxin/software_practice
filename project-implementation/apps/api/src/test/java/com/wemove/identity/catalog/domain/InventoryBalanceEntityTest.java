package com.wemove.identity.catalog.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.wemove.identity.api.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryBalanceEntityTest {
    @Test
    void adjustmentsUseDeltasAndNeverAllowNegativeStock() {
        InventoryBalanceEntity balance = new InventoryBalanceEntity(UUID.randomUUID(), 2, Instant.EPOCH);

        InventoryBalanceEntity.StockChange decrease = balance.adjust(StockDirection.DECREASE, 1, Instant.EPOCH.plusSeconds(1));
        assertEquals(2, decrease.before());
        assertEquals(1, decrease.after());
        assertEquals(1, balance.getQuantity());

        ApiException failure = assertThrows(ApiException.class,
            () -> balance.adjust(StockDirection.DECREASE, 2, Instant.EPOCH.plusSeconds(2)));
        assertEquals("INSUFFICIENT_STOCK", failure.getCode());
        assertEquals(1, balance.getQuantity());
    }

    @Test
    void increaseReportsAuditableBeforeAndAfterValues() {
        InventoryBalanceEntity balance = new InventoryBalanceEntity(UUID.randomUUID(), 0, Instant.EPOCH);
        InventoryBalanceEntity.StockChange change = balance.adjust(StockDirection.INCREASE, 5, Instant.EPOCH.plusSeconds(1));
        assertEquals(0, change.before());
        assertEquals(5, change.after());
    }
}
