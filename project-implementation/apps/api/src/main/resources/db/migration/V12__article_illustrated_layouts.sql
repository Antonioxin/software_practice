-- Add product-guide illustrations only to untouched V10 seed articles.
-- Binary body comparison avoids collation-equivalent edits being overwritten.
-- Titles, publication state, summaries and associations remain as stored.
UPDATE content_entries
SET body = '<p>一次小小的客厅探险，可以从十分钟的共同游戏开始。给路线起个名字，也给每一次尝试留一点从容。</p><section data-layout="image-left"><figure><img src="/assets/products/guides/balance-stones-guide.png" alt="卡通孩子在成人陪伴下走过彩色实物平衡石"><figcaption>把平衡石连成一条小路，由大人在旁陪伴。</figcaption></figure><div><h3>从一条安全的小路开始</h3><p>在平整、不打滑的地面留出活动空间，移开尖锐和易碎物品。由大人示范，再邀请孩子自己选择起点和终点。</p></div></section><section data-layout="image-right"><figure><img src="/assets/products/guides/rainbow-arch-guide.png" alt="卡通孩子正在摆放彩色实物彩虹拱积木"><figcaption>让颜色和排列成为想象的线索，具体玩法以产品说明为准。</figcaption></figure><div><h3>让规则跟着孩子成长</h3><p>走完小路，也可以坐下来摆一摆彩虹拱，把颜色口令变成搭建小桥的灵感。</p><p>可以从慢慢走、停下来和绕过标记开始。熟悉后，再加入颜色口令或轮流带路。每次只增加一个小挑战，并允许孩子随时休息。</p></div></section><h2>陪伴比闯关更重要</h2><p>关注孩子如何思考、尝试和表达。活动难度应结合孩子的年龄、能力和具体产品说明调整，全程由成人看护。</p><figure><img src="/assets/products/guides/team-board-guide.png" alt="两位卡通孩子在成人陪伴下体验实物协作板"><figcaption>一起商量、一起尝试，让陪伴成为游戏的一部分。</figcaption></figure>',
    version = version + 1,
    updated_at = UTC_TIMESTAMP(6)
WHERE id = UNHEX('e1000000000040008000000000000001')
  AND kind = 'ARTICLE'
  AND version = 0
  AND CAST(body AS BINARY) = CAST('<h2>从一条安全的小路开始</h2><p>在平整、不打滑的地面留出活动空间，移开尖锐和易碎物品。由大人示范，再邀请孩子自己选择起点和终点。</p><h2>让规则跟着孩子成长</h2><p>可以从慢慢走、停下来和绕过标记开始。熟悉后，再加入颜色口令或轮流带路。每次只增加一个小挑战，并允许孩子随时休息。</p><h2>陪伴比闯关更重要</h2><p>关注孩子如何思考、尝试和表达。活动难度应结合孩子的年龄、能力和具体产品说明调整，全程由成人看护。</p>' AS BINARY);

UPDATE content_entries
SET body = '<section data-layout="image-left"><figure><img src="/assets/products/guides/balance-stones-guide.png" alt="卡通孩子在成人看护下探索彩色实物平衡石路线"><figcaption>平衡石路线示意；具体间距与难度需结合产品说明和孩子能力确认。</figcaption></figure><div><h3>先熟悉，再组合</h3><p>本文为待审核草稿。发布前需要补充适龄条件、所需器材和成人看护要点。</p></div></section>',
    version = version + 1,
    updated_at = UTC_TIMESTAMP(6)
WHERE id = UNHEX('e1000000000040008000000000000002')
  AND kind = 'ARTICLE'
  AND version = 0
  AND CAST(body AS BINARY) = CAST('<h2>先熟悉，再组合</h2><p>本文为待审核草稿。发布前需要补充适龄条件、所需器材和成人看护要点。</p>' AS BINARY);

UPDATE content_entries
SET body = '<p>该季节活动已经结束，文章已下线。重新发布前请核对活动信息和适用条件。</p><figure><img src="/assets/products/guides/forest-kit-guide.png" alt="卡通亲子使用实物探索工具观察落叶"><figcaption>往期户外探索配图，保留供后续活动编辑参考。</figcaption></figure>',
    version = version + 1,
    updated_at = UTC_TIMESTAMP(6)
WHERE id = UNHEX('e1000000000040008000000000000003')
  AND kind = 'ARTICLE'
  AND version = 0
  AND CAST(body AS BINARY) = CAST('<p>该季节活动已经结束，文章已下线。重新发布前请核对活动信息和适用条件。</p>' AS BINARY);
