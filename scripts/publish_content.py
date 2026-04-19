#!/usr/bin/env python3
"""Package, upload, and verify English Reader remote content via mc."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
BOOKS_DIR = ROOT / "content" / "books"
CONTENT_DIR = ROOT / "app" / "src" / "main" / "assets" / "content"
DIST_DIR = ROOT / "dist"
MINIO_DIST_DIR = DIST_DIR / "minio"
PACKAGE_SCRIPT = ROOT / "tools" / "package_content_package.py"


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def run(cmd: list[str], *, dry_run: bool = False, capture_output: bool = False) -> subprocess.CompletedProcess[str]:
    printable = " ".join(cmd)
    print(f"$ {printable}")
    if dry_run:
        return subprocess.CompletedProcess(cmd, 0, "", "")
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
        raise ValueError(f"bookshelfUrl does not end with {marker}: {bookshelf_url}")
    return bookshelf_url[: -len(marker)]


def ensure_tool_exists(name: str) -> None:
    if shutil.which(name):
        return
    raise SystemExit(f"missing required command: {name}")


def build_remote_book_entry(book_dir: Path, bucket_base_url: str) -> tuple[dict[str, Any], Path, str]:
    book = read_json(book_dir / "book.json")
    book_id = str(book.get("id", "")).strip()
    title = str(book.get("title", "")).strip()
    level = str(book.get("level", "")).strip()
    version = str(book.get("version", "")).strip()
    page_count = int(book.get("pageCount") or 0)
    tags = list(book.get("tags") or [])
    cover_asset = str(book.get("coverAsset") or "cover.png").strip()

    if not book_id:
        raise SystemExit(f"book.json missing id: {book_dir}")
    if not title:
        raise SystemExit(f"book.json missing title: {book_dir}")
    if not version:
        raise SystemExit(f"book.json missing version: {book_dir}")
    if not page_count:
        raise SystemExit(f"book.json missing pageCount: {book_dir}")

    zip_path = DIST_DIR / f"{book_id}.zip"
    run([sys.executable, str(PACKAGE_SCRIPT), str(book_dir), "--output", str(zip_path)])
    sha = sha256_of(zip_path)

    cover_path = book_dir / cover_asset.lstrip("/")
    if not cover_path.is_file():
        raise SystemExit(f"cover asset not found: {cover_path}")

    remote_prefix = f"{bucket_base_url}/books/{book_id}"
    entry = {
        "bookId": book_id,
        "title": title,
        "level": level,
        "pageCount": page_count,
        "coverUri": f"{remote_prefix}/cover.png",
        "tags": tags,
        "package": {
            "version": version,
            "downloadUrl": f"{remote_prefix}/package.zip",
            "sha256": sha,
        },
    }
    return entry, cover_path, sha


def build_global_files(selected_book_ids: list[str]) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]], str]:
    config = read_json(CONTENT_DIR / "catalog-config.json")
    bucket_base_url = bucket_base_url_from_bookshelf(config["bookshelfUrl"])
    local_bookshelf = read_json(CONTENT_DIR / "bookshelf.json")
    existing_books = list(local_bookshelf.get("books") or [])
    existing_by_id = {str(item["bookId"]): item for item in existing_books}

    selected_info: dict[str, dict[str, Any]] = {}
    for book_id in selected_book_ids:
        book_dir = BOOKS_DIR / book_id
        if not book_dir.is_dir():
            raise SystemExit(f"book directory not found: {book_dir}")
        entry, cover_path, sha = build_remote_book_entry(book_dir, bucket_base_url)
        selected_info[book_id] = {
            "entry": entry,
            "cover_path": cover_path,
            "zip_path": DIST_DIR / f"{book_id}.zip",
            "sha": sha,
        }
        existing_by_id[book_id] = entry

    ordered_books: list[dict[str, Any]] = []
    seen: set[str] = set()
    for item in existing_books:
        book_id = str(item["bookId"])
        if book_id in existing_by_id:
            ordered_books.append(existing_by_id[book_id])
            seen.add(book_id)
    for book_id in selected_book_ids:
        if book_id not in seen:
            ordered_books.append(existing_by_id[book_id])
            seen.add(book_id)

    return ordered_books, selected_info, bucket_base_url


def stage_files(books: list[dict[str, Any]], selected_info: dict[str, dict[str, Any]]) -> tuple[Path, Path]:
    MINIO_DIST_DIR.mkdir(parents=True, exist_ok=True)
    bookshelf_payload = {"books": books}
    catalog_payload = {
        "packages": [
            {
                "bookId": item["bookId"],
                "title": item["title"],
                "version": item["package"]["version"],
                "downloadUrl": item["package"]["downloadUrl"],
                "sha256": item["package"]["sha256"],
            }
            for item in books
            if item.get("package")
        ]
    }
    bookshelf_path = MINIO_DIST_DIR / "catalog" / "bookshelf.json"
    catalog_path = MINIO_DIST_DIR / "catalog" / "catalog.json"
    write_json(bookshelf_path, bookshelf_payload)
    write_json(catalog_path, catalog_payload)

    for book_id, info in selected_info.items():
        book_dir = MINIO_DIST_DIR / "books" / book_id
        book_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(info["cover_path"], book_dir / "cover.png")
        shutil.copy2(info["zip_path"], book_dir / "package.zip")
        per_book_catalog = {
            "packages": [
                {
                    "bookId": info["entry"]["bookId"],
                    "title": info["entry"]["title"],
                    "version": info["entry"]["package"]["version"],
                    "downloadUrl": info["entry"]["package"]["downloadUrl"],
                    "sha256": info["entry"]["package"]["sha256"],
                }
            ]
        }
        write_json(book_dir / "catalog.json", per_book_catalog)

    return bookshelf_path, catalog_path


def upload_files(alias: str, bucket: str, selected_book_ids: list[str], *, dry_run: bool) -> None:
    target_prefix = f"{alias}/{bucket}"
    run(["mc", "cp", str(MINIO_DIST_DIR / "catalog" / "bookshelf.json"), f"{target_prefix}/catalog/bookshelf.json"], dry_run=dry_run)
    run(["mc", "cp", str(MINIO_DIST_DIR / "catalog" / "catalog.json"), f"{target_prefix}/catalog/catalog.json"], dry_run=dry_run)

    for book_id in selected_book_ids:
        local_book_dir = MINIO_DIST_DIR / "books" / book_id
        remote_book_prefix = f"{target_prefix}/books/{book_id}"
        run(["mc", "cp", str(local_book_dir / "cover.png"), f"{remote_book_prefix}/cover.png"], dry_run=dry_run)
        run(["mc", "cp", str(local_book_dir / "catalog.json"), f"{remote_book_prefix}/catalog.json"], dry_run=dry_run)
        run(["mc", "cp", str(local_book_dir / "package.zip"), f"{remote_book_prefix}/package.zip"], dry_run=dry_run)


def verify_url(url: str, *, range_probe: bool = False) -> None:
    headers = {"Range": "bytes=0-0"} if range_probe else {}
    methods = ["HEAD"] if not range_probe else []
    methods.append("GET")

    last_error: Exception | None = None
    for method in methods:
        try:
            request = urllib.request.Request(url, method=method, headers=headers)
            with urllib.request.urlopen(request, timeout=30) as response:
                if response.status >= 400:
                    raise RuntimeError(f"HTTP {response.status} for {url}")
                if method == "GET":
                    response.read(1)
            print(f"verified {url}")
            return
        except urllib.error.HTTPError as exc:
            last_error = exc
            if method == "HEAD" and exc.code in {403, 405}:
                continue
            raise SystemExit(f"verification failed: {url} -> HTTP {exc.code}") from exc
        except Exception as exc:
            last_error = exc
            if method == "HEAD":
                continue
            raise SystemExit(f"verification failed: {url} -> {exc}") from exc

    raise SystemExit(f"verification failed: {url} -> {last_error}")


def verify_remote(books: list[dict[str, Any]], selected_book_ids: list[str], *, skip_verify: bool) -> None:
    if skip_verify:
        return
    config = read_json(CONTENT_DIR / "catalog-config.json")
    bookshelf_url = config["bookshelfUrl"]
    verify_url(bookshelf_url)
    verify_url(bookshelf_url.rsplit("/", 1)[0] + "/catalog.json")

    books_by_id = {item["bookId"]: item for item in books}
    for book_id in selected_book_ids:
        item = books_by_id[book_id]
        verify_url(item["coverUri"])
        verify_url(item["package"]["downloadUrl"], range_probe=True)
        verify_url(item["package"]["downloadUrl"].rsplit("/", 1)[0] + "/catalog.json")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("book_ids", nargs="+", help="One or more book ids to package and upload")
    parser.add_argument("--mc-alias", default="aqura", help="mc alias name, default: aqura")
    parser.add_argument("--bucket", default="english-reader", help="bucket name, default: english-reader")
    parser.add_argument("--dry-run", action="store_true", help="Print planned mc commands without uploading")
    parser.add_argument("--skip-verify", action="store_true", help="Skip remote URL verification after upload")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    ensure_tool_exists("mc")
    books, selected_info, _bucket_base_url = build_global_files(args.book_ids)
    bookshelf_path, catalog_path = stage_files(books, selected_info)
    print(f"staged {bookshelf_path}")
    print(f"staged {catalog_path}")
    upload_files(args.mc_alias, args.bucket, args.book_ids, dry_run=args.dry_run)
    verify_remote(books, args.book_ids, skip_verify=args.skip_verify or args.dry_run)
    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
