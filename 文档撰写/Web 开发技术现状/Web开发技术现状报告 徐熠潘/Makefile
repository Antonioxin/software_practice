LATEXMK ?= latexmk
PDF = Web开发技术现状报告-前端技术与内容管理.pdf

.PHONY: all clean
all:
	$(LATEXMK) -xelatex -interaction=nonstopmode -halt-on-error -file-line-error -outdir=.build main.tex
	cp .build/main.pdf "$(PDF)"

clean:
	$(LATEXMK) -C -outdir=.build main.tex
