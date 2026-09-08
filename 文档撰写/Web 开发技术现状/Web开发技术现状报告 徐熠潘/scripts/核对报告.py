#!/usr/bin/env python3
"""Check the report's sources and count narrative Chinese characters conservatively."""
from pathlib import Path
import re
import json

ROOT = Path(__file__).resolve().parents[1]
chapters = sorted((ROOT / "chapters").glob("*.tex"))
body_files = sorted((ROOT / "chapters").glob("0[1-7]-*.tex"))
body = "\n".join(p.read_text(encoding="utf-8") for p in body_files)
all_text = "\n".join(p.read_text(encoding="utf-8") for p in chapters)
narrative = re.sub(r"\\begin\{(figure|table)\}.*?\\end\{\1\}", "", body, flags=re.S)
narrative = re.sub(r"\\\[.*?\\\]|\$[^$]*\$", "", narrative, flags=re.S)
narrative = re.sub(
    r"\\(?:section|subsection|subsubsection|caption|sourcecaption|cite|ref|label|input|includegraphics)"
    r"(?:\[[^]]*\])?\{[^}]*\}", "", narrative)
chinese_chars = len(re.findall(r"[\u4e00-\u9fff]", narrative))
assert chinese_chars >= 3000, chinese_chars

citations = {key for group in re.findall(r"\\cite\{([^}]+)\}", all_text) for key in group.split(",")}
bib = re.findall(r"\\bibitem\{([^}]+)\}", all_text)
assert len(bib) == len(set(bib)), "Repeated reference keys"
assert citations == set(bib), {"missing": citations - set(bib), "unused": set(bib) - citations}
labels = set(re.findall(r"\\label\{([^}]+)\}", all_text))
assert set(re.findall(r"\\ref\{([^}]+)\}", all_text)) <= labels

for p in [ROOT / "main.tex", *chapters]:
    text = p.read_text(encoding="utf-8")
    for relative in re.findall(r"\\input\{([^}]+)\}", text):
        assert (ROOT / relative).is_file(), relative
    for name in re.findall(r"\\includegraphics(?:\[[^]]*\])?\{([^}]+)\}", text):
        assert (ROOT / "figures" / name).is_file(), name
    assert not any(line.rstrip() != line for line in text.splitlines()), f"Trailing whitespace: {p}"

log = ROOT / ".build/main.log"
warnings = []
if log.exists():
    warnings = re.findall(r"^.*(?:Warning|Overfull|Underfull|Missing character|undefined).*$",
                          log.read_text(errors="replace"), flags=re.M)
assert not warnings, warnings

summary = {
    "narrative_chinese_characters": chinese_chars,
    "counting_scope": "chapters 1-7; exclude headings, figures, tables, formulas, citations, abstract, bibliography; Chinese characters only",
    "figures": len(re.findall(r"\\begin\{figure\}", body)),
    "tables": len(re.findall(r"\\begin\{table\}", body)),
    "references": len(bib),
    "missing_citations_or_assets": 0,
    "latex_warnings": len(warnings),
}
print(json.dumps(summary, ensure_ascii=False, indent=2))
