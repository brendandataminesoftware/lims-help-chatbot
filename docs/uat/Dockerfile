FROM texlive/texlive:latest-small

WORKDIR /docs

# Copy all LaTeX source files
COPY *.tex ./

# Compile the document (run twice for table of contents and references)
RUN pdflatex -interaction=nonstopmode main.tex && \
    pdflatex -interaction=nonstopmode main.tex

# Output stage - copy PDF to accessible location
FROM alpine:latest
WORKDIR /output
COPY --from=0 /docs/main.pdf ./uat-documentation.pdf

CMD ["cp", "/output/uat-documentation.pdf", "/export/"]
