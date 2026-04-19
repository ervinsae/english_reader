# Book Ingestion Workflow

Use `scripts/ingest_book.py` to turn an inbound PDF plus full-book MP3 into a bundled asset package under `app/src/main/assets/books/<book-id>/`.

The script is designed for the current macOS environment:

- PDF rasterization uses `PDFKit` through the checked-in Objective-C helper `scripts/pdf_book_renderer.m`
- audio duration probing uses `ffprobe`
- generated helper binaries land under ignored `dist/`

## What The Script Creates

For each ingested book:

- `book.json`
- `pages.json`
- `words.json`
- `cover.png`
- `pages/*.png`
- `audio/full-book.mp3`
- `ingestion.json`

`ingestion.json` is intentionally non-runtime metadata. It records:

- how the `bookId` was chosen
- which PDF pages were used for cover vs reader pages
- the source PDF/MP3 filenames
- the full-book audio duration
- placeholder page-level alignment entries for future transcript and sentence-audio work

## Basic Usage

```bash
python3 scripts/ingest_book.py \
  --pdf /path/to/book.pdf \
  --mp3 /path/to/book.mp3 \
  --title "Animal Tricksters" \
  --level "Stage 10" \
  --tag treetops \
  --tag myths-and-legends \
  --cover-page 0 \
  --reading-pages 2-15
```

Important flags:

- `--book-id`: override the derived id before normalization
- `--reading-pages`: zero-based PDF page indices or ranges such as `2-15` or `1,2,4-8`
- `--overwrite`: replace an existing `assets/books/<bookId>/` directory
- `--render-max-dimension`: control rendered page PNG size

## Id Rules

The preferred id source is the original PDF filename after normalization.

If the inbound PDF filename is a UUID or another opaque attachment name, the script falls back to a normalized title-derived id. In that case the id is marked as provisional in `ingestion.json` until the original source filename or naming convention is confirmed.

## Manual Review Checklist

The script does not fabricate page text or sentence timings. After ingestion, review:

1. Cover page choice.
2. Reader page range.
3. `pages.json` `englishText` values.
4. `words.json` vocabulary coverage.
5. `ingestion.json` `pageAudioAlignment` timings and `audio/sentences/*.m4a` outputs if you split the full MP3.

When page timings are ready, generate page-level sentence clips with:

```bash
python3 scripts/split_page_audio.py <book-id>
```

The script reads `ingestion.json` timing fields (`fullBookAudioStartSec` / `fullBookAudioEndSec`) and writes `audio/sentences/<pageNo>.m4a` for every page that has a valid range.

For PDFs with front matter, contents pages, copyright pages, or back covers, pass an explicit `--reading-pages` range instead of relying on the default non-cover-page fallback.

## Validation And Publish

Validate the generated package before publishing:

```bash
python3 tools/package_content_package.py app/src/main/assets/books/<book-id>
```

Publish remains a separate step through `scripts/publish_content.py`.

Do not commit generated upload artifacts such as:

- `dist/minio/**`
- `dist/*.zip`
- generated remote JSON upload outputs
