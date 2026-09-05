# Shared Layouts

No authored application layout exists yet. The captured site establishes this reference shell, which the Role A standalone shell will preserve while making account actions first-class.

Source: `material/首页.html`

```html
<div class="site-root">
  <header class="site-header">
    <div class="header-bar">
      <a href="/" class="header-logo"><img src="logo.png" alt="WEMOVE SPORTS"><span>WeMove</span></a>
      <nav class="header-nav" aria-label="主导航">
        <a href="/" class="hn-link">首页</a>
        <a href="/workshop" class="hn-link">玩具品类</a>
        <a href="/stem" class="hn-link">STEM教育</a>
      </nav>
      <button class="header-burger" aria-label="打开导航"><span></span><span></span><span></span></button>
    </div>
  </header>
  <main class="site-main"></main>
  <footer class="site-footer">
    <a href="/" class="footer-logo">WeMove</a>
    <p class="footer-copy">© 2026 WeMove</p>
  </footer>
</div>
```

Reference source CSS from `material/首页_files/FrontLayout-C-CUGIqS_vSOi.css`:

```css
.site-root{font-family:"PingFang SC","Microsoft YaHei",sans-serif;min-height:100vh;display:flex;flex-direction:column;background:#fff}
.site-header{position:sticky;top:0;z-index:100;background:#fff;border-bottom:1px solid #eee}
.header-bar{max-width:1200px;margin:0 auto;display:flex;align-items:center;height:68px;padding:0 28px}
.header-logo{display:flex;align-items:center;gap:10px;text-decoration:none}
.header-logo img{width:32px;height:32px;border-radius:6px}
.header-logo span{font-size:17px;font-weight:600;color:#1a1a1a;letter-spacing:.5px}
.header-nav{display:flex;align-items:center;margin-left:48px;gap:4px}
.hn-link{padding:8px 14px;font-size:14px;color:#555;text-decoration:none;border-radius:8px}
.site-main{flex:1}
.site-footer{background:#fff;border-top:1px solid #eee;padding:48px 24px 32px;text-align:center}
@media (max-width:768px){.header-bar{height:60px;padding:0 16px}.header-nav{display:none}.header-burger{display:flex}}
```
