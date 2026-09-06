# 前端运行视觉资源

这里保存页面实际加载的手绘图标与字体；原始素材继续保留在[前端设计视觉参考](../../../../前端设计视觉参考/)中。运行资源不使用外部字体 CDN，也不覆盖用户提供的原件。

## 手绘图标

来源为[视觉组件](../../../../前端设计视觉参考/视觉组件/)中的 27 张 PNG。每张原图为 1254 × 1254，具有真实 Alpha 透明通道、透明四角和内部留白。本轮按完整画布等比缩至 128 × 128，再保存为无损 WebP；没有裁切、换色、重绘或修改图标的笔画构成。这里“无损”指 WebP 对缩放后像素的编码方式，缩小分辨率本身仍会减少图像细节。

原 PNG 合计 **6,819,093 字节**，运行 WebP 合计 **102,960 字节**，体积减少约 **98.5%**。128 像素资源可用于 24、32、48 像素的常见图标展示，并为高像素密度屏幕留出余量。不要把这些缩略图当成大幅插画使用。

| 运行文件 | 原文件 | WebP 字节 |
| --- | --- | ---: |
| [sketch/home.webp](sketch/home.webp) | [原图：手绘小房子.png](../../../../前端设计视觉参考/视觉组件/手绘小房子.png) | 4,088 |
| [sketch/grid.webp](sketch/grid.webp) | [原图：手绘四宫格.png](../../../../前端设计视觉参考/视觉组件/手绘四宫格.png) | 6,284 |
| [sketch/menu.webp](sketch/menu.webp) | [原图：手绘三条横线.png](../../../../前端设计视觉参考/视觉组件/手绘三条横线.png) | 2,942 |
| [sketch/search.webp](sketch/search.webp) | [原图：手绘放大镜.png](../../../../前端设计视觉参考/视觉组件/手绘放大镜.png) | 3,332 |
| [sketch/filter.webp](sketch/filter.webp) | [原图：手绘调节滑杆.png](../../../../前端设计视觉参考/视觉组件/手绘调节滑杆.png) | 3,854 |
| [sketch/sort.webp](sketch/sort.webp) | [原图：手绘上下箭头.png](../../../../前端设计视觉参考/视觉组件/手绘上下箭头.png) | 2,868 |
| [sketch/user.webp](sketch/user.webp) | [原图：手绘人物轮廓.png](../../../../前端设计视觉参考/视觉组件/手绘人物轮廓.png) | 4,276 |
| [sketch/heart.webp](sketch/heart.webp) | [原图：手绘爱心.png](../../../../前端设计视觉参考/视觉组件/手绘爱心.png) | 3,546 |
| [sketch/history.webp](sketch/history.webp) | [原图：手绘时钟.png](../../../../前端设计视觉参考/视觉组件/手绘时钟.png) | 4,530 |
| [sketch/cart.webp](sketch/cart.webp) | [原图：手绘购物车.png](../../../../前端设计视觉参考/视觉组件/手绘购物车.png) | 5,104 |
| [sketch/plus.webp](sketch/plus.webp) | [原图：手绘加号.png](../../../../前端设计视觉参考/视觉组件/手绘加号.png) | 2,116 |
| [sketch/minus.webp](sketch/minus.webp) | [原图：手绘减号.png](../../../../前端设计视觉参考/视觉组件/手绘减号.png) | 1,182 |
| [sketch/trash.webp](sketch/trash.webp) | [原图：手绘垃圾桶.png](../../../../前端设计视觉参考/视觉组件/手绘垃圾桶.png) | 5,548 |
| [sketch/orders.webp](sketch/orders.webp) | [原图：手绘清单.png](../../../../前端设计视觉参考/视觉组件/手绘清单.png) | 5,318 |
| [sketch/wallet.webp](sketch/wallet.webp) | [原图：手绘钱包.png](../../../../前端设计视觉参考/视觉组件/手绘钱包.png) | 4,524 |
| [sketch/package.webp](sketch/package.webp) | [原图：手绘包裹.png](../../../../前端设计视觉参考/视觉组件/手绘包裹.png) | 5,128 |
| [sketch/truck.webp](sketch/truck.webp) | [原图：手绘货车.png](../../../../前端设计视觉参考/视觉组件/手绘货车.png) | 4,750 |
| [sketch/chat.webp](sketch/chat.webp) | [原图：手绘对话气泡.png](../../../../前端设计视觉参考/视觉组件/手绘对话气泡.png) | 3,566 |
| [sketch/return.webp](sketch/return.webp) | [原图：手绘回转箭头.png](../../../../前端设计视觉参考/视觉组件/手绘回转箭头.png) | 3,232 |
| [sketch/help.webp](sketch/help.webp) | [原图：手绘问号.png](../../../../前端设计视觉参考/视觉组件/手绘问号.png) | 2,602 |
| [sketch/ticket.webp](sketch/ticket.webp) | [原图：手绘票券.png](../../../../前端设计视觉参考/视觉组件/手绘票券.png) | 3,120 |
| [sketch/gift.webp](sketch/gift.webp) | [原图：手绘礼盒.png](../../../../前端设计视觉参考/视觉组件/手绘礼盒.png) | 5,256 |
| [sketch/timer.webp](sketch/timer.webp) | [原图：手绘秒表.png](../../../../前端设计视觉参考/视觉组件/手绘秒表.png) | 4,628 |
| [sketch/close.webp](sketch/close.webp) | [原图：手绘叉号.png](../../../../前端设计视觉参考/视觉组件/手绘叉号.png) | 3,008 |
| [sketch/arrow-left.webp](sketch/arrow-left.webp) | [原图：手绘左箭头.png](../../../../前端设计视觉参考/视觉组件/手绘左箭头.png) | 2,020 |
| [sketch/chevron-down.webp](sketch/chevron-down.webp) | [原图：手绘下箭头.png](../../../../前端设计视觉参考/视觉组件/手绘下箭头.png) | 2,180 |
| [sketch/share.webp](sketch/share.webp) | [原图：手绘分享符号.png](../../../../前端设计视觉参考/视觉组件/手绘分享符号.png) | 3,958 |

