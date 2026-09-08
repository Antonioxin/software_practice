// Only imported through the development-only, read-only preview adapter.
import { fixtures } from './fixtures'
import { ApiProblem } from '../services/http'
import type { ApiEnvelope } from '../types'

const createdAt = '2026-09-08T02:00:00Z'
const common = { version: 1, createdAt, updatedAt: createdAt, publishedAt: createdAt }
const imageIds = ['e1000000-0000-4000-8000-000000000001', 'e1000000-0000-4000-8000-000000000002']
export const contentFixtures = {
  articles: [
    {
      ...common, id: 'e2000000-0000-4000-8000-000000000001', title: '把客厅变成一座小小冒险岛', summary: '几块平衡石，一点想象力，就能开启今天的探索。', pageDescription: '', category: '居家运动',
      body: '<p>为游戏留出一块平整、没有障碍的空间，和孩子一起摆出自己的路线。</p><section data-layout="image-left"><figure><img src="/assets/products/guides/balance-stones-guide.png" alt="卡通孩子在成人陪伴下走过彩色实物平衡石"><figcaption>从一条短短的平衡石路线开始，给每一步留出尝试的时间。</figcaption></figure><div><h3>今天，一起跨过小河</h3><ol><li>把平衡石分开放置，从较近的间距开始。</li><li>轮流走过路线，尝试停下来保持平衡。</li><li>一起给每一站起个名字，让故事继续。</li></ol></div></section><section data-layout="image-right"><figure><img src="/assets/products/guides/rainbow-arch-guide.png" alt="卡通孩子用彩色实物彩虹拱积木搭出新的造型"><figcaption>换一种颜色，换一个故事，让想象继续。</figcaption></figure><div><h3>把下一站交给想象</h3><p>给路线终点起一个名字，也可以围坐下来，用彩虹拱搭出故事里的小房子。</p><p>由成人陪同，结合产品说明和孩子的实际能力调整难度。</p></div></section>',
      productIds: [fixtures.products[0]!.id], mediaIds: [], status: 'PUBLISHED', sortOrder: 1,
    },
    {
      ...common, id: 'e2000000-0000-4000-8000-000000000002', title: '周末，带着套圈去公园', summary: '不急着计分。和朋友一起，把每一次尝试都变成快乐。', pageDescription: '', category: '户外探索',
      body: '<p>选一块平坦、开阔的场地，把套圈目标依次摆好。</p><section data-layout="image-left"><figure><img src="/assets/products/guides/ring-toss-guide.png" alt="卡通孩子向实物木质套圈架投掷绳圈"><figcaption>先找到适合自己的距离，再慢慢尝试。</figcaption></figure><div><h3>让每个人都能参与</h3><ul><li>按参与者的能力调整投掷距离。</li><li>投掷前确认前方没有人。</li><li>轮流投掷，收拾器材时也一起合作。</li></ul></div></section><h2>休息时，也能发现小小的快乐</h2><p>收好套圈，和孩子一起观察脚边落叶的颜色与纹理。活动由成人陪同，场地与器材使用要求以对应说明为准。</p><figure><img src="/assets/products/guides/forest-kit-guide.png" alt="卡通亲子用实物放大镜观察落叶"><figcaption>把目光放低一点，散步的路上也有值得停留的小发现。</figcaption></figure>',
      productIds: [], mediaIds: [imageIds[1]!], status: 'PUBLISHED', sortOrder: 2,
    },
    { ...common, publishedAt: null, id: 'e2000000-0000-4000-8000-000000000003', title: '雨天的五分钟运动计划', summary: '一篇正在整理的玩法手记，等待下一次见面。', pageDescription: '', category: '居家运动', body: '<p>这篇内容仍在编辑中。</p>', productIds: [], mediaIds: [], status: 'DRAFT', sortOrder: 3 },
    { ...common, id: 'e2000000-0000-4000-8000-000000000004', title: '往期活动：夏日一起玩', summary: '已结束的夏日活动，保留后台记录。', pageDescription: '', category: '活动回顾', body: '<p>往期活动内容。</p>', productIds: [], mediaIds: [], status: 'OFFLINE', sortOrder: 4 },
  ],
  faqs: [
    ['如何选择适合的运动器材？', '先查看商品标注的适用年龄、使用场景和安全说明，再结合参与者的能力选择。', '选购指南'],
    ['平衡石可以在室内使用吗？', '可以先检查地面是否平整、防滑，并为活动留出足够空间。具体要求以产品说明为准。', '使用方法'],
    ['在哪里查找产品说明书？', '进入下载中心，按资料类型或适用商品筛选，即可查看当前版本的说明资料。', '资料下载'],
    ['产品使用后如何收纳？', '清洁并晾干后放在通风、干燥的地方，避免暴晒；具体清洁方法请查看对应说明。', '保养收纳'],
    ['经销商在哪里查看合作资料？', '使用合作企业对应的账户登录，资格有效时可在下载中心查看经销商资料。', '经销合作'],
  ].map(([question, answer, category], index) => ({ ...common, id: `e3000000-0000-4000-8000-00000000000${index + 1}`, question, answer, category, productIds: index === 1 ? [fixtures.products[0]!.id] : [], status: 'PUBLISHED', sortOrder: index + 1 })),
  banners: [
    { ...common, id: 'e4000000-0000-4000-8000-000000000001', title: '小小的平衡，大大的发现', imageId: imageIds[0]!, buttonText: '找一点玩法灵感', targetUrl: '/articles', sortOrder: 1, status: 'PUBLISHED' },
    { ...common, id: 'e4000000-0000-4000-8000-000000000002', title: '这个周末，一起去外面玩', imageId: imageIds[1]!, buttonText: '探索全部商品', targetUrl: '/products', sortOrder: 2, status: 'DRAFT' },
  ],
  settings: { brandName: 'WEMOVE', brandDescription: '把运动变成日常的小乐趣。\n我们相信，运动可以从一段短短的陪伴开始。一起探索、尝试、合作，让每一天都留一点时间给好玩。', contactEmail: 'hello@example.invalid', contactPhone: '', contactAddress: '线上课程演示站点', logoMediaId: null, termsText: 'WEMOVE 为软件开发实践课程演示系统。\n商品、支付、物流和合作信息用于教学演示。使用器材前请阅读对应说明，由成人陪同儿童活动。', termsVersion: '2026-09-05', privacyText: '此演示系统使用账户资料支持登录与业务演示。\n请勿在演示环境填写真实敏感资料；账户与合作资料按身份权限显示。', privacyVersion: '2026-09-05', version: 1 },
  home: { recommendedProductIds: fixtures.products.slice(0, 3).map(product => product.id), featuredArticleIds: ['e2000000-0000-4000-8000-000000000001', 'e2000000-0000-4000-8000-000000000002'], version: 1 },
  media: imageIds.map((id, index) => ({ id, url: `/assets/products/${index === 0 ? 'balance-stones' : 'ring-toss'}.png`, altText: index === 0 ? '平衡石玩法示意' : '套圈玩法示意', mimeType: 'image/png', width: 1024, height: 1024, filename: index === 0 ? 'balance-stones.png' : 'ring-toss.png', sizeBytes: 286720, version: 1, referenceCount: 2, publicReferenceCount: index === 0 ? 2 : 1, createdAt })),
  files: ['PUBLIC', 'DEALER', 'INTERNAL'].map((visibility, index) => {
    const id = `e5000000-0000-4000-8000-00000000000${index + 1}`
    const downloadId = `e6000000-0000-4000-8000-00000000000${index + 1}`
    return { ...common, id, title: ['平衡石 · 玩法与使用指南', '经销合作 · 产品资料册', '内部 · 内容发布检查记录'][index], type: ['使用说明', '产品目录', '内部资料'][index], versionNote: '2026.09 · 初版资料', productIds: index === 0 ? [fixtures.products[0]!.id] : [], visibility, status: 'PUBLISHED', downloadId, downloadUrl: `/api/v1/files/${id}/versions/${downloadId}/content`, filename: ['balance-guide.pdf', 'dealer-catalog.pdf', 'content-checklist.pdf'][index], mimeType: 'application/pdf', sizeBytes: 128 * 1024 * (index + 1) }
  }),
}

