# Content Packages

This app now supports three content sources:

- Bundled asset books in `app/src/main/assets/books/`
- Installed local packages in the app files directory under `filesDir/book-packages/installed/<book-id>/package/`
- Optional remote catalog packages downloaded into the same installed location

The runtime model uses `contentUri` values (`asset://...` or `file://...`), but package JSON can stay relative-path based.

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

At runtime the app checks these sources in priority order:

1. Installed local package in `filesDir/book-packages/installed/<book-id>/package/`
2. Bundled asset package in `app/src/main/assets/books/<book-id>/`

To sideload a package without wiring a remote catalog yet:

1. Build a zip with the packaging script below.
2. Copy the zip into `filesDir/book-packages/inbox/` on the device.
3. Tap `同步内容` on the bookshelf screen.

The app installs inbox zips first, then optionally downloads missing packages from the configured remote catalog.

## Remote Catalog Config

Bundled defaults live in:

- `app/src/main/assets/content/catalog-config.json`
- `app/src/main/assets/content/remote-catalog.json`

You can override the config on-device by writing:

- `filesDir/book-packages/config/catalog-config.json`

Config shape:

```json
{
  "catalogUrl": "https://example.com/english-reader/catalog.json",
  "autoSyncOnLaunch": false
}
```

Remote catalog shape:

```json
{
  "packages": [
    {
      "bookId": "oxford-tree-01",
      "title": "The Apple",
      "version": "2026.04.13",
      "downloadUrl": "https://example.com/packages/oxford-tree-01.zip",
      "sha256": "..."
    }
  ]
}
```

## Packaging Script

Use:

```bash
python3 tools/package_content_package.py /path/to/book-folder
```

Optional output path:

```bash
python3 tools/package_content_package.py /path/to/book-folder --output dist/oxford-tree-01.zip
```

The script validates required files and all referenced local assets before writing the zip.
