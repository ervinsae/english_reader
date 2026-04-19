#!/usr/bin/env python3
"""One-click pipeline for adding a new English Reader book.

Example:
  python3 scripts/add_book_pipeline.py \
    --pdf /path/to/book.pdf \
    --mp3 /path/to/full-book.mp3 \
    --book-id oxford-tree-02 \
    --title "A New Book" \
    --level Stage-02 \
    --tag oxford \
    --publish
"""

from __future__ import annotations

import argparse
import importlib
import json
import subprocess
import sys
from datetime import date
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
BOOKS_DIR = ROOT / "content" / "books"
CONTENT_DIR = ROOT / "app" / "src" / "main" / "assets" / "content"
DIST_DIR = ROOT / "dist"
INGEST_SCRIPT = ROOT / "scripts" / "ingest_book.py"
PACKAGE_SCRIPT = ROOT / "tools" / "package_content_package.py"
PUBLISH_SCRIPT = ROOT / "scripts" / "publish_content.py"
PILLOW_VENDOR_DIR = ROOT / ".cache" / "python-vendor" / "pillow"


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def run(cmd: list[str], *, capture_output: bool = False) -> subprocess.CompletedProcess[str]:
    print(f"$ {' '.join(str(part) for part in cmd)}")
    return subprocess.run(
        cmd,
        cwd=ROOT,
        check=True,
        text=True,
        capture_output=capture_output,
    )


def bucket_base_url_from_bookshelf(bookshelf_url: str) -> str:
    marker = "/catalog/bookshelf.json"
    if not bookshelf_url.endswith(marker):
        raise SystemExit(f"bookshelfUrl does not end with {marker}: {bookshelf_url}")
    return bookshelf_url[: -len(marker)]


def ensure_pillow():
    try:
        from PIL import Image  # type: ignore

        return Image
    except ImportError:
        pass

    PILLOW_VENDOR_DIR.mkdir(parents=True, exist_ok=True)
    if not (PILLOW_VENDOR_DIR / "PIL").exists():
        print("Pillow not found, bootstrapping a local copy for WebP conversion...")
        run([
            sys.executable,
            "-m",
            "pip",
            "install",
            "--quiet",
            "--target",
            str(PILLOW_VENDOR_DIR),
            "pillow",
        ])

    if str(PILLOW_VENDOR_DIR) not in sys.path:
        sys.path.insert(0, str(PILLOW_VENDOR_DIR))
    importlib.invalidate_caches()

    try:
        from PIL import Image  # type: ignore
    except ImportError as exc:
        raise SystemExit(
            "WebP optimization requires Pillow. Auto-bootstrap failed; rerun with --skip-webp or install pillow."
        ) from exc
    return Image


