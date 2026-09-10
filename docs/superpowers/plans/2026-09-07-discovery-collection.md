# Discovery and collection implementation plan

> Use superpowers:subagent-driven-development for independent data/search tasks; root integrates the UI and verifies delivery.

**Goal:** Implement the user's daily discovery, explicit collection, fuzzy search, and card interaction changes with a persistent project toolchain.

**Architecture:** Existing core/app separation. Add pure local suggestion logic, SQLite membership/daily state, and update Compose navigation and sessions. Reuse confidence scheduling and dictionary retrieval.

**Tech stack:** Existing Kotlin 2.1.20, Compose, SQLite, JDK17, SDK35, Gradle8.11.1.

**Spec:** `docs/superpowers/specs/2026-09-07-discovery-collection-design.md`

## Global constraints

Android API26+, compile/target35; local-only learning/search; no images; no cloud/account changes; preserve existing learning history and debug signing identity. Work in the user-specified folder; `.git` metadata is read-only, so do not create commits/branches/worktrees.

## Tasks

- [x] Toolchain: recreate ignored `.toolchain/jdk`, `android-sdk`, `gradle-home`, `android-user-home`; scripts `setup-toolchain` and `gradle-local`. Verify `./scripts/gradle-local --version` and git ignore matching. Keep downloads local and retain existing debug key.
- [x] Data: add `CollectionMark(saved:Boolean=false,starred:Boolean=false)`, `DailyTopics(day:String,topicIds:List<String>)`, v2 migration, repository mark APIs and daily snapshot. Tests start from real v1 SQLite data, verify all legacy records survive and only studied words migrate. Verify cache-only words never get marks; flags independently survive reopen.
- [x] Daily topics: expand Catalog to at least20 topics; deterministic per-day seeded order, persist ten IDs, distinct per day and stable on reload. Test fixed date/next date/catalogue changes with hand-checked invariants.
- [x] Search: bundled licensed WordNet headwords and phrases; pure `FuzzySearchIndex.suggest(query,limit)` with tests for loook for, transpositions, prefix completion, no results, ordering and limits. Android lexicon loader runs on IO; no external search request.
- [x] UI: Explore list of daily topics with search results; Graph tab; save/star controls; Collection filters/counts/review from membership; tap flip animation and confidence above meanings. Generation-safe ViewModel search; local-day refresh on resume/minute tick. Update Compose regressions to new behavior and add migration/search UI checks.
- [x] Review all changed behavior and correct material findings. Run `./scripts/gradle-local :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. Update README and provide APK with verified results.

## Execution record

The user explicitly requested these changes; implement within that scope without another design-approval round. Daily topic suggestions use the literal topic interpretation announced to the user; no correction was supplied. Version1 toolchain was removed by reboot and must be reinstalled, not moved. Existing app sources are clean at commit `7cad132` before work. Parallel tasks share model signatures; agents own separate source files; root owns ViewModel/UI/build scripts.


## Decisions and review record

- SDK correction: reused platform tools and licenses from `~/Hidden/Android/SDK` (which has platform/build-tools36.1), installed missing35 components locally, and preserved the existing debug key. `./scripts/gradle-local --version` resolves JDK and Gradle from `.toolchain/`; ignore matching and key equality verified.
- Collection filters are All, Saved and Starred. Saved tests only `saved`, Starred tests only `starred`; either mark qualifies for global collection review. Existing v1 studied words migrate to Saved.
- Topic selection uses date-seeded shuffle; no cyclic catalogue windows. Stable daily IDs live outside entries and collection marks.
- Search bundles 147,306 WordNet lemmas and source/license records. Android automatically expands `.gz` assets; use `.gzip` to retain the compressed data required by the loader. Packaged-asset tests reproduced the failure and verify loading through the actual Android asset manager.
- Corpus preparation, lexicon loading, and matching all run off Main. A generation token discards stale results. The Compose search test pumps UI work before checking background completion; debounce has independent virtual-clock coverage.
- Independent review confirmed the collection predicate fix, flip accessibility labels, and off-thread corpus preparation. Pictures remain deferred.

## Final verification

On 2026-09-07, `./scripts/gradle-local :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` completed successfully. 42 core tests and 39 app/Robolectric tests passed (81 total, zero failures/errors). APK metadata reports versionCode2/versionName0.2.0. `git diff --check` passed, `.toolchain/` ignore matching passed, and copied developer signing key matches the preexisting key. Hardware-device testing was not performed.