export function contentPreview<T>(endpoint: string, query: URLSearchParams, context: { role: string; state: string }): ApiEnvelope<T> | undefined {
  const admin = endpoint.startsWith('/admin/')
  const path = admin ? endpoint.slice(6) : endpoint
  if (!['/home', '/site-settings', '/articles', '/faqs', '/banners', '/files', '/media'].some(prefix => path === prefix || path.startsWith(`${prefix}/`))) return undefined
  const error = (status: number, detail: string): never => { throw new ApiProblem({ type: 'about:blank', title: 'Development preview', status, code: 'PREVIEW_RECORD_MISSING', detail }) }
  if (context.state === 'error') error(503, '示例：内容服务暂时不可用，请重试或切换为正常状态。')
  const empty = context.state === 'empty'
  const envelope = (data: unknown) => ({ data: structuredClone(data) as T })
  if (path === '/site-settings') return envelope(contentFixtures.settings)
  if (path === '/home') return envelope(admin ? contentFixtures.home : {
    banners: empty ? [] : contentFixtures.banners.filter(item => item.status === 'PUBLISHED'),
    featuredArticles: empty ? [] : contentFixtures.articles.filter(item => item.status === 'PUBLISHED'),
    recommendedProducts: empty ? [] : fixtures.products.slice(0, 3).map(({ id, name, sku, mainImageId, retailUnitPriceFen, inStock }) => ({ id, name, sku, mainImageId, retailUnitPriceFen, inStock, published: true })),
  })
  let items: Array<{ id: string; status?: string; visibility?: string; title?: string; question?: string; category?: string; productIds?: string[]; type?: string }> =
    path.startsWith('/articles') ? contentFixtures.articles : path.startsWith('/faqs') ? contentFixtures.faqs : path.startsWith('/banners') ? contentFixtures.banners : path.startsWith('/media') ? contentFixtures.media : contentFixtures.files
  items = items.filter(item => admin || (item.status === 'PUBLISHED' && (item.visibility === undefined || item.visibility === 'PUBLIC' || context.role === 'admin')))
  if (empty) items = []
  const id = path.split('/')[2]
  if (id) return envelope(items.find(item => item.id === id) ?? error(404, '此内容不存在或尚未公开。'))
  const keyword = query.get('keyword')?.toLocaleLowerCase()
  items = items.filter(item => (!keyword || `${item.title ?? ''} ${item.question ?? ''}`.toLocaleLowerCase().includes(keyword))
    && (!query.get('status') || item.status === query.get('status'))
    && (!query.get('category') || item.category === query.get('category'))
    && (!query.get('type') || item.type === query.get('type'))
    && (!query.get('visibility') || item.visibility === query.get('visibility'))
    && (!query.get('productId') || item.productIds?.includes(query.get('productId')!)))
  const page = Math.max(1, Number(query.get('page')) || 1)
  const pageSize = Math.min(50, Math.max(1, Number(query.get('pageSize')) || 12))
  return { ...envelope(items.slice((page - 1) * pageSize, page * pageSize)), meta: { page, pageSize, totalItems: items.length, totalPages: Math.ceil(items.length / pageSize) } }
}
