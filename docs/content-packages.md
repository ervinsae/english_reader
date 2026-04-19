# Content Packages

This app now uses a remote-first content model with two runtime sources:

- Installed local packages in the app files directory under `filesDir/book-packages/installed/<book-id>/package/`
- Remote bookshelf previews from `bookshelf.json`, with per-book packages downloaded on demand into the same installed location

Authoring and publishing source packages now live in `content/books/` inside the repo. Package JSON can stay relative-path based.

## Standard Package Layout

Each package zip should contain a single book package root with these files at the root:

```text
book.json
pages.json
words.json
cover.png
pages/
audio/
```

The zip may either place those files directly at the archive root or inside one top-level folder.

## `book.json`

Required fields:

```json
{
  "id": "oxford-tree-01",
  "title": "The Apple",
  "level": "L1",
  "coverAsset": "cover.png",
  "pageCount": 3,
  "version": "2026.04.13",
  "tags": ["sample"],
  "enabled": true
}
```

Notes:

- `version` is optional but recommended for remote catalog upgrades.
- `coverAsset` and `coverUri` are both accepted. Relative paths are resolved within the package.

## `pages.json`

```json
[
  {
    "bookId": "oxford-tree-01",
    "pageNo": 1,
    "imageAsset": "pages/001.png",
    "englishText": "Dad has an apple.",
    "sentenceAudioAsset": "audio/sentences/001.m4a",
    "words": [
      { "wordId": "dad", "text": "Dad", "startIndex": 0, "endIndex": 3 },
      { "wordId": "apple", "text": "apple", "startIndex": 11, "endIndex": 16 }
    ]
  }
]
```

Notes:

- `imageAsset` and `imageUri` are both accepted.
- `sentenceAudioAsset` and `sentenceAudioUri` are both accepted.

## `words.json`

```json
[
  {
    "id": "apple",
    "text": "apple",
    "meaningZh": "苹果",
    "phonetic": "/ˈæpəl/",
    "audioAsset": "audio/words/apple.m4a"
  }
]
```

Notes:

- `audioAsset` and `audioUri` are both accepted.

## Local Installation Flow

At runtime the app checks installed local packages in `filesDir/book-packages/installed/<book-id>/package/`.

The bookshelf screen separately merges:

1. Local readable books from installed packages
2. Remote bookshelf preview metadata from `bookshelf.json`

When the user opens a remote-only book, the app downloads its package, installs it locally, refreshes the local repository, and then opens the reader.

The bookshelf action now refreshes remote bookshelf metadata only. It no longer installs inbox zips or downloads book content proactively.

## Remote Content Config

Bundled defaults live in:

- `app/src/main/assets/content/catalog-config.json`
- `app/src/main/assets/content/bookshelf.json`

You can override the config on-device by writing:

- `filesDir/book-packages/config/catalog-config.json`

Config shape:

```json
{
  "bookshelfUrl": "https://example.com/english-reader/bookshelf.json",
  "catalogUrl": "https://example.com/english-reader/catalog.json"
}
```

Remote bookshelf shape:

```json
{
  "books": [
    {
      "bookId": "oxford-tree-01",
      "title": "The Apple",
      "level": "L1",
      "pageCount": 3,
      "coverUri": "https://example.com/books/oxford-tree-01/cover.png",
      "tags": ["sample"],
      "package": {
        "version": "2026.04.13",
        "downloadUrl": "https://example.com/books/oxford-tree-01/package.zip",
        "sha256": "..."
      }
    }
  ]
}
```

Notes:

- `coverUri` is preferred for remote manifests.
- `package` is required for remote-only books that should download on tap.

## Packaging Script

To create a new source book package from an inbound PDF + MP3, use:

```bash
python3 scripts/ingest_book.py --pdf /path/to/book.pdf --mp3 /path/to/book.mp3
```

See `docs/book-ingestion.md` for the full ingestion workflow, page-range review, and alignment placeholders.

Use:

```bash
python3 tools/package_content_package.py /path/to/book-folder
```

Optional output path:

```bash
python3 tools/package_content_package.py /path/to/book-folder --output dist/oxford-tree-01.zip
```

The script validates required files and all referenced local assets before writing the zip.
