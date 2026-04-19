#!/usr/bin/env python3
"""Cut per-page sentence audio clips from a full-book audio file using ingestion.json timings."""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
BOOKS_DIR = ROOT / "content" / "books"
DEFAULT_FFMPEG = "/Users/ervinzhang/Downloads/ffmpeg/bin/ffmpeg"


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def run(cmd: list[str]) -> None:
    subprocess.run(cmd, cwd=ROOT, check=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("book_id", help="Book id under content/books/")
    parser.add_argument("--ffmpeg", default=DEFAULT_FFMPEG, help=f"ffmpeg binary path (default: {DEFAULT_FFMPEG})")
    parser.add_argument("--force", action="store_true", help="Overwrite existing page sentence clips")
    args = parser.parse_args()

    book_dir = BOOKS_DIR / args.book_id
    ingestion_path = book_dir / "ingestion.json"
    if not ingestion_path.is_file():
        raise SystemExit(f"ingestion.json not found: {ingestion_path}")

    ingestion = read_json(ingestion_path)
    full_book_asset = ingestion.get("audio", {}).get("fullBookAsset")
    if not full_book_asset:
        raise SystemExit("ingestion.json missing audio.fullBookAsset")

    full_book_path = book_dir / str(full_book_asset).lstrip("/")
    if not full_book_path.is_file():
        raise SystemExit(f"full-book audio not found: {full_book_path}")

    page_alignments = ingestion.get("pageAudioAlignment") or []
    if not page_alignments:
        raise SystemExit("ingestion.json missing pageAudioAlignment entries")

    generated = 0
    skipped = 0
    for entry in page_alignments:
        page_no = entry.get("pageNo")
        start_sec = entry.get("fullBookAudioStartSec")
        end_sec = entry.get("fullBookAudioEndSec")
        sentence_audio_asset = entry.get("sentenceAudioAsset")
        if page_no is None or not sentence_audio_asset:
            skipped += 1
            continue
        if start_sec is None or end_sec is None or float(end_sec) <= float(start_sec):
            skipped += 1
            continue

        output_path = book_dir / str(sentence_audio_asset).lstrip("/")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        if output_path.exists() and not args.force:
            skipped += 1
            continue

        duration = float(end_sec) - float(start_sec)
        run(
            [
                args.ffmpeg,
                "-y",
                "-ss",
                f"{float(start_sec):.3f}",
                "-t",
                f"{duration:.3f}",
                "-i",
                str(full_book_path),
                "-vn",
                "-c:a",
                "aac",
                "-b:a",
                "96k",
                str(output_path),
            ]
        )
        entry["sentenceAudioStatus"] = "generated"
        generated += 1

    write_json(ingestion_path, ingestion)
    print(f"generated={generated} skipped={skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
