# Theme

## Compact token summary

- Primary wood: `#A89880`; secondary sand: `#C4B8A8`; ink: `#3D3226`.
- Surfaces: paper `#FFFFFF`, parchment `#F7F4EE`, moss wash `#EDF5E1`.
- Feedback: success `#4D7557`, warning `#9A6B2F`, danger `#9D493F`.
- Typography: display `"Noto Serif SC", "Songti SC", serif`; body `"PingFang SC", "Microsoft YaHei", sans-serif`.
- Radius: 6/10/16/24px; shadow: low warm shadows only.
- Breakpoints: 390px phone target, 768px compact navigation, 1440px desktop target.
- Motion: 160–240ms ease-out; honor `prefers-reduced-motion`.

## Raw reference values

Source: root element and layout stylesheet in `material/首页.html`.

```css
:root {
  --primary-color: #A89880;
  --secondary-color: #C4B8A8;
  --background-color: #ffffff;
  --text-color: #3D3226;
  --font-family: "PingFang SC", sans-serif;
}

.site-header { background: #fff; border-bottom: 1px solid #eee; }
.hn-link { color: #555; border-radius: 8px; }
.site-footer { background: #fff; border-top: 1px solid #eee; }
```
