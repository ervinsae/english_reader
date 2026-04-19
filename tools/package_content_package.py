#!/usr/bin/env python3
"""Validate and build a standard book content package zip."""

from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path


REQUIRED_FILES = ("book.json", "pages.json", "words.json")


def read_json(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def collect_referenced_paths(package_dir: Path) -> list[Path]:
    book = read_json(package_dir / "book.json")
    pages = read_json(package_dir / "pages.json")
    words = read_json(package_dir / "words.json")

    referenced: list[str] = []

    for key in ("coverAsset", "coverUri"):
        value = book.get(key)
        if isinstance(value, str) and value and "://" not in value:
            referenced.append(value)

    for page in pages:
        for key in ("imageAsset", "imageUri", "sentenceAudioAsset", "sentenceAudioUri"):
            value = page.get(key)
            if isinstance(value, str) and value and "://" not in value:
                referenced.append(value)

    for word in words:
        for key in ("audioAsset", "audioUri"):
            value = word.get(key)
            if isinstance(value, str) and value and "://" not in value:
                referenced.append(value)

    return [package_dir / rel_path.lstrip("/") for rel_path in referenced]


def validate_package(package_dir: Path) -> list[str]:
    errors: list[str] = []

    for filename in REQUIRED_FILES:
        if not (package_dir / filename).is_file():
            errors.append(f"missing required file: {filename}")

    if errors:
        return errors

    try:
        book = read_json(package_dir / "book.json")
    except json.JSONDecodeError as exc:
        errors.append(f"invalid book.json: {exc}")
        return errors

    book_id = str(book.get("id", "")).strip()
    title = str(book.get("title", "")).strip()
    if not book_id:
        errors.append("book.json is missing a non-empty id")
    if not title:
        errors.append("book.json is missing a non-empty title")

    for referenced_path in collect_referenced_paths(package_dir):
        if not referenced_path.is_file():
            errors.append(f"referenced file does not exist: {referenced_path.relative_to(package_dir)}")

    return errors


def build_package_file_list(package_dir: Path) -> list[Path]:
    referenced_files = collect_referenced_paths(package_dir)
    package_files = [package_dir / name for name in REQUIRED_FILES]

    deduped: list[Path] = []
    seen: set[Path] = set()
    for path in [*package_files, *referenced_files]:
        resolved = path.resolve()
        if resolved in seen:
            continue
        seen.add(resolved)
        deduped.append(path)
    return deduped


def write_zip(package_dir: Path, output_zip: Path) -> None:
    output_zip.parent.mkdir(parents=True, exist_ok=True)
    files_to_package = build_package_file_list(package_dir)
    with zipfile.ZipFile(output_zip, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in files_to_package:
            archive.write(path, path.relative_to(package_dir).as_posix())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package_dir", type=Path, help="Directory containing book.json/pages.json/words.json")
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Output zip path. Defaults to ./dist/<book-id>.zip",
    )
    args = parser.parse_args()

    package_dir = args.package_dir.resolve()
    if not package_dir.is_dir():
        print(f"package directory not found: {package_dir}", file=sys.stderr)
        return 1

    errors = validate_package(package_dir)
    if errors:
        print("package validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1

    book = read_json(package_dir / "book.json")
    output_zip = args.output or (Path.cwd() / "dist" / f"{book['id']}.zip")
    write_zip(package_dir=package_dir, output_zip=output_zip)
    print(output_zip)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
