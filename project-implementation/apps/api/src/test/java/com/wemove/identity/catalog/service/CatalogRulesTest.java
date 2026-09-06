package com.wemove.identity.catalog.service;

import static org.junit.jupiter.api.Assertions.*;

import com.wemove.identity.api.ApiException;
import com.wemove.identity.catalog.api.CatalogDtos;
import com.wemove.identity.catalog.domain.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogRulesTest {
    @Test
    void rejectsMutableLookingOrLowercaseSku() {
        CatalogDtos.CreateProductRequest request = request("wm bad", 100, false, null, null, null);
        ApiException failure = assertThrows(ApiException.class, () -> CatalogRules.validateDraft(request));
        assertEquals("VALIDATION_ERROR", failure.getCode());
        assertEquals("sku", failure.getErrors().getFirst().field());
    }

    @Test
    void publishedDealerProductRequiresPrivatePriceQuantityAndLeadTime() {
        ApiException failure = assertThrows(ApiException.class, () -> CatalogRules.requirePublishable(
            "WM-OK-001", "测试商品", new Object(), "测试简述", 5, PlayType.BALANCE, ProductScene.BOTH,
            "测试材质", "10 × 10 cm", "组件一套", "按说明使用", "成人看护", "asset_main",
            100L, true, null, null, null));
        assertEquals("dealerReferenceUnitPriceFen", failure.getErrors().getFirst().field());
    }

    @Test
    void priceUsesIntegerFenAndHonorsUpperBound() {
        CatalogRules.validateDraft(request("WM-OK-001", 99_999_999L, false, null, null, null));
        ApiException failure = assertThrows(ApiException.class,
            () -> CatalogRules.validateDraft(request("WM-OK-002", 100_000_000L, false, null, null, null)));
        assertEquals("retailUnitPriceFen", failure.getErrors().getFirst().field());
    }

    private CatalogDtos.CreateProductRequest request(String sku, long retailPrice, boolean dealer,
                                                       Long dealerPrice, Integer minimum, String leadTime) {
        return new CatalogDtos.CreateProductRequest(sku, "测试商品", null, "测试简述", null,
            5, null, PlayType.BALANCE, ProductScene.BOTH, "测试材质", "10 × 10 cm", "组件一套",
            "按说明使用", "成人看护", "asset_main", List.of(), retailPrice, dealer, dealerPrice,
            minimum, leadTime, 0, 0);
    }
}
