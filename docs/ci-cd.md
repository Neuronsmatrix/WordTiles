# CI and automatic Android releases

The [Android workflow](../.github/workflows/android.yml) runs on pull requests, pushes to `main`, and release-tag pushes. It uses GitHub's Ubuntu runner, JDK17, SDK35, and the checked-in Gradle wrapper. It does not require the ignored local `.toolchain` folder.

## Triggers

| Event | Result |
|---|---|
| Pull request opened or updated | Metadata tests, Kotlin/Android tests, lint, and debug build |
| Push to `main` | Same checks |
| Push `v0.2.0`, `v2`, or another numeric version tag | Checks, signed APK, then a published GitHub release |
| Push `v0.3.0-rc.1`, `PR`, or `PR-123` | Checks, signed APK, then a GitHub prerelease |
| Run workflow manually | Checks only |

“PR” here is a **Git tag**, not a pull-request label. Publishing requires a tag push. Fork pull requests receive neither signing secrets nor release-write permissions. The signing job has read-only repository permission; only the separate publishing job can create releases.

After this workflow is merged, tag the commit you want to distribute:

```sh
git tag v0.2.0
git push origin v0.2.0
```

For a test build:

```sh
git tag PR-123
git push origin PR-123
```

Use a new tag for each build. A rerun preserves an already published release and its assets. Failed uploads leave a draft that can be completed on rerun; a release becomes public only after both the APK and SHA-256 checksum are uploaded. Checks must pass before signing or publishing. Existing tags are not published retroactively.

## Signing

Configure these **repository Actions secrets** on `Neuronsmatrix/WordTiles`:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded persistent keystore.
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password.
- `ANDROID_KEY_ALIAS`: Signing key alias.
- `ANDROID_KEY_PASSWORD`: Signing key password.

Use the same signing key as the installed app to keep update compatibility and preserve its data. For the current personal build, that key is the existing `.toolchain/android-user-home/debug.keystore`; its alias is `androiddebugkey`, and its standard debug-key passwords are `android`. The release build itself is not debuggable. Keep this signing identity backed up: replacing it requires reinstalling the app. A different dedicated release key is supported if you choose to change signing identity.

The workflow decodes the key only in the signing job's temporary directory, never uploads it as an artifact, and removes it after building. No password is passed as a command-line argument. A release build fails clearly when signing configuration is missing; it does not silently publish an unsigned APK.

To configure the key yourself, authenticate `gh` and pass secret values via standard input. For example:

```sh
base64 -w 0 .toolchain/android-user-home/debug.keystore | gh secret set ANDROID_KEYSTORE_BASE64 --repo Neuronsmatrix/WordTiles
printf '%s' android | gh secret set ANDROID_KEYSTORE_PASSWORD --repo Neuronsmatrix/WordTiles
printf '%s' androiddebugkey | gh secret set ANDROID_KEY_ALIAS --repo Neuronsmatrix/WordTiles
printf '%s' android | gh secret set ANDROID_KEY_PASSWORD --repo Neuronsmatrix/WordTiles
```

## Versions and artifacts

The Android version name comes from the tag (`v0.2.0` → `0.2.0`; `PR-123` → `PR-123`). The Android version code is `1000 + GITHUB_RUN_NUMBER`, so subsequent runs of this workflow create increasing codes above the initial local builds. Keep the workflow's run-number history; a renamed/recreated workflow may need a larger offset. Reruns of one run retain its code.

Releases contain `WordTiles-<version>.apk` and a matching `.sha256` file, with automatically generated release notes. Prereleases do not become the Latest release. CI test/lint reports are retained for14 days and the intermediate signed bundle for7 days; published release assets remain attached to the release.

Local builds retain their default version unless `WORDTILES_VERSION_NAME` and/or `WORDTILES_VERSION_CODE` are supplied. After installing a CI build, give any local replacement a version code at least as high as the installed one.

For local signed release verification, set `ANDROID_SIGNING_STORE_FILE`, the three signing credential environment variables above, and run `./scripts/gradle-local :app:assembleRelease`. GitHub Releases distribute the APK directly; this pipeline does not upload to Google Play.