def optimize_referenced_pages_to_webp(book_dir: Path, *, quality: int = 90) -> dict[str, Any]:
    Image = ensure_pillow()
    pages_path = book_dir / "pages.json"
    pages = read_json(pages_path)

    converted_count = 0
    old_total = 0
    new_total = 0

    for page in pages:
        for key in ("imageAsset", "imageUri"):
            value = page.get(key)
            if not isinstance(value, str) or not value.endswith(".png") or "://" in value:
                continue

            src = book_dir / value.lstrip("/")
            if not src.is_file():
                raise SystemExit(f"page image not found: {src}")

            dst_rel = value[:-4] + ".webp"
            dst = book_dir / dst_rel.lstrip("/")
            dst.parent.mkdir(parents=True, exist_ok=True)

            with Image.open(src) as image:
                save_image = image
                if image.mode not in ("RGB", "RGBA", "L", "LA"):
                    save_image = image.convert("RGB")
                save_image.save(dst, "WEBP", quality=quality, method=6)

            old_total += src.stat().st_size
            new_total += dst.stat().st_size
            page[key] = dst_rel
            src.unlink()
            converted_count += 1
            break

    pages_path.write_text(json.dumps(pages, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return {
        "convertedCount": converted_count,
        "oldTotalBytes": old_total,
        "newTotalBytes": new_total,
    }


def build_ingest_command(args: argparse.Namespace) -> list[str]:
    cmd = [
        sys.executable,
        str(INGEST_SCRIPT),
        "--pdf",
        str(args.pdf),
        "--mp3",
        str(args.mp3),
        "--level",
        args.level,
        "--version",
        args.version,
        "--cover-page",
        str(args.cover_page),
        "--render-max-dimension",
        str(args.render_max_dimension),
        "--ffprobe",
        args.ffprobe,
        "--output-root",
        str(args.output_root),
    ]
    if args.title:
        cmd.extend(["--title", args.title])
    if args.book_id:
        cmd.extend(["--book-id", args.book_id])
    if args.reading_pages:
        cmd.extend(["--reading-pages", args.reading_pages])
    if args.overwrite:
        cmd.append("--overwrite")
    for tag in args.tag:
        cmd.extend(["--tag", tag])
    return cmd


def build_publish_command(args: argparse.Namespace, book_id: str) -> list[str]:
    cmd = [
        sys.executable,
        str(PUBLISH_SCRIPT),
        book_id,
        "--mc-alias",
        args.mc_alias,
        "--bucket",
        args.bucket,
    ]
    if args.skip_verify:
        cmd.append("--skip-verify")
    return cmd


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
    parser.add_argument("--reading-pages", help="Zero-based PDF page indices or ranges, for example: 2-15 or 1,2,4-8")
    parser.add_argument("--render-max-dimension", type=int, default=1600, help="Longest pixel dimension for rendered pages")
    parser.add_argument("--output-root", type=Path, default=BOOKS_DIR, help="Root directory where source book packages live")
    parser.add_argument("--ffprobe", default="/usr/local/bin/ffprobe", help="ffprobe binary used to read audio duration")
    parser.add_argument("--skip-webp", action="store_true", help="Skip page image conversion from PNG to WebP")
    parser.add_argument("--publish", action="store_true", help="Upload and verify the generated content package")
    parser.add_argument("--skip-verify", action="store_true", help="Skip remote verification when used with --publish")
    parser.add_argument("--mc-alias", default="aqura", help="mc alias name, default: aqura")
    parser.add_argument("--bucket", default="english-reader", help="bucket name, default: english-reader")
    parser.add_argument("--overwrite", action="store_true", help="Replace an existing content/books/<bookId> directory")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    ingest_result = run(build_ingest_command(args), capture_output=True)
    print(ingest_result.stdout, end="")
    ingest_summary = json.loads(ingest_result.stdout)

    book_id = ingest_summary["bookId"]
    book_dir = Path(ingest_summary["outputDirectory"])

    webp_summary: dict[str, Any] | None = None
    if not args.skip_webp:
        webp_summary = optimize_referenced_pages_to_webp(book_dir)
        print(
            "webp optimization:",
            f"{webp_summary['convertedCount']} pages,",
            f"{webp_summary['oldTotalBytes'] / 1024 / 1024:.2f}MB -> {webp_summary['newTotalBytes'] / 1024 / 1024:.2f}MB",
        )

    package_path = DIST_DIR / f"{book_id}.zip"
    package_result = run(
        [sys.executable, str(PACKAGE_SCRIPT), str(book_dir), "--output", str(package_path)],
        capture_output=True,
    )
    print(package_result.stdout, end="")

    remote_package_url: str | None = None
    if args.publish:
        run(build_publish_command(args, book_id))
        config = read_json(CONTENT_DIR / "catalog-config.json")
        remote_package_url = f"{bucket_base_url_from_bookshelf(config['bookshelfUrl'])}/books/{book_id}/package.zip"

    print("\n=== add-book summary ===")
    print(f"book id      : {book_id}")
    print(f"content dir  : {book_dir}")
    print(f"package path : {package_path}")
    print(f"package size : {package_path.stat().st_size / 1024 / 1024:.2f} MB")
    if remote_package_url:
        print(f"remote url   : {remote_package_url}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
