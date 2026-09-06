#!/usr/bin/env python3
"""Create full-coverage CJK-only on-demand WOFF2 chunks without editing sources.
Requires Python 3 with fontTools and Brotli. Output CSS expects files served
from /assets/fonts/yozai/. Each generated font uses the derivative name Wemove Hand.
"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools import subset
from concurrent.futures import ProcessPoolExecutor, as_completed
import argparse, hashlib, json, shutil, re, sys

RANGES=((0x2E80,0x9FFF),(0xF900,0xFAFF),(0xFE10,0xFE1F),(0xFE30,0xFE4F),(0xFF00,0xFFEF),(0x20000,0x323AF))
WEIGHTS=(('Regular',400),('Medium',500))

def is_cjk(c):return any(a<=c<=b for a,b in RANGES)
def ranges_css(points):
 points=sorted(set(points));spans=[]
 if not points:return ''
 a=b=points[0]
 for n in points[1:]:
  if n==b+1:b=n
  else:spans.append((a,b));a=b=n
 spans.append((a,b))
 return ','.join(f'U+{a:X}' if a==b else f'U+{a:X}-{b:X}' for a,b in spans)
def preserved_records(font):
 return {(n.nameID,n.platformID,n.platEncID,n.langID):n.toUnicode() for n in font['name'].names if n.nameID in (0,13,14)}
def rename(font,style,index):
 # Copyright, license and author records are deliberately retained.
 replacement={1:'Wemove Hand',2:style,3:f'WemoveHand-{style}-{index}-0.868',4:f'Wemove Hand {style}',6:f'WemoveHand-{style}',16:'Wemove Hand',17:style,18:f'Wemove Hand {style}',19:'Wemove Hand',21:'Wemove Hand',22:style}
 name=font['name']
 for n in name.names:
  if n.nameID in replacement:n.string=replacement[n.nameID].encode(n.getEncoding(),errors='replace')
 for id_,value in replacement.items():
  name.setName(value,id_,3,1,0x409)
  name.setName(value,id_,1,0,0)
 # All primary and identity names are derivative names; preserved copyright URLs
 # legitimately still identify the upstream font, as required by its license.
 assert all('yozai' not in n.toUnicode().lower() and '悠哉' not in n.toUnicode() for n in name.names if n.nameID in replacement)

def produce(job):
 source,dest,style,weight,index,points=job
 font=TTFont(source);original_legal=preserved_records(font)

 opts=subset.Options();opts.flavor='woff2';opts.name_IDs=['*'];opts.name_languages=['*'];opts.name_legacy=True
 opts.drop_tables += ['feat','morx'];opts.layout_features=['*'];opts.notdef_glyph=True;opts.notdef_outline=True;opts.recalc_bounds=True;opts.recalc_timestamp=False
 sub=subset.Subsetter(options=opts);sub.populate(unicodes=points);sub.subset(font)
 rename(font,style,index);font['OS/2'].usWeightClass=weight;font.flavor='woff2';font.save(dest)
 actual=TTFont(dest);codes=set(actual.getBestCmap())
 assert codes==set(points),(str(dest),'cmap mismatch',len(codes),len(points))
 assert preserved_records(actual)==original_legal,(str(dest),'legal metadata changed')
 assert actual['OS/2'].usWeightClass==weight
 for nameid in (1,2,3,4,6,16,17,18,19,21,22):
  for n in actual['name'].names:
   if n.nameID==nameid:assert 'yozai' not in n.toUnicode().lower() and '悠哉' not in n.toUnicode()
 return {'file':Path(dest).name,'style':style,'weight':weight,'chunk':index,'bytes':Path(dest).stat().st_size,'unicodeCount':len(codes),'unicodeRange':ranges_css(codes),'sha256':hashlib.sha256(Path(dest).read_bytes()).hexdigest()}

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--source-font-dir',type=Path,required=True);parser.add_argument('--source-code-dir',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--workers',type=int,default=4);args=parser.parse_args()
 fontroot=args.source_font_dir;src=args.source_code_dir;out=args.output;dest=out/'chunks';dest.mkdir(parents=True,exist_ok=True)
 source_chars=set();sourcefiles=[]
 for p in sorted(src.rglob('*')):
  if p.suffix not in ('.vue','.ts') or '.spec.' in p.name:continue
  sourcefiles.append(str(p.relative_to(src)));source_chars.update(ord(c) for c in p.read_text() if is_cjk(ord(c)))
 cmaps={style:set(TTFont(fontroot/f'Yozai-{style}.ttf').getBestCmap()) for style,_ in WEIGHTS}
 relevant={style:{c for c in points if is_cjk(c)} for style,points in cmaps.items()};assert relevant['Regular']==relevant['Medium']
 full=relevant['Regular'];core=full&source_chars;rest=sorted(full-core);chunks=[('core',sorted(core))]+[(f'{i//512+1:03d}',rest[i:i+512]) for i in range(0,len(rest),512)]
 assert len(set().union(*(set(p) for _,p in chunks)))==len(full)
 assert sum(len(p) for _,p in chunks)==len(full)
 tasks=[]
 for style,weight in WEIGHTS:
  for i,points in chunks:tasks.append((str(fontroot/f'Yozai-{style}.ttf'),str(dest/f'wemove-hand-{weight}-{i}.woff2'),style,weight,i,points))
 print(f'Source files: {len(sourcefiles)}; source CJK: {len(source_chars)}; supported core: {len(core)}; full CJK: {len(full)}; chunks per weight: {len(chunks)}',flush=True)
 records=[]
 with ProcessPoolExecutor(max_workers=args.workers) as pool:
  futures=[pool.submit(produce,t) for t in tasks]
  for future in as_completed(futures):
   r=future.result();records.append(r)
   if r['chunk']=='core' or len(records)%10==0:print(f"Completed {len(records)}/{len(tasks)}: {r['file']} = {r['bytes']:,} bytes",flush=True)
 records.sort(key=lambda r:(r['weight'],r['chunk']!='core',r['chunk']))
 # Read produced files again and verify disjoint unions independently.
 for style,weight in WEIGHTS:
  union=set()
  for r in [r for r in records if r['weight']==weight]:
   points=set(TTFont(dest/r['file']).getBestCmap());assert not (union&points),(weight,'overlap');union|=points
  assert union==relevant[style],(weight,'union mismatch')
  assert (source_chars&cmaps[style])<=union,(weight,'source characters dropped')
 css=['/* Generated full CJK coverage. Derivative font family: Wemove Hand.',' * Sources: local Yozai 0.868, under SIL OFL 1.1; retain Yozai-OFL.txt.',' * Core covers current Vue/TS text; disjoint remaining chunks keep all source CJK mappings.',' * This file is a source stylesheet; font URLs point to public/assets/fonts/yozai/.',' */','']
 for r in records:
  css += ["@font-face {", "  font-family: 'Wemove Hand';",f"  src: url('/assets/fonts/yozai/{r['file']}') format('woff2');",f"  font-weight: {r['weight']};","  font-style: normal;","  font-display: swap;",f"  unicode-range: {r['unicodeRange']};","}",'']
 (out/'wemove-hand.css').write_text('\n'.join(css))
 shutil.copy2(fontroot/'OFL.txt',dest/'Yozai-OFL.txt')
 report={'family':'Wemove Hand','sourceFamily':'Yozai','sourceVersion':'0.868','sourceFiles':sourcefiles,'sourceCjkCount':len(source_chars),'coreUnicodeCount':len(core),'sourceCharactersAbsentUpstream':''.join(chr(c) for c in sorted(source_chars-full)),'fullCjkUnicodeCount':len(full),'chunksPerWeight':len(chunks),'chunkTarget':512,'rangeFilter':ranges_css(c for a,b in RANGES for c in range(a,b+1)),'weights':{str(w):{'coreBytes':next(r['bytes'] for r in records if r['weight']==w and r['chunk']=='core'),'totalBytes':sum(r['bytes'] for r in records if r['weight']==w)} for _,w in WEIGHTS},'validation':{'allSubsetCmapsEqualRequested':True,'disjointWithinEachWeight':True,'unionMatchesSourceCjk':True,'allSupportedSourceCjkPreserved':True,'legalNameRecordsPreserved':True,'reservedPrimaryNamesRemoved':True},'subsetNotes':['Apple AAT feat and morx tables are removed because fontTools does not support subsetting them; supported OpenType layout features are retained.'],'files':records}
 (out/'chunk-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:report[k] for k in ('family','sourceCjkCount','coreUnicodeCount','sourceCharactersAbsentUpstream','fullCjkUnicodeCount','chunksPerWeight','weights','validation')},ensure_ascii=False,indent=2),flush=True)
if __name__=='__main__':main()
