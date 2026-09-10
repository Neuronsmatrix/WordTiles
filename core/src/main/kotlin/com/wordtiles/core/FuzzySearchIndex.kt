package com.wordtiles.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A compact in-memory index for deterministic, typo-tolerant word suggestions. */
class FuzzySearchIndex(words: Collection<String>) {
    private val sortedWords = words
        .asSequence()
        .map(::canonicalWord)
        .filter(String::isNotEmpty)
        .distinct()
        .sorted()
        .toList()
    private val exactWords = sortedWords.toHashSet()
    private val wordsByLength = sortedWords.groupBy(String::length)

    fun suggest(query: String, limit: Int = DEFAULT_LIMIT): List<String> {
        if (limit <= 0) return emptyList()
        val normalized = canonicalWord(query)
        if (normalized.isEmpty() || normalized.length > MAX_QUERY_LENGTH) return emptyList()

        val result = ArrayList<String>(limit)
        if (normalized in exactWords) result += normalized

        val prefixMatches = prefixMatches(normalized)
            .asSequence()
            .filter { it != normalized }
            .sortedWith(compareBy<String> { it.length }.thenBy { it })
            .take(limit - result.size)
        result.addAll(prefixMatches)
        if (result.size == limit) return result

        val maxDistance = maximumDistance(normalized.length)
        val fuzzy = ArrayList<FuzzyCandidate>()
        val previousPrevious = IntArray(normalized.length + 1)
        val previous = IntArray(normalized.length + 1)
        val current = IntArray(normalized.length + 1)

        for (length in max(1, normalized.length - maxDistance)..normalized.length + maxDistance) {
            wordsByLength[length].orEmpty().forEach { word ->
                if (word == normalized || word.startsWith(normalized)) return@forEach
                val distance = boundedDamerauLevenshtein(
                    left = word,
                    right = normalized,
                    maximum = maxDistance,
                    previousPrevious = previousPrevious,
                    previous = previous,
                    current = current,
                )
                if (distance <= maxDistance) {
                    fuzzy += FuzzyCandidate(
                        word = word,
                        distance = distance,
                        lengthDifference = abs(word.length - normalized.length),
                        commonPrefix = commonPrefixLength(word, normalized),
                    )
                }
            }
        }

        fuzzy.sortWith(
            compareBy<FuzzyCandidate>({ it.distance }, { it.lengthDifference })
                .thenByDescending { it.commonPrefix }
                .thenBy { it.word.length }
                .thenBy { it.word },
        )
        result.addAll(fuzzy.asSequence().map(FuzzyCandidate::word).take(limit - result.size))
        return result
    }

    private fun prefixMatches(prefix: String): List<String> {
        var low = 0
        var high = sortedWords.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (sortedWords[middle] < prefix) low = middle + 1 else high = middle
        }

        val matches = ArrayList<String>()
        var index = low
        while (index < sortedWords.size && sortedWords[index].startsWith(prefix)) {
            matches += sortedWords[index]
            index++
        }
        return matches
    }

    private data class FuzzyCandidate(
        val word: String,
        val distance: Int,
        val lengthDifference: Int,
        val commonPrefix: Int,
    )

    private companion object {
        const val DEFAULT_LIMIT = 8
        const val MAX_QUERY_LENGTH = 100

        fun maximumDistance(length: Int): Int = when {
            length <= 2 -> 1
            length <= 5 -> 1
            else -> 2
        }

        fun commonPrefixLength(left: String, right: String): Int {
            var length = 0
            while (length < min(left.length, right.length) && left[length] == right[length]) length++
            return length
        }

        fun boundedDamerauLevenshtein(
            left: String,
            right: String,
            maximum: Int,
            previousPrevious: IntArray,
            previous: IntArray,
            current: IntArray,
        ): Int {
            if (abs(left.length - right.length) > maximum) return maximum + 1

            for (column in 0..right.length) previous[column] = column
            previousPrevious.fill(maximum + 1)
            var twoRowsBack = previousPrevious
            var lastRow = previous
            var thisRow = current

            for (row in 1..left.length) {
                thisRow.fill(maximum + 1)
                thisRow[0] = row
                val firstColumn = max(1, row - maximum)
                val lastColumn = min(right.length, row + maximum)

                for (column in firstColumn..lastColumn) {
                    val substitutionCost = if (left[row - 1] == right[column - 1]) 0 else 1
                    var distance = min(
                        min(lastRow[column] + 1, thisRow[column - 1] + 1),
                        lastRow[column - 1] + substitutionCost,
                    )
                    if (
                        row > 1 && column > 1 &&
                        left[row - 1] == right[column - 2] &&
                        left[row - 2] == right[column - 1]
                    ) {
                        distance = min(distance, twoRowsBack[column - 2] + 1)
                    }
                    thisRow[column] = distance
                }

                val swap = twoRowsBack
                twoRowsBack = lastRow
                lastRow = thisRow
                thisRow = swap
            }
            return lastRow[right.length].coerceAtMost(maximum + 1)
        }
    }
}