页面通过 [SketchIcon.vue](../../src/components/SketchIcon.vue) 使用英文资源名，例如 `<SketchIcon name="cart" />`。图标旁边已有文字时保持装饰语义；独立表达含义时传入 `label`，图标按钮本身仍需提供清晰的可访问名称。

## 字体组合与许可证

选用用户提供的三种字体家族，按用途分配，避免同一段内容频繁混用字形：

| 字体 | 字重与运行文件 | 主要用途 | 字节 |
| --- | --- | --- | ---: |
| Barlow Condensed | 400：[barlow-condensed-regular.woff2](fonts/barlow-condensed-regular.woff2) | 备用常规字重 | 35,956 |
| Barlow Condensed | 600：[barlow-condensed.woff2](fonts/barlow-condensed.woff2) | 品牌字标、英文标题与强调 | 37,696 |
| Caveat | 400：[caveat.woff2](fonts/caveat.woff2) | 首页英文主题语及少量手写注记 | 102,760 |

中文运行分块见 [yozai/](fonts/yozai/) 和[清单](fonts/yozai/manifest.json)。Regular / Medium 的 core 分别为 **164,872 / 162,608 字节**，合计约 **327 KB**；两个字重共 94 个分块，保留源字体在约定 CJK 范围内的 **24,118 个码点**。全部分块合计 16,227,504 字节，仅按页面文字需要下载，不能将完整目录大小当成首屏下载量。

以上两种英文字体原件来自[字体参考目录](../../../../前端设计视觉参考/fonts/)，三个 TTF 原件合计 457,956 字节；完整转换的 WOFF2 合计 **176,412 字节**，没有裁剪字形集合。转换使用本地已有的 fontTools 4.51.0 和 Brotli 1.2.0，转换后重新读取确认字重与 Unicode 字符映射。

中文改用用户提供的[悠哉 0.868](../../../../前端设计视觉参考/fonts/中文字体/yozai-font-0.868/)，覆盖正文、表单、导航、标题及弹窗。运行版本采用 Regular 400 / Medium 500；较粗的中文样式匹配 Medium，不合成虚假粗体。中文字体声明限定在中日韩字符与标点范围，因此英文品牌、主题语以及正文的拉丁字母继续使用原有字体。

为避免首屏加载两份完整中文字库，运行字体按当前界面字符和剩余码点分块。当前源码常用字放在 core 文件中，其余字形仍保留在每块 512 个码点的补充分块；浏览器通过 `unicode-range` 自动请求当前文本所需的文件。这样能够显示字体本身支持的后端动态中文，而不局限于当前页面文案。`font-display: swap` 允许下载期间先显示系统字形。

由于分块是字体子集，内部家族名和 CSS 名称使用 **Wemove Hand**，以遵守原字体保留名称的要求；字形仍来自悠哉，不改变笔画形状。原始版权和 OFL 随分块保留。

原始许可文本随资源保存：

- [Barlow Condensed — SIL Open Font License](fonts/Barlow_Condensed-OFL.txt)
- [Caveat — SIL Open Font License](fonts/Caveat-OFL.txt)
- [Yozai 来源及 Wemove Hand 子集 — SIL Open Font License](fonts/yozai/Yozai-OFL.txt)

图标压缩参数、大小、Alpha 和字体映射已经检查，整套缩图经过联系表视觉复核；这些素材检查不替代应用构建、页面交互或真实 API 验收。页面覆盖与开发预览说明见[静态界面与开发预览](../../../../docs/modules/content/静态界面与开发预览.md)。

### 重新生成中文字体分块

使用[字体准备脚本](../../../../scripts/assets/prepare-yozai.py)，传入 `--source-font-dir`（悠哉原始目录）、`--source-code-dir`（web/src）、`--output`（临时输出目录）；需要 Python 3、fontTools 与 Brotli，正常启动和构建网站不需要这些字体工具。输出的 `chunks/` 对应本目录的 `fonts/yozai/`，`wemove-hand.css` 对应 [src/fonts/yozai.css](../../src/fonts/yozai.css)，清单对应 `fonts/yozai/manifest.json`。

新增页面文案仍会自动请求补充分块，不必每次重新生成；需要把新常用字并入 core 时再运行脚本。脚本验证每个字重的分块无重叠、并集与源 CJK 映射一致、源码字形覆盖和原始版权许可保留。
