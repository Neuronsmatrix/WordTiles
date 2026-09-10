package com.wordtiles.ui

import com.wordtiles.core.*

/** Collection membership is independent of cache/history. Unrated collected words can be reviewed. */
fun collectionSession(state: AppState, mode: SessionMode, topicWords: List<String>, now: Long, seed: Int): List<StudyCard> {
    val available = state.entries.keys - state.excluded
    val collected = state.collectionWords.intersect(available)
    val reviews = if (mode == SessionMode.LEARN) emptyList() else {
        val unrated = collected.filter { it !in state.progress }.sorted().map { StudyCard(it, true) }
        val rated = reviewOrder(state.progress.filterKeys { it in collected }.values, now, seed)
            .map { StudyCard(it, false) }
        (unrated + rated).take(10)
    }
    val introductions = if (mode == SessionMode.REVIEW) emptyList() else buildSession(
        SessionMode.LEARN, topicWords, available - reviews.map { it.word }.toSet(), state.progress.values, now, seed,
    )
    return reviews + introductions
}
