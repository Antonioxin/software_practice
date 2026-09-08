# LaTeX 编译 Makefile
# 使用方法：make        编译
#           make clean  清理临时文件
#           make open   编译后打开 PDF

MAIN = main
LATEX = xelatex
LATEXFLAGS = -interaction=nonstopmode -file-line-error

.PHONY: all clean open

all:
	$(LATEX) $(LATEXFLAGS) $(MAIN).tex
	$(LATEX) $(LATEXFLAGS) $(MAIN).tex

open: all
	open $(MAIN).pdf

clean:
	rm -f *.aux *.log *.toc *.out *.fls *.fdb_latexmk *.synctex.gz