# 在本目录运行 make；中间产物放在 .build/。
LATEXMK ?= latexmk
PDF = WEMOVE网站需求规格说明书.pdf

.PHONY: all clean
all:
	$(LATEXMK) -xelatex -interaction=nonstopmode -halt-on-error -file-line-error -outdir=.build main.tex
	cp .build/main.pdf "$(PDF)"

clean:
	$(LATEXMK) -C -outdir=.build main.tex
