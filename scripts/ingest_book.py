#!/usr/bin/env python3
"""Create a reusable bundled-book package from a PDF + full-book MP3."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT_ROOT = ROOT / "app" / "src" / "main" / "assets" / "books"
RENDERER_SOURCE = ROOT / "scripts" / "pdf_book_renderer.m"
RENDERER_BINARY = ROOT / "dist" / "tools" / "pdf_book_renderer"
UUID_STEM_RE = re.compile(
    r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
)


def run(cmd: list[str], *, capture_output: bool = False) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        cwd=ROOT,
        check=True,
        text=True,
        capture_output=capture_output,
    )


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def normalize_book_id(raw_value: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", "-", raw_value.strip().lower())
    normalized = re.sub(r"-{2,}", "-", normalized).strip("-")
    return normalized


def is_uuid_like(stem: str) -> bool:
    return UUID_STEM_RE.fullmatch(stem) is not None


def parse_index_spec(spec: str) -> list[int]:
    indices: list[int] = []
    for part in (chunk.strip() for chunk in spec.split(",")):
        if not part:
            continue
        if "-" in part:
            start_text, end_text = part.split("-", 1)
            start = int(start_text)
            end = int(end_text)
            step = 1 if end >= start else -1
            indices.extend(range(start, end + step, step))
        else:
            indices.append(int(part))
    seen: set[int] = set()
    ordered: list[int] = []
    for index in indices:
        if index not in seen:
            seen.add(index)
            ordered.append(index)
    return ordered


def ensure_renderer_binary() -> Path:
    if not RENDERER_SOURCE.is_file():
        raise SystemExit(f"renderer source not found: {RENDERER_SOURCE}")

    needs_compile = (
        not RENDERER_BINARY.is_file()
        or RENDERER_BINARY.stat().st_mtime < RENDERER_SOURCE.stat().st_mtime
    )
    if not needs_compile:
        return RENDERER_BINARY

    RENDERER_BINARY.parent.mkdir(parents=True, exist_ok=True)
    run(
        [
            "clang",
            "-fobjc-arc",
            "-framework",
            "Foundation",
            "-framework",
            "PDFKit",
            "-framework",
            "AppKit",
            str(RENDERER_SOURCE),
            "-o",
            str(RENDERER_BINARY),
        ]
    )
    return RENDERER_BINARY


def inspect_pdf(renderer_binary: Path, pdf_path: Path) -> dict[str, Any]:
    result = run(
        [str(renderer_binary), "inspect", str(pdf_path)],
        capture_output=True,
    )
    return json.loads(result.stdout)


def render_pdf_page(
    renderer_binary: Path,
    pdf_path: Path,
    page_index: int,
    output_path: Path,
    max_dimension: int,
) -> dict[str, Any]:
    result = run(
        [
            str(renderer_binary),
            "render",
            str(pdf_path),
            str(page_index),
            str(output_path),
            str(max_dimension),
        ],
        capture_output=True,
    )
    return json.loads(result.stdout)


def probe_audio_duration(ffprobe_path: str, audio_path: Path) -> float:
    result = run(
        [
            ffprobe_path,
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            str(audio_path),
        ],
        capture_output=True,
    )
    return float(result.stdout.strip())


def derive_title(
    *,
    explicit_title: str | None,
    pdf_metadata_title: str | None,
    pdf_stem: str,
) -> tuple[str, str]:
    if explicit_title and explicit_title.strip():
        return explicit_title.strip(), "argument"

    if pdf_metadata_title and pdf_metadata_title.strip():
        return pdf_metadata_title.strip(), "pdf-metadata"

    if not is_uuid_like(pdf_stem):
        candidate = re.sub(r"[_-]+", " ", pdf_stem).strip()
        if candidate:
            return candidate, "pdf-filename"

    return "Untitled Book", "fallback"


def derive_book_id(
    *,
    explicit_book_id: str | None,
    title: str,
    pdf_stem: str,
) -> tuple[str, str, bool]:
    if explicit_book_id and explicit_book_id.strip():
        book_id = normalize_book_id(explicit_book_id)
        if not book_id:
            raise SystemExit(f"book id is empty after normalization: {explicit_book_id}")
        return book_id, "argument", False

    if not is_uuid_like(pdf_stem):
        book_id = normalize_book_id(pdf_stem)
        if book_id:
            return book_id, "pdf-filename", False

    book_id = normalize_book_id(title)
    if book_id:
        return book_id, "title-derived", True

    fallback = f"book-{date.today():%Y%m%d}"
    return fallback, "fallback", True


def infer_reading_pages(page_count: int, cover_page_index: int) -> tuple[list[int], str]:
    reading_pages = [index for index in range(page_count) if index != cover_page_index]
    return reading_pages, "auto-all-non-cover-pages"


def build_pages_payload(
    *,
    book_id: str,
    reading_page_indices: list[int],
) -> list[dict[str, Any]]:
    pages: list[dict[str, Any]] = []
    for page_no, source_page_index in enumerate(reading_page_indices, start=1):
        pages.append(
            {
                "bookId": book_id,
                "pageNo": page_no,
                "imageAsset": f"pages/{page_no:03d}.png",
                "englishText": "",
                "words": [],
                "sourcePdfPageIndex": source_page_index,
                "alignmentStatus": "pending",
            }
        )
    return pages


def build_ingestion_payload(
    *,
    pdf_path: Path,
    mp3_path: Path,
    title: str,
    title_source: str,
    book_id: str,
    book_id_source: str,
    provisional_book_id: bool,
    pdf_info: dict[str, Any],
    cover_page_index: int,
    reading_page_indices: list[int],
    reading_page_source: str,
    audio_duration_seconds: float,
    level: str,
    tags: list[str],
) -> dict[str, Any]:
    skipped_pages = [
        page["index"]
        for page in pdf_info["pages"]
        if page["index"] not in set(reading_page_indices) and page["index"] != cover_page_index
    ]
    alignment_entries = [
        {
            "pageNo": page_no,
            "sourcePdfPageIndex": source_page_index,
            "fullBookAudioStartSec": None,
            "fullBookAudioEndSec": None,
            "transcriptText": "",
            "transcriptStatus": "pending",
            "sentenceAudioAsset": f"audio/sentences/{page_no:03d}.m4a",
            "sentenceAudioStatus": "pending",
            "notes": [
                "Fill timings from transcript alignment or turn-page cue detection.",
                "Keep englishText in pages.json consistent with the final transcript text.",
            ],
        }
        for page_no, source_page_index in enumerate(reading_page_indices, start=1)
    ]
    return {
        "workflowVersion": 1,
        "createdAtUtc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "book": {
            "id": book_id,
            "idSource": book_id_source,
            "idIsProvisional": provisional_book_id,
            "title": title,
            "titleSource": title_source,
            "level": level,
            "tags": tags,
        },
        "sourceFiles": {
            "pdfFilename": pdf_path.name,
            "mp3Filename": mp3_path.name,
        },
        "pdf": {
            "pageCount": pdf_info["pageCount"],
            "documentTitle": pdf_info.get("documentTitle", ""),
            "coverPageIndex": cover_page_index,
            "readingPageIndices": reading_page_indices,
            "readingPageSelection": reading_page_source,
            "skippedPageIndices": skipped_pages,
        },
        "audio": {
            "fullBookAsset": "audio/full-book.mp3",
            "durationSec": audio_duration_seconds,
            "pageSentenceAssetsPattern": "audio/sentences/{pageNo}.m4a",
            "pageSentenceAlignmentStatus": "pending",
        },
        "pageAudioAlignment": alignment_entries,
        "notes": [
            "This metadata is for ingestion and manual review. The app runtime ignores this file.",
            "If the inbound PDF filename was UUID-renamed, the book id should be treated as provisional until cross-checked with the original source filename or catalog naming rules.",
        ],
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pdf", required=True, type=Path, help="Input PDF path")
    parser.add_argument("--mp3", required=True, type=Path, help="Input full-book MP3 path")
    parser.add_argument("--title", help="Book title override")
    parser.add_argument("--book-id", help="Book id override before normalization")
    parser.add_argument("--level", default="L1", help="Book level label")
    parser.add_argument("--tag", action="append", default=[], help="Book tag; repeat for multiple values")
    parser.add_argument("--version", default=date.today().strftime("%Y.%m.%d"), help="Book/package version")
    parser.add_argument("--cover-page", type=int, default=0, help="Zero-based PDF page index used for cover.png")
    parser.add_argument(
        "--reading-pages",
        help="Zero-based PDF page indices or ranges, for example: 2-15 or 1,2,4-8",
    )
    parser.add_argument(
        "--output-root",
        type=Path,
        default=DEFAULT_OUTPUT_ROOT,
        help="Root directory where app book assets live",
    )
    parser.add_argument(
        "--render-max-dimension",
        type=int,
        default=1600,
        help="Longest pixel dimension for rendered PNG pages",
    )
    parser.add_argument(
        "--ffprobe",
        default="/usr/local/bin/ffprobe",
        help="ffprobe binary used to read audio duration",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace an existing app/src/main/assets/books/<bookId> directory",
    )
    return parser.parse_args()


def validate_args(args: argparse.Namespace) -> None:
    if not args.pdf.is_file():
        raise SystemExit(f"pdf not found: {args.pdf}")
    if not args.mp3.is_file():
        raise SystemExit(f"mp3 not found: {args.mp3}")
    if shutil.which("clang") is None:
        raise SystemExit("missing required compiler: clang")
    if shutil.which(args.ffprobe) is None and not Path(args.ffprobe).is_file():
        raise SystemExit(f"ffprobe not found: {args.ffprobe}")


def main() -> int:
    args = parse_args()
    validate_args(args)

    renderer_binary = ensure_renderer_binary()
    pdf_info = inspect_pdf(renderer_binary, args.pdf)
    title, title_source = derive_title(
        explicit_title=args.title,
        pdf_metadata_title=str(pdf_info.get("documentTitle", "")).strip() or None,
        pdf_stem=args.pdf.stem,
    )
    book_id, book_id_source, provisional_book_id = derive_book_id(
        explicit_book_id=args.book_id,
        title=title,
        pdf_stem=args.pdf.stem,
    )

    if args.reading_pages:
        reading_page_indices = parse_index_spec(args.reading_pages)
        reading_page_source = "argument"
    else:
        reading_page_indices, reading_page_source = infer_reading_pages(
            page_count=int(pdf_info["pageCount"]),
            cover_page_index=args.cover_page,
        )

    page_count = int(pdf_info["pageCount"])
    invalid_pages = [
        index
        for index in [args.cover_page, *reading_page_indices]
        if index < 0 or index >= page_count
    ]
    if invalid_pages:
        raise SystemExit(f"page indices out of range for {page_count}-page pdf: {sorted(set(invalid_pages))}")
    if args.cover_page in reading_page_indices:
        raise SystemExit("cover page index cannot also be part of reading pages")
    if not reading_page_indices:
        raise SystemExit("reading pages are empty")

    book_dir = args.output_root / book_id
    if book_dir.exists():
        if not args.overwrite:
            raise SystemExit(f"output directory already exists: {book_dir} (use --overwrite to replace it)")
        shutil.rmtree(book_dir)

    pages_dir = book_dir / "pages"
    audio_dir = book_dir / "audio"
    pages_dir.mkdir(parents=True, exist_ok=True)
    audio_dir.mkdir(parents=True, exist_ok=True)

    render_pdf_page(
        renderer_binary=renderer_binary,
        pdf_path=args.pdf,
        page_index=args.cover_page,
        output_path=book_dir / "cover.png",
        max_dimension=args.render_max_dimension,
    )

    for page_no, source_page_index in enumerate(reading_page_indices, start=1):
        render_pdf_page(
            renderer_binary=renderer_binary,
            pdf_path=args.pdf,
            page_index=source_page_index,
            output_path=pages_dir / f"{page_no:03d}.png",
            max_dimension=args.render_max_dimension,
        )

    shutil.copy2(args.mp3, audio_dir / "full-book.mp3")
    audio_duration_seconds = probe_audio_duration(args.ffprobe, args.mp3)

    book_payload = {
        "id": book_id,
        "title": title,
        "level": args.level,
        "coverAsset": "cover.png",
        "pageCount": len(reading_page_indices),
        "version": args.version,
        "tags": args.tag,
        "enabled": True,
    }
    pages_payload = build_pages_payload(
        book_id=book_id,
        reading_page_indices=reading_page_indices,
    )
    words_payload: list[dict[str, Any]] = []
    ingestion_payload = build_ingestion_payload(
        pdf_path=args.pdf,
        mp3_path=args.mp3,
        title=title,
        title_source=title_source,
        book_id=book_id,
        book_id_source=book_id_source,
        provisional_book_id=provisional_book_id,
        pdf_info=pdf_info,
        cover_page_index=args.cover_page,
        reading_page_indices=reading_page_indices,
        reading_page_source=reading_page_source,
        audio_duration_seconds=audio_duration_seconds,
        level=args.level,
        tags=args.tag,
    )

    write_json(book_dir / "book.json", book_payload)
    write_json(book_dir / "pages.json", pages_payload)
    write_json(book_dir / "words.json", words_payload)
    write_json(book_dir / "ingestion.json", ingestion_payload)

    summary = {
        "bookId": book_id,
        "title": title,
        "pageCount": len(reading_page_indices),
        "pdfPageCount": page_count,
        "coverPageIndex": args.cover_page,
        "readingPageIndices": reading_page_indices,
        "outputDirectory": str(book_dir),
        "audioDurationSec": audio_duration_seconds,
        "bookIdIsProvisional": provisional_book_id,
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
