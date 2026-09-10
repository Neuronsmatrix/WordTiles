#!/usr/bin/env python3
"""Build WordTiles' deterministic search-only WordNet 3.0 lemma asset."""

from __future__ import annotations

import argparse
import gzip
import io
from pathlib import Path
import tarfile


INDEX_NAMES = (
    "dict/index.noun",
    "dict/index.verb",
    "dict/index.adj",
    "dict/index.adv",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "archive",
        nargs="?",
        type=Path,
        default=Path(".toolchain/downloads/wordnet3.tar.gz"),
    )
    parser.add_argument(
        "output",
        nargs="?",
        type=Path,
        default=Path("app/src/main/assets/search/wordnet-3.0-lemmas.txt.gzip"),
    )
    return parser.parse_args()


def read_lemmas(archive: Path) -> list[str]:
    lemmas: set[str] = set()
    with tarfile.open(archive, "r:gz") as wordnet:
        for name in INDEX_NAMES:
            extracted = wordnet.extractfile(name)
            if extracted is None:
                raise RuntimeError(f"Missing {name} in {archive}")
            with io.TextIOWrapper(extracted, encoding="utf-8") as index:
                for line in index:
                    if not line or line[0].isspace():
                        continue
                    lemma = " ".join(line.split(None, 1)[0].replace("_", " ").split())
                    if lemma:
                        lemmas.add(lemma)
    return sorted(lemmas)


def write_deterministic_gzip(output: Path, lemmas: list[str]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as compressed:
            for lemma in lemmas:
                compressed.write(lemma.encode("utf-8") + b"\n")


def main() -> None:
    args = parse_args()
    lemmas = read_lemmas(args.archive)
    write_deterministic_gzip(args.output, lemmas)
    print(f"Wrote {len(lemmas)} unique lemmas to {args.output}")


if __name__ == "__main__":
    main()
