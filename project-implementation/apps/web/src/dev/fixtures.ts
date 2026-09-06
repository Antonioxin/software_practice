// Development-only display fixtures. Import only through the DEV-gated preview adapter.
import type { Actor, AdminProduct, Category, ProductOptions, PublicProduct, UserDetail, UserSummary } from '../types'
import type { Cart, Detail, Order, Preview } from '../features/commerce/types'

interface StockMovement {
  id: string; productId: string; direction: string; quantity: number
  quantityBefore: number; quantityAfter: number; reason: string; sourceType: string
  sourceId: string; actorId: string; createdAt: string
}
interface PreviewFixtures {
  actors: Record<'user' | 'admin', Actor>
  users: UserSummary[]
  userDetails: Record<string, UserDetail>
  categories: Category[]
  products: PublicProduct[]
  adminProducts: AdminProduct[]
  productOptions: ProductOptions
  stockMovements: Record<string, StockMovement[]>
  orders: Detail[]
  orderSummaries: Order[]
  adminOrderAllowedActions: Record<string, string[]>
  cart: Cart
  checkoutPreview: Preview & { currency: string }
  registrationPolicy: {
    adultStatement: string; termsVersion: string; termsPath: string
    privacyVersion: string; privacyPath: string
  }
}

export const fixtures: PreviewFixtures = {
  "actors": {
    "admin": {
      "id": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
      "email": "admin@example.invalid",
      "nickname": "示例管理员",
      "phone": null,
      "baseRole": "ADMIN",
      "accountStatus": "ACTIVE",
      "derivedIdentity": "USER",
      "version": 1,
      "capabilities": [
        "ADMIN_USERS_READ",
        "ADMIN_USERS_WRITE",
        "ADMIN_CATALOG_READ",
        "ADMIN_CATALOG_WRITE",
        "ADMIN_ORDERS_READ",
        "ADMIN_ORDERS_WRITE"
      ]
    },
    "user": {
      "id": "92c2fcc3-f5f1-5b2a-9dd1-5731b12dd824",
      "email": "hello@example.invalid",
      "nickname": "示例体验用户",
      "phone": null,
      "baseRole": "USER",
      "accountStatus": "ACTIVE",
      "derivedIdentity": "USER",
      "version": 1,
      "capabilities": [
        "ACCOUNT_PROFILE_READ",
        "ACCOUNT_PROFILE_WRITE",
        "CART_READ",
        "CART_WRITE",
        "ORDERS_READ",
        "ORDERS_WRITE"
      ]
    }
  },
  "users": [
    {
      "id": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
      "email": "admin@example.invalid",
      "nickname": "示例管理员",
      "phone": null,
      "baseRole": "ADMIN",
      "accountStatus": "ACTIVE",
      "derivedIdentity": "USER",
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z",
      "updatedAt": "2026-09-06T02:00:00Z"
    },
    {
      "id": "92c2fcc3-f5f1-5b2a-9dd1-5731b12dd824",
      "email": "hello@example.invalid",
      "nickname": "示例体验用户",
      "phone": null,
      "baseRole": "USER",
      "accountStatus": "ACTIVE",
      "derivedIdentity": "USER",
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z",
      "updatedAt": "2026-09-06T02:00:00Z"
    },
    {
      "id": "5b596a5a-d1f3-5db5-9c92-1cfe832bf35a",
      "email": "paused@example.invalid",
      "nickname": "示例停用账户",
      "phone": null,
      "baseRole": "USER",
      "accountStatus": "DISABLED",
      "derivedIdentity": "USER",
      "version": 2,
      "createdAt": "2026-09-01T02:00:00Z",
      "updatedAt": "2026-09-06T02:00:00Z"
    }
  ],
  "userDetails": {
    "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003": {
      "account": {
        "id": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
        "email": "admin@example.invalid",
        "nickname": "示例管理员",
        "phone": null,
        "baseRole": "ADMIN",
        "accountStatus": "ACTIVE",
        "derivedIdentity": "USER",
        "version": 1,
        "createdAt": "2026-09-01T02:00:00Z",
        "updatedAt": "2026-09-06T02:00:00Z"
      },
      "statusHistory": []
    },
    "92c2fcc3-f5f1-5b2a-9dd1-5731b12dd824": {
      "account": {
        "id": "92c2fcc3-f5f1-5b2a-9dd1-5731b12dd824",
        "email": "hello@example.invalid",
        "nickname": "示例体验用户",
        "phone": null,
        "baseRole": "USER",
        "accountStatus": "ACTIVE",
        "derivedIdentity": "USER",
        "version": 1,
        "createdAt": "2026-09-01T02:00:00Z",
        "updatedAt": "2026-09-06T02:00:00Z"
      },
      "statusHistory": []
    },
    "5b596a5a-d1f3-5db5-9c92-1cfe832bf35a": {
      "account": {
        "id": "5b596a5a-d1f3-5db5-9c92-1cfe832bf35a",
        "email": "paused@example.invalid",
        "nickname": "示例停用账户",
        "phone": null,
        "baseRole": "USER",
        "accountStatus": "DISABLED",
        "derivedIdentity": "USER",
        "version": 2,
        "createdAt": "2026-09-01T02:00:00Z",
        "updatedAt": "2026-09-06T02:00:00Z"
      },
      "statusHistory": [
        {
          "action": "DISABLE",
          "previousStatus": "ACTIVE",
          "newStatus": "DISABLED",
          "reason": "示例状态展示，不关联真实账户。",
          "createdAt": "2026-09-06T02:00:00Z"
        }
      ]
    }
  },
  "categories": [
    {
      "id": "161e57ac-26f4-5743-9f0c-7bb50db857e3",
      "name": "平衡与探索",
      "description": "用身体感知重心，在游戏中探索空间。",
      "sortOrder": 10,
      "enabled": true,
      "version": 1
    },
    {
      "id": "de295d63-b907-550f-ac64-22289c579df3",
      "name": "协作与投掷",
      "description": "和朋友一起投、接、配合，发现默契。",
      "sortOrder": 20,
      "enabled": true,
      "version": 1
    },
    {
      "id": "d7f370a5-45e7-5609-b3fc-bbf6b7507ac8",
      "name": "户外与运动",
      "description": "带上轻巧器材，把游戏带到户外。",
      "sortOrder": 30,
      "enabled": true,
      "version": 1
    }
  ],
  "products": [
    {
      "id": "469ec43c-559a-5040-8015-614ed58f12a6",
      "sku": "WM-BALANCE-STONES",
      "name": "平衡石 · 示例",
      "summary": "用六块柔和色彩的平衡石，搭一条自己的小路。",
      "description": "用六块柔和色彩的平衡石，搭一条自己的小路。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 3,
      "ageMax": 10,
      "playType": "BALANCE",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-1",
      "imageIds": [
        "preview-art-1"
      ],
      "retailUnitPriceFen": 25900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "category": {
        "id": "161e57ac-26f4-5743-9f0c-7bb50db857e3",
        "name": "平衡与探索",
        "description": "用身体感知重心，在游戏中探索空间。",
        "sortOrder": 10,
        "enabled": true,
        "version": 1
      },
      "currency": "CNY",
      "inStock": true,
      "stockStatus": "IN_STOCK",
      "purchasable": true,
      "availabilityMessage": "当前有货"
    },
    {
      "id": "f793492f-b8c1-5703-a4d6-ea9ace37c8e8",
      "sku": "WM-RAINBOW-ARCH",
      "name": "彩虹拱桥 · 示例",
      "summary": "堆叠、穿越、想象，让简单的形状连接更多玩法。",
      "description": "堆叠、穿越、想象，让简单的形状连接更多玩法。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 3,
      "ageMax": 12,
      "playType": "COORDINATION",
      "scene": "INDOOR",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-2",
      "imageIds": [
        "preview-art-2"
      ],
      "retailUnitPriceFen": 32900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "category": {
        "id": "161e57ac-26f4-5743-9f0c-7bb50db857e3",
        "name": "平衡与探索",
        "description": "用身体感知重心，在游戏中探索空间。",
        "sortOrder": 10,
        "enabled": true,
        "version": 1
      },
      "currency": "CNY",
      "inStock": true,
      "stockStatus": "IN_STOCK",
      "purchasable": true,
      "availabilityMessage": "当前有货"
    },
    {
      "id": "9b07436c-613e-53ba-b12e-f552254283fe",
      "sku": "WM-RING-TOSS",
      "name": "环环投掷 · 示例",
      "summary": "循序渐进的投掷挑战，练习专注与手眼协调。",
      "description": "循序渐进的投掷挑战，练习专注与手眼协调。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 4,
      "ageMax": 14,
      "playType": "THROWING",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-3",
      "imageIds": [
        "preview-art-3"
      ],
      "retailUnitPriceFen": 16900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "category": {
        "id": "de295d63-b907-550f-ac64-22289c579df3",
        "name": "协作与投掷",
        "description": "和朋友一起投、接、配合，发现默契。",
        "sortOrder": 20,
        "enabled": true,
        "version": 1
      },
      "currency": "CNY",
      "inStock": true,
      "stockStatus": "IN_STOCK",
      "purchasable": true,
      "availabilityMessage": "当前有货"
    },
    {
      "id": "d84473a5-10f5-5dad-b124-74a521a1f0f1",
      "sku": "WM-TEAM-BOARD",
      "name": "伙伴协作板 · 示例",
      "summary": "一起出发、一起保持平衡，把合作变成游戏。",
      "description": "一起出发、一起保持平衡，把合作变成游戏。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 6,
      "ageMax": 16,
      "playType": "TEAM_PLAY",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-4",
      "imageIds": [
        "preview-art-4"
      ],
      "retailUnitPriceFen": 28900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "category": {
        "id": "de295d63-b907-550f-ac64-22289c579df3",
        "name": "协作与投掷",
        "description": "和朋友一起投、接、配合，发现默契。",
        "sortOrder": 20,
        "enabled": true,
        "version": 1
      },
      "currency": "CNY",
      "inStock": true,
      "stockStatus": "IN_STOCK",
      "purchasable": true,
      "availabilityMessage": "当前有货"
    },
    {
      "id": "ada916b9-cd61-5de8-89d0-1668f9869376",
      "sku": "WM-FOREST-KIT",
      "name": "森林探索套装 · 示例",
      "summary": "收集色彩、观察纹理，为一次散步增添新发现。",
      "description": "收集色彩、观察纹理，为一次散步增添新发现。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 5,
      "ageMax": 14,
      "playType": "OUTDOOR_EXPLORATION",
      "scene": "OUTDOOR",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-5",
      "imageIds": [
        "preview-art-5"
      ],
      "retailUnitPriceFen": 21900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "category": {
        "id": "d7f370a5-45e7-5609-b3fc-bbf6b7507ac8",
        "name": "户外与运动",
        "description": "带上轻巧器材，把游戏带到户外。",
        "sortOrder": 30,
        "enabled": true,
        "version": 1
      },
      "currency": "CNY",
      "inStock": true,
      "stockStatus": "IN_STOCK",
      "purchasable": true,
      "availabilityMessage": "当前有货"
    },
    {
      "id": "2570598e-b992-5e3f-b2cd-126b34027881",
      "sku": "WM-SKIP-ROPE",
      "name": "轻盈跳绳 · 示例",
      "summary": "轻巧握柄与适合练习的绳长，找到自己的节奏。",
      "description": "轻巧握柄与适合练习的绳长，找到自己的节奏。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 6,
      "ageMax": 18,
      "playType": "COORDINATION",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-6",
      "imageIds": [
        "preview-art-6"
      ],
      "retailUnitPriceFen": 8900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "category": {
        "id": "d7f370a5-45e7-5609-b3fc-bbf6b7507ac8",
        "name": "户外与运动",
        "description": "带上轻巧器材，把游戏带到户外。",
        "sortOrder": 30,
        "enabled": true,
        "version": 1
      },
      "currency": "CNY",
      "inStock": false,
      "stockStatus": "OUT_OF_STOCK",
      "purchasable": false,
      "availabilityMessage": "暂时缺货"
    }
  ],
  "adminProducts": [
    {
      "id": "469ec43c-559a-5040-8015-614ed58f12a6",
      "sku": "WM-BALANCE-STONES",
      "name": "平衡石 · 示例",
      "summary": "用六块柔和色彩的平衡石，搭一条自己的小路。",
      "description": "用六块柔和色彩的平衡石，搭一条自己的小路。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 3,
      "ageMax": 10,
      "playType": "BALANCE",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-1",
      "imageIds": [
        "preview-art-1"
      ],
      "retailUnitPriceFen": 25900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "categoryId": "161e57ac-26f4-5743-9f0c-7bb50db857e3",
      "categoryName": "平衡与探索",
      "dealerEnabled": true,
      "dealerReferenceUnitPriceFen": 20720,
      "minInquiryQuantity": 10,
      "leadTimeText": "示例：7—10 个工作日",
      "displayOrder": 10,
      "stock": 42,
      "stockVersion": 1,
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z"
    },
    {
      "id": "f793492f-b8c1-5703-a4d6-ea9ace37c8e8",
      "sku": "WM-RAINBOW-ARCH",
      "name": "彩虹拱桥 · 示例",
      "summary": "堆叠、穿越、想象，让简单的形状连接更多玩法。",
      "description": "堆叠、穿越、想象，让简单的形状连接更多玩法。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 3,
      "ageMax": 12,
      "playType": "COORDINATION",
      "scene": "INDOOR",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-2",
      "imageIds": [
        "preview-art-2"
      ],
      "retailUnitPriceFen": 32900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "categoryId": "161e57ac-26f4-5743-9f0c-7bb50db857e3",
      "categoryName": "平衡与探索",
      "dealerEnabled": true,
      "dealerReferenceUnitPriceFen": 26320,
      "minInquiryQuantity": 10,
      "leadTimeText": "示例：7—10 个工作日",
      "displayOrder": 20,
      "stock": 18,
      "stockVersion": 1,
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z"
    },
    {
      "id": "9b07436c-613e-53ba-b12e-f552254283fe",
      "sku": "WM-RING-TOSS",
      "name": "环环投掷 · 示例",
      "summary": "循序渐进的投掷挑战，练习专注与手眼协调。",
      "description": "循序渐进的投掷挑战，练习专注与手眼协调。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 4,
      "ageMax": 14,
      "playType": "THROWING",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-3",
      "imageIds": [
        "preview-art-3"
      ],
      "retailUnitPriceFen": 16900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "categoryId": "de295d63-b907-550f-ac64-22289c579df3",
      "categoryName": "协作与投掷",
      "dealerEnabled": false,
      "dealerReferenceUnitPriceFen": null,
      "minInquiryQuantity": null,
      "leadTimeText": null,
      "displayOrder": 30,
      "stock": 35,
      "stockVersion": 1,
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z"
    },
    {
      "id": "d84473a5-10f5-5dad-b124-74a521a1f0f1",
      "sku": "WM-TEAM-BOARD",
      "name": "伙伴协作板 · 示例",
      "summary": "一起出发、一起保持平衡，把合作变成游戏。",
      "description": "一起出发、一起保持平衡，把合作变成游戏。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 6,
      "ageMax": 16,
      "playType": "TEAM_PLAY",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-4",
      "imageIds": [
        "preview-art-4"
      ],
      "retailUnitPriceFen": 28900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "categoryId": "de295d63-b907-550f-ac64-22289c579df3",
      "categoryName": "协作与投掷",
      "dealerEnabled": true,
      "dealerReferenceUnitPriceFen": 23120,
      "minInquiryQuantity": 10,
      "leadTimeText": "示例：7—10 个工作日",
      "displayOrder": 40,
      "stock": 12,
      "stockVersion": 1,
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z"
    },
    {
      "id": "ada916b9-cd61-5de8-89d0-1668f9869376",
      "sku": "WM-FOREST-KIT",
      "name": "森林探索套装 · 示例",
      "summary": "收集色彩、观察纹理，为一次散步增添新发现。",
      "description": "收集色彩、观察纹理，为一次散步增添新发现。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 5,
      "ageMax": 14,
      "playType": "OUTDOOR_EXPLORATION",
      "scene": "OUTDOOR",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-5",
      "imageIds": [
        "preview-art-5"
      ],
      "retailUnitPriceFen": 21900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "categoryId": "d7f370a5-45e7-5609-b3fc-bbf6b7507ac8",
      "categoryName": "户外与运动",
      "dealerEnabled": false,
      "dealerReferenceUnitPriceFen": null,
      "minInquiryQuantity": null,
      "leadTimeText": null,
      "displayOrder": 50,
      "stock": 27,
      "stockVersion": 1,
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z"
    },
    {
      "id": "2570598e-b992-5e3f-b2cd-126b34027881",
      "sku": "WM-SKIP-ROPE",
      "name": "轻盈跳绳 · 示例",
      "summary": "轻巧握柄与适合练习的绳长，找到自己的节奏。",
      "description": "轻巧握柄与适合练习的绳长，找到自己的节奏。 本资料为前端展示样例，用于检查排版、筛选与详情布局。",
      "ageMin": 6,
      "ageMax": 18,
      "playType": "COORDINATION",
      "scene": "BOTH",
      "material": "示例材质：圆角木质构件与水性涂层。",
      "dimensions": "示例规格：320 × 240 × 80 mm；以实际产品资料为准。",
      "packageContents": "主体 1 套、收纳袋 1 件、玩法说明 1 份。",
      "instructions": "由成人陪同，在平坦开阔的空间内使用。可按能力调整难度，先熟悉基础动作再组合挑战。",
      "safetyNotes": "仅为界面展示样例；使用前请检查器材完整性，并由成人陪同。",
      "mainImageId": "preview-art-6",
      "imageIds": [
        "preview-art-6"
      ],
      "retailUnitPriceFen": 8900,
      "status": "PUBLISHED",
      "updatedAt": "2026-09-06T02:00:00Z",
      "categoryId": "d7f370a5-45e7-5609-b3fc-bbf6b7507ac8",
      "categoryName": "户外与运动",
      "dealerEnabled": false,
      "dealerReferenceUnitPriceFen": null,
      "minInquiryQuantity": null,
      "leadTimeText": null,
      "displayOrder": 60,
      "stock": 0,
      "stockVersion": 1,
      "version": 1,
      "createdAt": "2026-09-01T02:00:00Z"
    }
  ],
  "productOptions": {
    "playTypes": [
      {
        "value": "BALANCE",
        "label": "平衡能力"
      },
      {
        "value": "COORDINATION",
        "label": "协调训练"
      },
      {
        "value": "THROWING",
        "label": "投掷与瞄准"
      },
      {
        "value": "TEAM_PLAY",
        "label": "团队游戏"
      },
      {
        "value": "OUTDOOR_EXPLORATION",
        "label": "户外探索"
      }
    ],
    "scenes": [
      {
        "value": "INDOOR",
        "label": "室内"
      },
      {
        "value": "OUTDOOR",
        "label": "户外"
      },
      {
        "value": "BOTH",
        "label": "室内与户外"
      }
    ]
  },
  "stockMovements": {
    "469ec43c-559a-5040-8015-614ed58f12a6": [
      {
        "id": "6db8809e-94e5-5d5c-92e8-0c2201274d69",
        "productId": "469ec43c-559a-5040-8015-614ed58f12a6",
        "direction": "INCREASE",
        "quantity": 42,
        "quantityBefore": 0,
        "quantityAfter": 42,
        "reason": "示例初始库存记录",
        "sourceType": "PRODUCT_CREATE",
        "sourceId": "469ec43c-559a-5040-8015-614ed58f12a6",
        "actorId": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
        "createdAt": "2026-09-01T02:00:00Z"
      }
    ],
    "f793492f-b8c1-5703-a4d6-ea9ace37c8e8": [
      {
        "id": "d1597636-3784-5515-b133-6d83cdf1342a",
        "productId": "f793492f-b8c1-5703-a4d6-ea9ace37c8e8",
        "direction": "INCREASE",
        "quantity": 18,
        "quantityBefore": 0,
        "quantityAfter": 18,
        "reason": "示例初始库存记录",
        "sourceType": "PRODUCT_CREATE",
        "sourceId": "f793492f-b8c1-5703-a4d6-ea9ace37c8e8",
        "actorId": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
        "createdAt": "2026-09-01T02:00:00Z"
      }
    ],
    "9b07436c-613e-53ba-b12e-f552254283fe": [
      {
        "id": "4e274d9f-3245-5b60-93f3-53f6a033f598",
        "productId": "9b07436c-613e-53ba-b12e-f552254283fe",
        "direction": "INCREASE",
        "quantity": 35,
        "quantityBefore": 0,
        "quantityAfter": 35,
        "reason": "示例初始库存记录",
        "sourceType": "PRODUCT_CREATE",
        "sourceId": "9b07436c-613e-53ba-b12e-f552254283fe",
        "actorId": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
        "createdAt": "2026-09-01T02:00:00Z"
      }
    ],
    "d84473a5-10f5-5dad-b124-74a521a1f0f1": [
      {
        "id": "dd35166c-4036-55a9-8b0c-0820d1f50418",
        "productId": "d84473a5-10f5-5dad-b124-74a521a1f0f1",
        "direction": "INCREASE",
        "quantity": 12,
        "quantityBefore": 0,
        "quantityAfter": 12,
        "reason": "示例初始库存记录",
        "sourceType": "PRODUCT_CREATE",
        "sourceId": "d84473a5-10f5-5dad-b124-74a521a1f0f1",
        "actorId": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
        "createdAt": "2026-09-01T02:00:00Z"
      }
    ],
    "ada916b9-cd61-5de8-89d0-1668f9869376": [
      {
        "id": "782d2df0-9568-52be-8e6f-af364a4cefc6",
        "productId": "ada916b9-cd61-5de8-89d0-1668f9869376",
        "direction": "INCREASE",
        "quantity": 27,
        "quantityBefore": 0,
        "quantityAfter": 27,
        "reason": "示例初始库存记录",
        "sourceType": "PRODUCT_CREATE",
        "sourceId": "ada916b9-cd61-5de8-89d0-1668f9869376",
        "actorId": "4e9c3cd8-e9ab-50e1-bbd9-10131f6cc003",
        "createdAt": "2026-09-01T02:00:00Z"
      }
    ],
    "2570598e-b992-5e3f-b2cd-126b34027881": []
  },
  "orders": [
    {
      "id": "fafbf3d3-db7f-5a18-bd98-68f59ccafaa3",
      "orderNumber": "WM-SAMPLE-20260905-001",
      "status": "PENDING_PAYMENT",
      "version": 1,
      "totalFen": 59700,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-05T02:00:00Z",
      "shippingAddress": {
        "recipient": "示例收件人",
        "phone": "+86 10000000000",
        "countryOrRegion": "中国",
        "region": "示例省",
        "city": "示例城市",
        "addressLine": "示例展示路 100 号（非真实地址）"
      },
      "remark": "只读界面样例，不产生交易或物流。",
      "items": [
        {
          "productId": "469ec43c-559a-5040-8015-614ed58f12a6",
          "sku": "WM-BALANCE-STONES",
          "name": "平衡石 · 示例",
          "unitPriceFen": 25900,
          "quantity": 1,
          "subtotalFen": 25900,
          "valid": true,
          "reason": null,
          "priceChanged": false,
          "previousUnitPriceFen": 25900
        },
        {
          "productId": "9b07436c-613e-53ba-b12e-f552254283fe",
          "sku": "WM-RING-TOSS",
          "name": "环环投掷 · 示例",
          "unitPriceFen": 16900,
          "quantity": 2,
          "subtotalFen": 33800,
          "valid": true,
          "reason": null,
          "priceChanged": false,
          "previousUnitPriceFen": 16900
        }
      ],
      "subtotalFen": 59700,
      "shippingFen": 0,
      "taxFen": 0,
      "discountFen": 0,
      "allowedActions": [
        "CANCEL",
        "MOCK_PAYMENT"
      ],
      "logisticsName": null,
      "trackingNumber": null,
      "paymentAttempts": [],
      "refunds": [],
      "history": [
        {
          "action": "CREATE",
          "fromStatus": null,
          "toStatus": "PENDING_PAYMENT",
          "version": 1,
          "reason": "示例订单创建",
          "createdAt": "2026-09-05T02:00:00Z"
        }
      ]
    },
    {
      "id": "0caf56da-db21-5081-8bf6-baa006fa4789",
      "orderNumber": "WM-SAMPLE-20260904-002",
      "status": "SHIPPED",
      "version": 3,
      "totalFen": 32900,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-04T02:00:00Z",
      "shippingAddress": {
        "recipient": "示例收件人",
        "phone": "+86 10000000000",
        "countryOrRegion": "中国",
        "region": "示例省",
        "city": "示例城市",
        "addressLine": "示例展示路 100 号（非真实地址）"
      },
      "remark": "只读界面样例，不产生交易或物流。",
      "items": [
        {
          "productId": "f793492f-b8c1-5703-a4d6-ea9ace37c8e8",
          "sku": "WM-RAINBOW-ARCH",
          "name": "彩虹拱桥 · 示例",
          "unitPriceFen": 32900,
          "quantity": 1,
          "subtotalFen": 32900,
          "valid": true,
          "reason": null,
          "priceChanged": false,
          "previousUnitPriceFen": 32900
        }
      ],
      "subtotalFen": 32900,
      "shippingFen": 0,
      "taxFen": 0,
      "discountFen": 0,
      "allowedActions": [
        "CONFIRM_RECEIPT"
      ],
      "logisticsName": "示例模拟物流",
      "trackingNumber": "SAMPLE-TRACK-001",
      "paymentAttempts": [
        {
          "id": "1cc18d4e-9a4d-5ec8-b12b-f653b0683573",
          "outcome": "SUCCESS",
          "amountFen": 32900,
          "simulationReference": "SIM-PAY-SAMPLE-001"
        }
      ],
      "refunds": [],
      "history": [
        {
          "action": "CREATE",
          "fromStatus": null,
          "toStatus": "PENDING_PAYMENT",
          "version": 1,
          "reason": "示例订单创建",
          "createdAt": "2026-09-04T02:00:00Z"
        },
        {
          "action": "MOCK_PAYMENT",
          "fromStatus": "PENDING_PAYMENT",
          "toStatus": "PAID",
          "version": 2,
          "reason": "示例模拟付款成功",
          "createdAt": "2026-09-04T02:10:00Z"
        },
        {
          "action": "MOCK_SHIPMENT",
          "fromStatus": "PAID",
          "toStatus": "SHIPPED",
          "version": 3,
          "reason": "示例模拟整单发货",
          "createdAt": "2026-09-04T06:00:00Z"
        }
      ]
    },
    {
      "id": "6d0182f8-6102-5829-b9d4-18f765d61248",
      "orderNumber": "WM-SAMPLE-20260903-003",
      "status": "COMPLETED",
      "version": 4,
      "totalFen": 50800,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-03T02:00:00Z",
      "shippingAddress": {
        "recipient": "示例收件人",
        "phone": "+86 10000000000",
        "countryOrRegion": "中国",
        "region": "示例省",
        "city": "示例城市",
        "addressLine": "示例展示路 100 号（非真实地址）"
      },
      "remark": "只读界面样例，不产生交易或物流。",
      "items": [
        {
          "productId": "d84473a5-10f5-5dad-b124-74a521a1f0f1",
          "sku": "WM-TEAM-BOARD",
          "name": "伙伴协作板 · 示例",
          "unitPriceFen": 28900,
          "quantity": 1,
          "subtotalFen": 28900,
          "valid": true,
          "reason": null,
          "priceChanged": false,
          "previousUnitPriceFen": 28900
        },
        {
          "productId": "ada916b9-cd61-5de8-89d0-1668f9869376",
          "sku": "WM-FOREST-KIT",
          "name": "森林探索套装 · 示例",
          "unitPriceFen": 21900,
          "quantity": 1,
          "subtotalFen": 21900,
          "valid": true,
          "reason": null,
          "priceChanged": false,
          "previousUnitPriceFen": 21900
        }
      ],
      "subtotalFen": 50800,
      "shippingFen": 0,
      "taxFen": 0,
      "discountFen": 0,
      "allowedActions": [],
      "logisticsName": "示例模拟物流",
      "trackingNumber": "SAMPLE-TRACK-002",
      "paymentAttempts": [
        {
          "id": "9e253ad5-12a2-5c4a-8c10-f7f58f158cd6",
          "outcome": "SUCCESS",
          "amountFen": 50800,
          "simulationReference": "SIM-PAY-SAMPLE-002"
        }
      ],
      "refunds": [],
      "history": [
        {
          "action": "CREATE",
          "fromStatus": null,
          "toStatus": "PENDING_PAYMENT",
          "version": 1,
          "reason": "示例订单创建",
          "createdAt": "2026-09-03T02:00:00Z"
        },
        {
          "action": "MOCK_PAYMENT",
          "fromStatus": "PENDING_PAYMENT",
          "toStatus": "PAID",
          "version": 2,
          "reason": "示例模拟付款成功",
          "createdAt": "2026-09-03T02:10:00Z"
        },
        {
          "action": "MOCK_SHIPMENT",
          "fromStatus": "PAID",
          "toStatus": "SHIPPED",
          "version": 3,
          "reason": "示例模拟整单发货",
          "createdAt": "2026-09-03T06:00:00Z"
        },
        {
          "action": "CONFIRM_RECEIPT",
          "fromStatus": "SHIPPED",
          "toStatus": "COMPLETED",
          "version": 4,
          "reason": "示例确认收货",
          "createdAt": "2026-09-05T08:00:00Z"
        }
      ]
    },
    {
      "id": "c707b880-c841-5303-9dda-8dd5976d3253",
      "orderNumber": "WM-SAMPLE-20260904-004",
      "status": "PAID",
      "version": 2,
      "totalFen": 32900,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-04T02:00:00Z",
      "shippingAddress": {
        "recipient": "示例收件人",
        "phone": "+86 10000000000",
        "countryOrRegion": "中国",
        "region": "示例省",
        "city": "示例城市",
        "addressLine": "示例展示路 100 号（非真实地址）"
      },
      "remark": "只读界面样例，不产生交易或物流。",
      "items": [
        {
          "productId": "f793492f-b8c1-5703-a4d6-ea9ace37c8e8",
          "sku": "WM-RAINBOW-ARCH",
          "name": "彩虹拱桥 · 示例",
          "unitPriceFen": 32900,
          "quantity": 1,
          "subtotalFen": 32900,
          "valid": true,
          "reason": null,
          "priceChanged": false,
          "previousUnitPriceFen": 32900
        }
      ],
      "subtotalFen": 32900,
      "shippingFen": 0,
      "taxFen": 0,
      "discountFen": 0,
      "allowedActions": [
        "CANCEL"
      ],
      "logisticsName": null,
      "trackingNumber": null,
      "paymentAttempts": [
        {
          "id": "1cc18d4e-9a4d-5ec8-b12b-f653b0683573",
          "outcome": "SUCCESS",
          "amountFen": 32900,
          "simulationReference": "SIM-PAY-SAMPLE-001"
        }
      ],
      "refunds": [],
      "history": [
        {
          "action": "CREATE",
          "fromStatus": null,
          "toStatus": "PENDING_PAYMENT",
          "version": 1,
          "reason": "示例订单创建",
          "createdAt": "2026-09-04T02:00:00Z"
        },
        {
          "action": "MOCK_PAYMENT",
          "fromStatus": "PENDING_PAYMENT",
          "toStatus": "PAID",
          "version": 2,
          "reason": "示例模拟付款成功",
          "createdAt": "2026-09-04T02:10:00Z"
        }
      ]
    }
  ],
  "orderSummaries": [
    {
      "id": "fafbf3d3-db7f-5a18-bd98-68f59ccafaa3",
      "orderNumber": "WM-SAMPLE-20260905-001",
      "status": "PENDING_PAYMENT",
      "version": 1,
      "totalFen": 59700,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-05T02:00:00Z"
    },
    {
      "id": "0caf56da-db21-5081-8bf6-baa006fa4789",
      "orderNumber": "WM-SAMPLE-20260904-002",
      "status": "SHIPPED",
      "version": 3,
      "totalFen": 32900,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-04T02:00:00Z"
    },
    {
      "id": "6d0182f8-6102-5829-b9d4-18f765d61248",
      "orderNumber": "WM-SAMPLE-20260903-003",
      "status": "COMPLETED",
      "version": 4,
      "totalFen": 50800,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-03T02:00:00Z"
    },
    {
      "id": "c707b880-c841-5303-9dda-8dd5976d3253",
      "orderNumber": "WM-SAMPLE-20260904-004",
      "status": "PAID",
      "version": 2,
      "totalFen": 32900,
      "currency": "CNY",
      "mode": "SIMULATED",
      "createdAt": "2026-09-04T02:00:00Z"
    }
  ],
  "adminOrderAllowedActions": {
    "fafbf3d3-db7f-5a18-bd98-68f59ccafaa3": [
      "CANCEL"
    ],
    "0caf56da-db21-5081-8bf6-baa006fa4789": [],
    "6d0182f8-6102-5829-b9d4-18f765d61248": [],
    "c707b880-c841-5303-9dda-8dd5976d3253": [
      "CANCEL",
      "MOCK_SHIPMENT"
    ]
  },
  "cart": {
    "cartVersion": 3,
    "items": [
      {
        "productId": "469ec43c-559a-5040-8015-614ed58f12a6",
        "sku": "WM-BALANCE-STONES",
        "name": "平衡石 · 示例",
        "unitPriceFen": 25900,
        "quantity": 1,
        "subtotalFen": 25900,
        "valid": true,
        "reason": null,
        "priceChanged": false,
        "previousUnitPriceFen": 25900
      },
      {
        "productId": "9b07436c-613e-53ba-b12e-f552254283fe",
        "sku": "WM-RING-TOSS",
        "name": "环环投掷 · 示例",
        "unitPriceFen": 16900,
        "quantity": 2,
        "subtotalFen": 33800,
        "valid": true,
        "reason": null,
        "priceChanged": false,
        "previousUnitPriceFen": 16900
      }
    ],
    "totalFen": 59700,
    "canCheckout": true,
    "currency": "CNY"
  },
  "checkoutPreview": {
    "previewToken": "preview-only-not-a-server-token",
    "cartVersion": 3,
    "expiresAt": "2026-09-06T10:15:00Z",
    "currency": "CNY",
    "items": [
      {
        "productId": "469ec43c-559a-5040-8015-614ed58f12a6",
        "sku": "WM-BALANCE-STONES",
        "name": "平衡石 · 示例",
        "unitPriceFen": 25900,
        "quantity": 1,
        "subtotalFen": 25900,
        "valid": true,
        "reason": null,
        "priceChanged": false,
        "previousUnitPriceFen": 25900
      },
      {
        "productId": "9b07436c-613e-53ba-b12e-f552254283fe",
        "sku": "WM-RING-TOSS",
        "name": "环环投掷 · 示例",
        "unitPriceFen": 16900,
        "quantity": 2,
        "subtotalFen": 33800,
        "valid": true,
        "reason": null,
        "priceChanged": false,
        "previousUnitPriceFen": 16900
      }
    ],
    "subtotalFen": 59700,
    "shippingFen": 0,
    "taxFen": 0,
    "discountFen": 0,
    "totalFen": 59700
  },
  "registrationPolicy": {
    "adultStatement": "我已年满 18 周岁，以本人身份使用本网站。",
    "termsVersion": "preview-2026-09",
    "termsPath": "/terms",
    "privacyVersion": "preview-2026-09",
    "privacyPath": "/privacy"
  }
}
