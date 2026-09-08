import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "release_metadata.py"
spec = importlib.util.spec_from_file_location("release_metadata", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ReleaseMetadataTest(unittest.TestCase):
    def test_stable_tag_sets_android_version_and_monotonic_code(self):
        self.assertEqual(module.release_metadata("v0.2.0", "12"), {
            "tag": "v0.2.0", "version_name": "0.2.0", "version_code": "1012",
            "prerelease": "false", "apk_name": "WordTiles-0.2.0.apk",
        })

    def test_short_version_tags_match_existing_repository_convention(self):
        self.assertEqual(module.release_metadata("v2", "1")["version_name"], "2")

    def test_preview_and_version_suffix_tags_are_prereleases(self):
        for tag in ("PR", "PR-123", "PR-search", "v0.3.0-rc.1"):
            with self.subTest(tag=tag):
                self.assertEqual(module.release_metadata(tag, "1")["prerelease"], "true")

    def test_rejects_tags_that_could_change_paths_or_inject_output(self):
        for tag in ("feature", "v", "v01.2", "PR/123", "PR-../../x", "v1;echo x", "PR\nfoo=bar", "--help"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                module.release_metadata(tag, "1")

    def test_rejects_invalid_or_overflowing_version_codes(self):
        for number in ("0", "-1", "x", "1\nfoo=bar", "2100000000"):
            with self.subTest(number=number), self.assertRaises(ValueError):
                module.release_metadata("v2", number)

    def test_command_appends_github_outputs(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            output.write_text("existing=value\n")
            subprocess.run([sys.executable, str(SCRIPT)], check=True, env={
                **os.environ, "GITHUB_REF_NAME": "PR-42", "GITHUB_RUN_NUMBER": "3", "GITHUB_OUTPUT": str(output),
            })
            self.assertEqual(output.read_text(), "existing=value\ntag=PR-42\nversion_name=PR-42\nversion_code=1003\nprerelease=true\napk_name=WordTiles-PR-42.apk\n")


if __name__ == "__main__":
    unittest.main()
