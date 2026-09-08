# WordTiles

A native Android vocabulary app organized around topics and connections between words. All learning history, confidence calculations, graph navigation, and quiz grading run on your phone.

## Try it

Open a topic in **Explore**, select **Download topic**, then choose **Pure learn** or **Review + new topic**. A word card reveals every meaning returned by the dictionary. Rate your confidence from 1 to 5 to add or update that word in your review collection. Downloaded cards work offline.

Tap a graph node to open a word. Its dictionary entry includes local synonym/opposite neighborhoods when those relationships are available. Follow a connection to download and explore another word. A word shared by several topics has one confidence record. Search accepts both words and phrases; unsupported expressions produce an explicit missing-entry message.

**Collection** shows your weakest studied words and all downloaded entries. **Practice** has six curated multi-select questions with explanations. Quiz answers alone never alter confidence; rating the question's target word is optional.

## Build and install

Requires JDK 17 and the Android SDK with platform **35** and build tools **35.0.0**. The project includes the Gradle 8.11.1 wrapper and its distribution checksum. Open this directory in Android Studio, select a JDK 17 Gradle runtime, install the requested SDK components, and run the `app` configuration.

From a terminal with `JAVA_HOME` and `ANDROID_HOME` configured (or the SDK path in `local.properties`):

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK runs on Android **8.0 (API 26)** and later. For sideloading, copy `app/build/outputs/apk/debug/app-debug.apk` to your phone and open it. It uses the local development signing key; a release signing setup is not included.

## Confidence and sessions

| Rating | Reset strength |
|---|---:|
| 1 | 1 |
| 2 | 10 |
| 3 | 20 |
| 4 | 40 |
| 5 | 100 |

`current strength = reset strength − elapsed days since rating`

Days are elapsed 24-hour periods, including fractions. Clock rollback cannot increase strength. Negative values preserve overdue ordering. Review selection uses a priority queue with a fixed random offset within ±0.5 per word per session. Randomness affects selection, never saved strength. Reviews select the weakest available words even while every strength is above zero.

- **Review:** up to 10 previously studied, downloaded, non-excluded words from the whole collection.
- **Pure learn:** up to 10 unstudied, downloaded words in the chosen topic.
- **Review + new topic:** the review batch followed by the new-word batch.

Each word appears at most once per session. Ratings save immediately; skipping does not change progress. A revealed card and its queue survive screen rotation. After process termination, completed ratings remain saved, while the session itself starts afresh. Excluding a word removes it from session selection without deleting its entry or learning history.

## Dictionary content

Dictionary data is provided by [FreeDictionaryAPI.com](https://freedictionaryapi.com/), using `https://freedictionaryapi.com/api/v1/entries/en/…`. No API key is needed. Requests are made directly from the phone on lookup or a topic download; WordTiles has no server. A failed request does not discard cached content. Downloads use HTTPS, timeouts, cancellation checks, and a 2 MiB response limit.

Each response's source URLs and content license are retained and displayed with the entry. Dictionary content is governed by those source licenses, separately from application code. The provider serves [Wiktionary](https://en.wiktionary.org/) content under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Original source links and provider attribution are shown with each entry. Its documented allowance is 1,000 requests per hour per IP; WordTiles downloads one topic at a time. WordTiles does not scrape Merriam-Webster or Oxford.

The bundled catalogue contains **8 starter topics and 80 topic memberships**, with some words shared between topics. It emphasizes areas such as argument, inquiry, change, literary tone, and complexity. Topic labels, lists, and the six quiz prompts are locally authored. These are starting points for graph exploration, **not a complete ranked 10,000-word collection**. Dictionary coverage, examples, and synonym availability vary; a particular phrase may be missing. Automatic semantic clustering and automatically generated quizzes are not implemented. Valid quiz alternatives are explicitly checked in context.

## Project structure

- `core/`: immutable dictionary models, complete-response parsing, confidence scheduler, session selection, and quiz grading.
- `app/.../data/`: SQLite storage, dictionary HTTP client, repository, catalogue, and quiz prompts.
- `app/.../ui/`: Compose screens and lifecycle-owned UI/session state.
- `docs/superpowers/`: agreed design, implementation plan, and execution record.

SQLite stores dictionary entries and confidence separately, keyed by normalized words. No account, analytics, cloud synchronization, or background scraping is included. Android backup and device transfer are disabled for app data. Uninstalling the app removes the local collection; an export/restore feature is not included in this version.

## Verification

Verified on 2026-09-06: **51 tests passed**, debug APK built, and Android lint reported **zero errors**. Lint notices about newer dependency versions remain.

The project includes pure Kotlin tests for decay, ordering, session isolation, and quiz grading; parser fixtures for multiple meanings and source metadata; Android database/repository tests; and Compose screen-flow tests under Robolectric. The screen tests exercise full-definition reveal, rotation, persisted ratings, skipping, and multi-answer quizzes. Android lint checks platform compatibility.

Live source checks confirmed an advanced entry with multiple meanings and relations. The original provisional provider, dictionaryapi.dev, timed out on that entry; the implemented default uses FreeDictionaryAPI.com instead.

The initial implementation was built using temporary JDK/SDK/Gradle installations outside the project. Those machine-specific paths are not required by the project. Hardware-device testing is still needed before relying on the app for a long-term personal collection.


## CI and releases

GitHub Actions checks pull requests and pushes to `main`. Pushing a version tag such as `v0.2.0` publishes a signed APK after tests and lint pass; `PR-*` and release-candidate tags publish prereleases. See [CI/CD setup and signing](docs/ci-cd.md) for triggers, signing secrets, and versioning.
