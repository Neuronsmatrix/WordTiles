# WordTiles design

Agreed direction: native Android vocabulary learning for one person, with a navigable local relationship graph, complete dictionary entries, and confidence-based review. The user approved proceeding on 2026-09-06.

## Learning and vocabulary

The entry view reveals all definitions returned by the dictionary, grouped by part of speech, including examples and attribution. A single learning record belongs to a normalized word or phrase, irrespective of topic membership. Connections may refer to a particular sense; broad topic membership is distinct from synonymy. No compulsory beginner curriculum or manual deck import.

Provide an immediately browsable catalogue of advanced topics and their vocabulary. Fetch dictionary entries on the device when requested, cache successful results, and allow offline review of downloaded words. Unavailable entries must be identified without inventing definitions. The initial catalogue is a starting collection, not a claim of a complete ranked 10,000-word corpus. Users can expand neighborhoods through dictionary relations and search for additional expressions. A replaceable provider boundary permits changing dictionary sources.

Initial provider: FreeDictionaryAPI.com, retaining all returned definitions, the original response, and content license/source metadata. An optional provider preference was offered and remained unanswered while independent implementation proceeded. The provisional dictionaryapi.dev provider timed out on an advanced word; FreeDictionaryAPI.com returned that word with complete senses and relations and was selected after verification. Include its visible provider attribution and Wiktionary source/license links. No bulk MW/Oxford scraping or claimed right to retain their content. All scheduling, storage, graph traversal, and quiz grading run on the phone. There is no application server, account, analytics, or cloud synchronization. Only dictionary retrieval requires internet.

## Graph and screens

Home presents topic nodes, learned counts, and navigation to a local neighborhood. Selecting a topic shows its word nodes and a readable equivalent list. Selecting a word opens its full entry and sense-associated related expressions. Limit the rendered neighborhood to 12 words per page, with explicit paging; never render the whole catalogue. Confidence is shown with text as well as color. Common words in multiple topics share progress.

Screens: Explore (topics, graph/list, search), word detail, study session, quiz, and collection/progress. Use Kotlin and Jetpack Compose, minimum Android API 26, compile/target API 35, Java 17 bytecode. Support scrolling, accessible touch targets, dark/light themes, and system back navigation.

## Confidence scheduler

Ratings 1–5 set strength to 1, 10, 20, 40, 100 respectively. Current strength is stored strength minus nonnegative elapsed milliseconds divided by 86,400,000. Decay is fractional across elapsed 24-hour periods, unaffected by timezone and daylight-saving transitions. A clock moved backwards cannot increase strength beyond the stored value. Negative strength is retained for overdue ordering.

Review selection is weakest-first, without a zero threshold. At session creation each eligible word receives one random offset within [-0.5, 0.5] strength units. The offset changes selection order only, never stored strength, and remains fixed for that session. Deterministic tie-breaking uses the normalized word. Use a priority queue over strength plus offset. One review batch contains each word at most once; this stronger cooldown prevents immediate loops, including tiny decks. Self-rating resets strength and timestamp and increments review count atomically. Persist each rating immediately.

Review + new topic: up to 10 previously learned, downloaded words from the whole collection, then up to 10 unlearned downloaded words from the selected topic. Pure learn: up to 10 unlearned downloaded words in the chosen topic, no existing reviews. Review only is available for the collection. Sessions show phase and progress, require reveal before rating, and only add new words to review on rating. Canceling leaves completed ratings intact and unrated words unchanged. Previously known words can be assigned rating 5; skipped words do not enter a session. Explicitly indicate when there are no eligible cards. A session's list is stable across rotation; interrupted sessions can be restarted while completed ratings survive process death.

## Quiz

Meaning plus example → select every suitable displayed expression. A curated set of sense-specific questions provides validated alternative answers and explanations, including constructions such as `seek` versus `look for`. Never infer grammatical interchangeability from a flat thesaurus list. Quiz grading uses exact set equality and identifies missed correct choices and extra wrong choices. Only explicit self-rating of a target word changes its confidence; merely selecting distractors does not enroll them. Initial validated quizzes are a small labeled practice set, not automatically generated coverage for every word.

## Persistence and errors

Use a SQLite database on device with separate cached dictionary entries and progress rows keyed by normalized word. Persist original response content plus source/license information. Downloaded entries and progress survive relaunch. Database upgrade logic must preserve progress. Network work and storage I/O run off the UI thread. Bound HTTP connections to 10 seconds, response reads to 30 seconds, and responses to 2 MiB; network reads remain bounded even during provider slowdowns. Missing entries, malformed responses, and network failure leave existing cache and progress intact; offer retry and keep offline content available. Each download is user-initiated, with a bounded topic batch and visible progress. No background scraping.

## Validation and limits

Pure Kotlin unit tests cover confidence reset/decay, overdue ordering, jitter bounds/stability, global deduplication, session mode separation, and quiz set grading. Parser fixture tests cover multiple homographs, definitions, empty fields, attribution, and malformed/missing entries. Android checks cover compilation and lint; exercise persistence and reveal/rating flows with Robolectric where available. Build a debug APK if the toolchain can be provisioned. Record any checks prevented by the environment rather than claiming them successful.

Images, cloud sync, automatic semantic clustering, a universal generated quiz engine, and a complete frequency-ranked corpus are outside this initial implementation. The graph and provider boundaries allow expansion without changing learning records.
