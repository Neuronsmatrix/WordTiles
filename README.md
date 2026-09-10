# WordTiles

A native Android vocabulary app organized around topics and connections between words. All learning history, confidence calculations, graph navigation, and quiz grading run on your phone.

## Try it

**Explore** presents ten topic suggestions selected for the current local date. They stay fixed for the day, survive app restarts, and change tomorrow. Suggestions do not download definitions or add words to Collection. Open a topic and download its entries when you want to study it offline.

**Graph** is a separate tab for browsing topic and word connections. Topic and dictionary pages link to the corresponding neighborhood there.

**Find** offers offline spelling suggestions for words and phrases, including `loook for` → `look for`. Choose a suggestion to fetch its dictionary entry, or explicitly look up the exact text. Suggestions use a bundled headword index plus locally available words and relations, rather than making external search requests.

Use **Save word** or **Star word** on an entry to keep it in **Collection**. All, Saved, and Starred filters distinguish membership; each mark can be removed independently. Merely looking up or downloading a word does not add it. Rating a word adds it to Saved. Existing v1 studied words migrate to Saved while their confidence and cached entries remain intact.

In a study session, **tap the card to flip it** and reveal all meanings. Tap again to return to the word. Confidence buttons are pinned above the card; they become available after reveal. Ratings save immediately. Collection Review includes saved or starred words, including ones you have not rated yet. **Practice** retains the curated multi-answer quiz set.

## Build and install

The Linux x86_64 toolchain lives in the ignored **`.toolchain/`** folder and survives reboot. It contains JDK 17, SDK 35, build tools 35.0.0, Gradle distributions/caches, and Android tool preferences. No `/tmp` paths are needed. The setup script reuses compatible installed SDK components and downloads only what is missing:

```sh
./scripts/setup-toolchain "$HOME/Hidden/Android/SDK"
./scripts/gradle-local :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
.toolchain/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The SDK argument is optional; setup checks `ANDROID_HOME` and then `~/Hidden/Android/SDK`. Setup copies compatible components into `.toolchain/android-sdk`, and writes the ignored `local.properties` for Android Studio. For Studio, use `.toolchain/jdk` as the Gradle JDK. Other hosts can use their own JDK 17 and SDK 35 with the standard Gradle wrapper.

The original developer debug key is copied into the ignored toolchain when available, so version **0.2.0** can update the first APK without uninstalling it. The APK is `app/build/outputs/apk/debug/app-debug.apk` and runs on Android **8.0 (API 26)** and later. You can also copy it to your phone and open it to sideload. This is a development build, not a release signing setup.

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

- **Review:** up to 10 saved or starred, downloaded, non-excluded words. Unrated collected words are introduced first, followed by the weakest reviewed words.
- **Pure learn:** up to 10 unstudied, downloaded words in the chosen topic.
- **Review + new topic:** the review batch followed by the new-word batch.

Each word appears at most once per session. Ratings save immediately; skipping does not change progress. A revealed card and its queue survive screen rotation. After process termination, completed ratings remain saved, while the session itself starts afresh. Excluding a word removes it from session selection without deleting its entry or learning history.

## Dictionary content

Dictionary data is provided by [FreeDictionaryAPI.com](https://freedictionaryapi.com/), using `https://freedictionaryapi.com/api/v1/entries/en/…`. No API key is needed. Requests are made directly from the phone on lookup or a topic download; WordTiles has no server. A failed request does not discard cached content. Downloads use HTTPS, timeouts, cancellation checks, and a 2 MiB response limit.

Each response's source URLs and content license are retained and displayed with the entry. Dictionary content is governed by those source licenses, separately from application code. The provider serves [Wiktionary](https://en.wiktionary.org/) content under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Original source links and provider attribution are shown with each entry. Its documented allowance is 1,000 requests per hour per IP; WordTiles downloads one topic at a time. WordTiles does not scrape Merriam-Webster or Oxford.

The bundled catalogue contains **20 topics**, with some words shared between topics; ten are suggested each day. It emphasizes areas such as argument, inquiry, change, literary tone, and complexity. Topic labels, lists, and the six quiz prompts are locally authored. These are starting points for graph exploration, **not a complete ranked 10,000-word collection**. Dictionary coverage, examples, and synonym availability vary; a particular phrase may be missing. Automatic semantic clustering and automatically generated quizzes are not implemented. Valid quiz alternatives are explicitly checked in context.

## Offline search data

Spelling suggestions use **147,306 WordNet 3.0 headwords and phrases**, bundled as a roughly 600 KB gzip. This is a suggestion index, not imported flashcards or a saved vocabulary collection. Definitions are still fetched from the dictionary when you choose an entry. Local cached words and relations extend the index during search.

WordNet 3.0 is copyright 2006 Princeton University; its complete [license](licenses/wordnet-license.txt) is preserved beside the bundled data and in this repository. The [source and derivation record](app/src/main/assets/search/SOURCE.txt) and `scripts/build-search-lexicon.py` document reproducible generation. Search indexing and ranking run off the UI thread; no remote spelling service is used.

## Project structure

- `core/`: immutable dictionary models, complete-response parsing, confidence scheduler, session selection, and quiz grading.
- `app/.../data/`: SQLite storage, dictionary HTTP client, repository, catalogue, and quiz prompts.
- `app/.../ui/`: Compose screens and lifecycle-owned UI/session state.
- `docs/superpowers/`: agreed design, implementation plan, and execution record.

SQLite stores cached dictionary entries, confidence, and saved/starred membership separately, keyed by normalized words. Daily topic IDs live in a separate metadata row. No account, analytics, cloud synchronization, or background scraping is included. Android backup and device transfer are disabled for app data. Uninstalling the app removes the local collection; an export/restore feature is not included in this version.

## Verification

Verified on 2026-09-07: **81 tests passed**, the version 0.2.0 APK built successfully, and Android lint reported **zero errors**. The suite covers migration, save/star membership, daily selection, offline fuzzy search, and card-flip/navigation regressions. Nonblocking dependency-update notices remain.

The project includes pure Kotlin tests for decay, ordering, session isolation, and quiz grading; parser fixtures for multiple meanings and source metadata; Android database/repository tests; and Compose screen-flow tests under Robolectric. The screen tests exercise full-definition reveal, rotation, persisted ratings, skipping, and multi-answer quizzes. Android lint checks platform compatibility.

Live source checks confirmed an advanced entry with multiple meanings and relations. The original provisional provider, dictionaryapi.dev, timed out on that entry; the implemented default uses FreeDictionaryAPI.com instead.

The toolchain is now project-local and ignored by git. Physical-device testing remains useful in addition to the JVM and Robolectric checks. Association pictures are intentionally deferred; a future update can attach them to specific meanings.

## CI and releases

GitHub Actions checks pull requests and pushes to `main`. Pushing a version tag such as `v0.2.0` publishes a signed APK after tests and lint pass; `PR-*` and release-candidate tags publish prereleases. See [CI/CD setup and signing](docs/ci-cd.md) for triggers, signing secrets, and versioning.
