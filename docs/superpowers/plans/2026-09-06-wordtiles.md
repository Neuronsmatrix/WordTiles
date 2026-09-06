# WordTiles Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task by task. Track completed work here.

**Goal:** Build a usable native Android vocabulary app with local graph browsing, automatic dictionary lookup, full-entry cards, and the agreed confidence scheduler.

**Architecture:** A pure Kotlin `core` module owns learning records, selection, sessions, quiz grading, and dictionary parsing. The Android `app` module owns SQLite persistence, HTTP retrieval, lifecycle state, and Compose screens. The phone stores all progress and downloaded entries.

**Tech Stack:** Kotlin 2.1.20, AGP 8.9.2, Gradle 8.11.1, Compose BOM 2025.04.01, SQLiteOpenHelper, kotlinx.serialization JSON, JUnit 4, Java 17.

**Spec:** `docs/superpowers/specs/2026-09-06-wordtiles-design.md`

## Global constraints

- Minimum Android API 26; compile and target API 35; Java 17 bytecode.
- No application backend, account, analytics, or cloud synchronization.
- All returned dictionary definitions remain visible on reveal.
- Confidence belongs to a normalized word, not a topic or individual sense.
- Strength scale: 1, 10, 20, 40, 100; decay: one per elapsed 24 hours; session jitter: at most ±0.5.
- Initial catalogue and validated quiz collection must be described honestly as starter content.

## Files and interfaces

- `core/src/main/kotlin/com/wordtiles/core/Models.kt`: WordEntry, Meaning, Definition, Source, Topic, Progress, canonicalWord.
- `core/src/main/kotlin/com/wordtiles/core/ReviewScheduler.kt`: strengthAt, rate, reviewOrder, buildSession; SessionMode and StudyCard.
- `core/src/main/kotlin/com/wordtiles/core/Quiz.kt`: QuizQuestion, QuizOption, QuizResult, grade.
- `core/src/main/kotlin/com/wordtiles/core/DictionaryParser.kt`: parseDictionary(json: String): WordEntry preserving full entry and attribution.
- `app/src/main/java/com/wordtiles/data/`: catalogue, SQLite store, repository, dictionary client.
- `app/src/main/java/com/wordtiles/ui/`: lifecycle state and Compose screens.
- `README.md`: installation, content provenance, behavior, and validation.

### Task 1: Pure Kotlin learning engine and executable build

- [x] Create Gradle wrapper/build configuration with independently runnable `core:test`.
- [x] Write scheduler tests before implementing. Literal checks include strength 10 after rating 2; strength 7.5 after 2.5 days; strength -2 after rating 1 and 3 days; a future timestamp keeps strength 10; records shared across two topics appear once.
- [x] Observe the failing tests, then implement deterministic pure functions and a priority queue. The selection contract is:

```kotlin
fun strengthAt(progress: Progress, nowMillis: Long): Double
fun rate(word: String, rating: Int, nowMillis: Long, previous: Progress? = null): Progress
fun reviewOrder(progress: Collection<Progress>, nowMillis: Long, seed: Int): List<String>
fun buildSession(mode: SessionMode, topicWords: List<String>, availableWords: Set<String>, progress: Collection<Progress>, nowMillis: Long, seed: Int, limit: Int = 10): List<StudyCard>
```

- [x] Test pure learn exclusion, global reviews before topic introductions, absent downloads, duplicate spelling/case, and one appearance per session.
- [x] Test quiz grading with correct `{seek, look for}`, selection `{seek}` missing `look for`, and `{seek, search}` reporting both missing and extra options.
- [x] Run `./gradlew :core:test` and review results.

### Task 2: Complete dictionary entries and local persistence

- [x] Define immutable entry models and serialization; construct fixtures with multiple parts of speech and multiple homographs.
- [x] Write parser tests asserting all definitions, examples, sense synonyms, broader meaning synonyms, source URLs, and licenses survive parsing. Reject no-definition and error responses.
- [x] Implement `parseDictionary(json: String): WordEntry`, with limits at the HTTP boundary and explicit missing-entry exceptions.
- [x] Implement SQLite tables `entries(word PRIMARY KEY, payload)` and `progress(word PRIMARY KEY, strength, rated_at, reviews)`, plus excluded words. Upsert progress without replacing entry content.
- [x] Implement an HTTPS dictionary client and repository: cache-first lookup, explicit refresh, bounded sequential topic download with progress; coroutine I/O dispatching and recoverable errors.
- [x] Ship approximately 8 curated topic lists emphasizing less frequent vocabulary, with overlapping words and phrases. Ship a small separate original quiz collection.
- [x] Run core parser tests and Android persistence tests where supported.

### Task 3: Graph, complete entries, sessions, and quizzes

- [x] Build Explore with topic overview, local word graph limited to 12 nodes/page, equivalent list, search, topic download status, and mode selection.
- [x] Build entry detail showing every returned definition, example, relation type, attribution, and current confidence. Relation selection navigates to another word.
- [x] Build sessions with stable queues in ViewModel, reveal gate, 1–5 ratings, phase/progress, immediate persistence, skip, completion, and back behavior.
- [x] Build validated multi-select quizzes with explanation and explicit target-word self-rating after reveal. No synonym-generated answer keys.
- [x] Build collection/progress with learned count, downloaded count, weakest entries, and review-only launch.
- [x] Run `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` and fix verified failures.

### Task 4: Review, documentation, and delivery

- [x] Review all changes against the spec for lost meanings, duplicate learning records, thread blocking, session double rating, and cache loss.
- [x] Resolve material findings and rerun affected checks.
- [x] Write README with exact build/install commands, provider attribution, initial content limits, and verified check results.
- [x] Report the debug APK location if built, and any remaining environment or content limitations.

## Execution record

The shared workspace is empty and its `.git` directory is externally managed/read-only and is not recognized as a repository. Work in place; no branch, worktree, or commits can be created here. The user approved the design and proceeding; execute without another approval gate. Selected FreeDictionaryAPI.com after verifying its advanced-word and phrasal-verb responses; the provisional provider returned HTTP 522.

## Verified delivery

Final integrated check: `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` succeeded on 2026-09-06. 32 core tests and 19 Android/Robolectric tests passed. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Lint has no errors; remaining notices concern newer dependency versions. Physical-device verification remains outside the available environment.
