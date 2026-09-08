#!/usr/bin/env python3
"""Extract the existing hero fonts into filled SVG glyphs (fontTools + Brotli).

The authored pen trajectories live in src/features/home/*Strokes.ts.
Coordinates are normalized to 1000 units/em with baseline y=900, y pointing down.
Source font licenses remain in public/assets/fonts/ (Caveat and Yozai SIL OFL).
Run from any directory; no network or runtime font parser is needed by the site.
"""
from pathlib import Path
import json
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

ROOT = Path(__file__).resolve().parents[2]
FONT_ROOT = ROOT / 'apps/web/public/assets/fonts'
SOURCES = {
    'english': (FONT_ROOT / 'caveat.woff2', 'Life is betterat play.'),
    'chinese': (FONT_ROOT / 'yozai/wemove-hand-500-core.woff2', '把日常，玩出新花样。'),
}

def number(value):
    return str(round(value, 2)).removesuffix('.0')

def extract():
    output = {}
    for language, (path, text) in SOURCES.items():
        font = TTFont(path)
        glyphs = font.getGlyphSet()
        cmap = font.getBestCmap()
        scale = 1000 / font['head'].unitsPerEm
        data = {}
        for character in dict.fromkeys(text):
            name = cmap[ord(character)]
            pen = SVGPathPen(glyphs, ntos=number)
            glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, 0, 900)))
            data[character] = {'advance': round(glyphs[name].width * scale, 2), 'path': pen.getCommands()}
        output[language] = data
        print(language, 'unitsPerEm=', font['head'].unitsPerEm, 'glyphs=', len(data))
    target = ROOT / 'apps/web/src/features/home/heroGlyphs.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(output, ensure_ascii=False, separators=(',', ':')) + '\n')
    return output

if __name__ == '__main__':
    extract()
