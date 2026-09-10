#!/usr/bin/env python3
"""Validate a release tag and provide safe values to GitHub Actions."""

import os
import re
from pathlib import Path

VERSION_TAG = re.compile(r"v(?:0|[1-9][0-9]*)(?:\.(?:0|[1-9][0-9]*)){0,2}(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?")
PREVIEW_TAG = re.compile(r"PR(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?")


def release_metadata(tag: str, run_number: str) -> dict[str, str]:
    if not (VERSION_TAG.fullmatch(tag) or PREVIEW_TAG.fullmatch(tag)):
        raise ValueError("Use a version tag such as v0.2.0/v2, or a prerelease tag such as PR-123.")
    if not re.fullmatch(r"[1-9][0-9]*", run_number):
        raise ValueError("GITHUB_RUN_NUMBER must be a positive integer.")
    code = 1000 + int(run_number)
    if code > 2_100_000_000:
        raise ValueError("Generated version code exceeds Android's supported range.")
    version = tag[1:] if tag.startswith("v") else tag
    return {
        "tag": tag,
        "version_name": version,
        "version_code": str(code),
        "prerelease": str(tag.startswith("PR") or "-" in version).lower(),
        "apk_name": f"WordTiles-{version}.apk",
    }


def main() -> None:
    values = release_metadata(os.environ["GITHUB_REF_NAME"], os.environ["GITHUB_RUN_NUMBER"])
    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
        for key, value in values.items():
            output.write(f"{key}={value}\n")


if __name__ == "__main__":
    main()
