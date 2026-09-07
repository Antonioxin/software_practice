export interface ProductUsageGuide {
  title: string
  intro: string
  imageSrc: string
  imageAlt: string
  steps: readonly { title: string; body: string }[]
}

// Editorial play ideas for the six illustrated concept products. Keep these
// separate from API instructions, specifications and development fixtures.
const guides = new Map<string, ProductUsageGuide>([
  ['WM-BALANCE-STONES', {
    title: '铺一条属于自己的小路',
    intro: '从近一点、慢一点开始，在一步一步的移动中感受平衡。',
    imageSrc: '/assets/products/guides/balance-stones-guide.png',
    imageAlt: '玩法示意：孩子在成人陪伴下，张开双臂沿低矮的彩色平衡石慢慢行走。',
    steps: [
      { title: '把小路摆好', body: '在平坦、防滑的地面上分开放好平衡石，先排成容易跨到的短直线。逐块检查是否平稳，周围留出走动空间。' },
      { title: '一步一步走过去', body: '双臂自然展开，一只脚踩稳后，再迈向下一块。初次尝试时请成人在身旁陪伴，按自己的节奏慢慢走。' },
      { title: '换一种路线', body: '熟悉以后，把直线改成小弯道，或按颜色依次经过。不必追求速度，每次只改变一点难度。' },
    ],
  }],
  ['WM-RAINBOW-ARCH', {
    title: '把彩虹拆开，再重新想象',
    intro: '认识大小、组合形状，用双手搭出自己的小小世界。',
    imageSrc: '/assets/products/guides/rainbow-arch-guide.png',
    imageAlt: '玩法示意：孩子坐在地面上，用手把大小不同的彩色木拱重新排列成小桥。',
    steps: [
      { title: '排一排大小', body: '坐在平稳的地垫或桌边，把拱形积木轻轻拆开。观察每一块的大小与颜色，试着从大到小排成一列。' },
      { title: '搭起一座小桥', body: '让拱形的两端平稳落地，试着套回彩虹，或把几座小桥排在一起。一次摆一块，用手轻扶，确认放稳后再松开。' },
      { title: '讲一个自己的故事', body: '把小桥想象成山洞、房子或隧道，和伙伴轮流接着搭。结束后按大小嵌套收好；积木用于手部搭建，不作为踩踏器材。' },
    ],
  }],
  ['WM-RING-TOSS', {
    title: '瞄准，再轻轻送出去',
    intro: '从容易命中的距离开始，把每一次投掷当作一次小练习。',
    imageSrc: '/assets/products/guides/ring-toss-guide.png',
    imageAlt: '玩法示意：孩子站在投掷架前，从身体下方向前轻抛绳环，虚线表示绳环朝立柱飞行的方向。',
    steps: [
      { title: '放好目标，约定站位', body: '把投掷架平放在开阔地面，检查立柱是否稳固。从较近的位置开始，约定同一条起投线，并让伙伴站在投掷方向之外。' },
      { title: '下手轻抛', body: '目光看向一根立柱，手握绳环，从身体下方向前轻轻摆臂，把环送向目标。先感受方向，再慢慢调整力度。' },
      { title: '轮流挑战', body: '投完一轮后再一起捡回绳环，数一数命中了几个。熟悉后稍微后退，或轮流选择不同立柱作为目标。' },
    ],
  }],
  ['WM-TEAM-BOARD', {
    title: '把两个人的脚步合在一起',
    intro: '先商量好，再一起迈步。比起走得快，更有趣的是找到默契。',
    imageSrc: '/assets/products/guides/team-board-guide.png',
    imageAlt: '玩法示意：两名孩子前后站在同一对协作板上，各自双脚分踩两块板，成人在旁引导他们同步迈步。',
    steps: [
      { title: '站稳，再准备出发', body: '将两块协作板平行放好，在成人协助下前后站到踏位上，每人双脚分别踩住一块板。先保持不动，确认两个人都站稳。' },
      { title: '约定同一个口令', body: '一起说“左、右”，按口令让同一侧的脚配合木板小步移动。先试一两步，有人跟不上时就一起停下，重新找节奏。' },
      { title: '一起走到终点', body: '在前方选一个很近的终点，慢慢配合着前进。到达后停稳，再依次下板；保持成人陪同，不进行追逐或竞速。' },
    ],
  }],
  ['WM-FOREST-KIT', {
    title: '把散步变成一次小发现',
    intro: '一片落叶、一条纹路，都可以成为今天的观察主题。',
    imageSrc: '/assets/products/guides/forest-kit-guide.png',
    imageAlt: '玩法示意：孩子与成人蹲下观察一片落叶，孩子用木柄放大镜看叶脉，旁边放着收纳袋、记录本和指南针。',
    steps: [
      { title: '选好今天的观察点', body: '和成人一起选择熟悉的步道或草地边，带上收纳袋和记录本。先约定活动范围，再找一片落叶或一处树皮纹理。' },
      { title: '靠近一点，仔细看', body: '把放大镜对着观察物，慢慢调整距离，看看颜色、边缘与纹路有什么不同。放大镜不要朝向太阳，也不要触碰不认识的植物或小动物。' },
      { title: '画下来，分享发现', body: '用简单的线条记录形状，和伙伴说说自己的发现。把自然物留在原处，清点工具、收进袋子，再出发去下一个观察点。' },
    ],
  }],
  ['WM-SKIP-ROPE', {
    title: '找到轻轻落地的节奏',
    intro: '先会摇绳，再连起来跳。不用一开始就追求很多次。',
    imageSrc: '/assets/products/guides/skip-rope-guide.png',
    imageAlt: '玩法示意：孩子双手各握一只跳绳手柄，膝盖微弯、双脚轻轻离地，绳子从脚下通过。',
    steps: [
      { title: '整理好绳子和空间', body: '请成人协助确认绳长适合自己，检查手柄与绳体连接。穿好运动鞋，在平坦、开阔的地面开始，前后和两侧留出摇绳空间。' },
      { title: '摇一下，跨过去', body: '双手握住手柄，把绳子放在脚后。用手腕向前摇到脚前，先停下来跨过去，重复几次，熟悉绳子经过身体的顺序。' },
      { title: '连成轻轻的小跳', body: '让摇绳与双脚起跳连起来，膝盖微弯，脚只需轻轻离地。先完成少量连跳，绊绳就停下来整理，累了随时休息。' },
    ],
  }],
])

export function productUsageGuide(sku: string): ProductUsageGuide | null {
  return guides.get(sku) ?? null
}
