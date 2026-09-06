package com.wordtiles.core

import java.util.Locale
import java.util.PriorityQueue
import kotlin.random.Random

private const val MILLIS_PER_DAY = 86_400_000.0
private val WHITESPACE = Regex("\\s+")
private val RATING_STRENGTHS = doubleArrayOf(1.0, 10.0, 20.0, 40.0, 100.0)

data class Progress(
    val word: String,
    val strength: Double,
    val ratedAtMillis: Long,
    val reviews: Int = 1,
)

enum class SessionMode {
    REVIEW,
    LEARN,
    MIXED,
}

data class StudyCard(
    val word: String,
    val isNew: Boolean,
)

fun canonicalWord(word: String): String =
    word.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")

fun strengthAt(progress: Progress, nowMillis: Long): Double {
    val elapsedMillis = if (nowMillis <= progress.ratedAtMillis) {
        0.0
    } else {
        nowMillis.toDouble() - progress.ratedAtMillis.toDouble()
    }
    return progress.strength - elapsedMillis / MILLIS_PER_DAY
}

fun rate(
    word: String,
    rating: Int,
    nowMillis: Long,
    previous: Progress? = null,
): Progress {
    require(rating in 1..5) { "rating must be between 1 and 5" }
    return Progress(
        word = canonicalWord(word),
        strength = RATING_STRENGTHS[rating - 1],
        ratedAtMillis = nowMillis,
        reviews = previous?.reviews?.plus(1) ?: 1,
    )
}

fun reviewOrder(
    progress: Collection<Progress>,
    nowMillis: Long,
    seed: Int,
): List<String> {
    val latest = latestProgress(progress)
    val random = Random(seed)
    val queue = PriorityQueue(compareBy<ScheduledWord>({ it.adjustedStrength }, { it.word }))

    latest.keys.sorted().forEach { word ->
        val jitter = random.nextDouble() - 0.5
        queue.add(
            ScheduledWord(
                word = word,
                adjustedStrength = strengthAt(latest.getValue(word), nowMillis) + jitter,
            ),
        )
    }

    return buildList(queue.size) {
        while (queue.isNotEmpty()) {
            add(queue.remove().word)
        }
    }
}

fun buildSession(
    mode: SessionMode,
    topicWords: List<String>,
    availableWords: Set<String>,
    progress: Collection<Progress>,
    nowMillis: Long,
    seed: Int,
    limit: Int = 10,
): List<StudyCard> {
    require(limit >= 0) { "limit must not be negative" }
    if (limit == 0) return emptyList()

    val available = availableWords.mapTo(hashSetOf(), ::canonicalWord)
    val latest = latestProgress(progress)
    val learnedWords = latest.keys

    val reviews = when (mode) {
        SessionMode.LEARN -> emptyList()
        SessionMode.REVIEW,
        SessionMode.MIXED,
        -> reviewOrder(
            progress = latest.filterKeys { it in available }.values,
            nowMillis = nowMillis,
            seed = seed,
        ).take(limit).map { StudyCard(word = it, isNew = false) }
    }

    val introductions = when (mode) {
        SessionMode.REVIEW -> emptyList()
        SessionMode.LEARN,
        SessionMode.MIXED,
        -> topicWords
            .asSequence()
            .map(::canonicalWord)
            .distinct()
            .filter { it in available && it !in learnedWords }
            .take(limit)
            .map { StudyCard(word = it, isNew = true) }
            .toList()
    }

    return reviews + introductions
}

private data class ScheduledWord(
    val word: String,
    val adjustedStrength: Double,
)

private val progressPreference = compareBy<Progress>(
    { it.ratedAtMillis },
    { it.reviews },
    { it.strength },
    { it.word },
)

private fun latestProgress(progress: Collection<Progress>): Map<String, Progress> {
    val latest = HashMap<String, Progress>()
    progress.forEach { candidate ->
        val word = canonicalWord(candidate.word)
        val current = latest[word]
        if (current == null || progressPreference.compare(candidate, current) > 0) {
            latest[word] = candidate
        }
    }
    return latest
}
