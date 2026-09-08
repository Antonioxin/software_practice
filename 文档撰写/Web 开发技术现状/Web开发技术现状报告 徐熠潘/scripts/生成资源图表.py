#!/usr/bin/env python3
"""Rebuild source-byte chart with standard-library Python and PGFPlots/XeLaTeX."""
from pathlib import Path
import json
import struct
import subprocess
import shutil

REPORT = Path(__file__).resolve().parents[1]
REPO = next(p for p in REPORT.parents if (p / "project-implementation/apps/web").is_dir())
ASSETS = REPO / 'project-implementation/apps/web/public/assets/products/guides'
LABELS = {
    'balance-stones-guide.png': '平衡石',
    'forest-kit-guide.png': '自然观察工具',
    'rainbow-arch-guide.png': '彩虹拱',
    'ring-toss-guide.png': '木质套圈',
    'skip-rope-guide.png': '跳绳',
    'team-board-guide.png': '多人协作板',
}
rows = []
for name, label in LABELS.items():
    asset = ASSETS / name
    data = asset.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', name
    width, height = struct.unpack('>II', data[16:24])
    rows.append({'file': str(asset.relative_to(REPO)), 'label': label,
                 'bytes': len(data), 'width': width, 'height': height})
record = {'date': '2026-09-08', 'method': 'PNG source file bytes, not network transfer or page load',
          'unit': 'MiB = 1048576 bytes', 'total_bytes': sum(r['bytes'] for r in rows),
          'assets': rows}
(REPORT / 'figures/图片资源统计.json').write_text(
    json.dumps(record, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
values = [row['bytes'] / 1048576 for row in rows]
coordinates = ' '.join(f'({value:.8f},{i})' for i, value in enumerate(values))
labels = ','.join(row['label'] for row in rows)
tex = r'''\documentclass[border=3pt]{standalone}
\usepackage[fontset=none]{ctex}
\IfFileExists{/System/Library/Fonts/Supplemental/Songti.ttc}{
\setmainfont{Songti.ttc}[Path=/System/Library/Fonts/Supplemental/,FontIndex=6]
\setCJKmainfont{Songti.ttc}[Path=/System/Library/Fonts/Supplemental/,FontIndex=6]
}{\setmainfont{FandolSong-Regular}\setCJKmainfont{FandolSong-Regular}}
\usepackage{pgfplots}
\pgfplotsset{compat=1.18}
\definecolor{wmbar}{HTML}{496C86}
\begin{document}
\begin{tikzpicture}
\begin{axis}[
 xbar,width=14cm,height=6.6cm,
 xmin=0,xmax=2.1,ymin=-0.65,ymax=5.65,y dir=reverse,
 ytick={0,1,2,3,4,5},yticklabels={LABELS},
 xtick={0,0.5,1,1.5,2},xlabel={源文件体积（MiB）},
 tick label style={font=\fontsize{11}{14}\selectfont},
 label style={font=\fontsize{11}{14}\selectfont},
 axis x line*=bottom,axis y line*=left,axis line style={gray!40},
 xmajorgrids,grid style={gray!20},tick style={draw=none},
 bar width=12pt,
 nodes near coords,point meta=x,
 every node near coord/.append style={font=\fontsize{10.5}{13}\selectfont,
 /pgf/number format/fixed,/pgf/number format/precision=2,/pgf/number format/fixed zerofill},
 enlarge y limits=false]
\addplot[fill=wmbar,draw=none] coordinates {COORDINATES};
\end{axis}
\end{tikzpicture}
\end{document}
'''.replace('LABELS', labels).replace('COORDINATES', coordinates)
source = REPORT / 'figures/图片资源体积.tex'
source.write_text(tex, encoding='utf-8')
build = REPORT / '.build/chart'
build.mkdir(parents=True, exist_ok=True)
result = subprocess.run(['xelatex', '-interaction=nonstopmode', '-halt-on-error',
                         '-output-directory=' + str(build), str(source)],
                        cwd=REPORT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
(build / 'compile.log').write_text(result.stdout)
if result.returncode:
    print(result.stdout[-4000:])
    raise SystemExit(result.returncode)
shutil.copyfile(build / '图片资源体积.pdf', REPORT / 'figures/图片资源体积.pdf')
print(f"{len(rows)} files; {record['total_bytes']} bytes; {sum(values):.4f} MiB")
