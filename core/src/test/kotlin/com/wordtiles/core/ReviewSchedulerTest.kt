package com.wordtiles.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReviewSchedulerTest {
    private val dayMillis = 86_400_000L

    @Test
    fun `canonical word trims folds case with root locale and collapses whitespace`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))

            assertEquals("i seek out", canonicalWord("  I\tSEEK\n OUT  "))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `rating two stores strength ten for a new normalized word`() {
        assertEquals(
            Progress(word = "seek out", strength = 10.0, ratedAtMillis = 123L, reviews = 1),
            rate("  SEEK   OUT ", rating = 2, nowMillis = 123L),
        )
    }

    @Test
    fun `rerating resets strength and increments review count`() {
        val previous = Progress("seek", strength = -4.0, ratedAtMillis = 10L, reviews = 3)

        assertEquals(
            Progress(word = "seek", strength = 100.0, ratedAtMillis = 500L, reviews = 4),
            rate(" SEEK ", rating = 5, nowMillis = 500L, previous = previous),
        )
    }

    @Test
    fun `each valid rating maps to the documented strength scale`() {
        val expectedStrengths = listOf(1.0, 10.0, 20.0, 40.0, 100.0)

        assertEquals(
            expectedStrengths,
            (1..5).map { rating -> rate("seek", rating, nowMillis = 0L).strength },
        )
    }

    @Test
    fun `rating outside one through five is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            rate("seek", rating = 0, nowMillis = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            rate("seek", rating = 6, nowMillis = 0L)
        }
    }

    @Test
    fun `strength decays fractionally by one point per elapsed day`() {
        val progress = Progress("seek", strength = 10.0, ratedAtMillis = 1_000L)

        assertEquals(7.5, strengthAt(progress, 1_000L + (dayMillis * 5L / 2L)), 0.0)
    }

    @Test
    fun `strength remains negative when overdue`() {
        val progress = Progress("seek", strength = 1.0, ratedAtMillis = 0L)

        assertEquals(-2.0, strengthAt(progress, 3L * dayMillis), 0.0)
    }

    @Test
    fun `future rating timestamp cannot increase strength`() {
        val progress = Progress("seek", strength = 10.0, ratedAtMillis = 5L * dayMillis)

        assertEquals(10.0, strengthAt(progress, nowMillis = 0L), 0.0)
    }

    @Test
    fun `review order is weakest first even when every word is above zero`() {
        val progress = listOf(
            Progress("strong", strength = 100.0, ratedAtMillis = 0L),
            Progress("medium", strength = 40.0, ratedAtMillis = 0L),
            Progress("weak", strength = 20.0, ratedAtMillis = 0L),
        )

        assertEquals(
            listOf("weak", "medium", "strong"),
            reviewOrder(progress, nowMillis = 0L, seed = 7),
        )
    }

    @Test
    fun `review order uses decayed strength so overdue words come first`() {
        val progress = listOf(
            Progress("fresh weak", strength = 1.0, ratedAtMillis = 20L * dayMillis),
            Progress("overdue", strength = 10.0, ratedAtMillis = 0L),
        )

        assertEquals(
            listOf("overdue", "fresh weak"),
            reviewOrder(progress, nowMillis = 20L * dayMillis, seed = 7),
        )
    }

    @Test
    fun `bounded jitter cannot reverse strengths one point apart`() {
        val progress = listOf(
            Progress("weaker", strength = 10.0, ratedAtMillis = 0L),
            Progress("stronger", strength = 11.0, ratedAtMillis = 0L),
        )

        repeat(100) { seed ->
            assertEquals(
                listOf("weaker", "stronger"),
                reviewOrder(progress, nowMillis = 0L, seed = seed),
            )
        }
    }

    @Test
    fun `jitter assignment is stable for a seed and independent of input order`() {
        val progress = listOf(
            Progress("zeta", strength = 10.0, ratedAtMillis = 0L),
            Progress("alpha", strength = 10.0, ratedAtMillis = 0L),
            Progress("mu", strength = 10.0, ratedAtMillis = 0L),
        )

        val first = reviewOrder(progress, nowMillis = 0L, seed = 91)

        assertEquals(first, reviewOrder(progress.asReversed(), nowMillis = 0L, seed = 91))
        assertEquals(first, reviewOrder(progress, nowMillis = 0L, seed = 91))
    }

    @Test
    fun `jitter can change the order of tied words between seeds`() {
        val progress = listOf(
            Progress("alpha", strength = 10.0, ratedAtMillis = 0L),
            Progress("beta", strength = 10.0, ratedAtMillis = 0L),
            Progress("gamma", strength = 10.0, ratedAtMillis = 0L),
        )

        val observedOrders = (0..20)
            .map { seed -> reviewOrder(progress, nowMillis = 0L, seed = seed) }
            .toSet()

        assertEquals(true, observedOrders.size > 1)
    }

    @Test
    fun `review order deduplicates canonical words using the latest record`() {
        val progress = listOf(
            Progress(" Shared ", strength = 1.0, ratedAtMillis = 10L),
            Progress("shared", strength = 100.0, ratedAtMillis = 20L),
            Progress("other", strength = 20.0, ratedAtMillis = 20L),
        )

        assertEquals(
            listOf("other", "shared"),
            reviewOrder(progress, nowMillis = 20L, seed = 1),
        )
    }

    @Test
    fun `duplicate record ties resolve independently of collection order`() {
        val first = Progress(" SHARED", strength = 1.0, ratedAtMillis = 20L, reviews = 1)
        val second = Progress("shared ", strength = 100.0, ratedAtMillis = 20L, reviews = 2)
        val other = Progress("other", strength = 20.0, ratedAtMillis = 20L)

        assertEquals(
            reviewOrder(listOf(first, second, other), nowMillis = 20L, seed = 1),
            reviewOrder(listOf(second, first, other), nowMillis = 20L, seed = 1),
        )
    }

    @Test
    fun `review session includes learned cached words from the whole collection`() {
        val cards = buildSession(
            mode = SessionMode.REVIEW,
            topicWords = listOf("topic new"),
            availableWords = setOf("GLOBAL WEAK", "topic learned", "topic new"),
            progress = listOf(
                Progress("global weak", strength = 1.0, ratedAtMillis = 0L),
                Progress("topic learned", strength = 40.0, ratedAtMillis = 0L),
                Progress("not downloaded", strength = -100.0, ratedAtMillis = 0L),
            ),
            nowMillis = 0L,
            seed = 2,
            limit = 10,
        )

        assertEquals(
            listOf(
                StudyCard("global weak", isNew = false),
                StudyCard("topic learned", isNew = false),
            ),
            cards,
        )
    }

    @Test
    fun `learn session contains only downloaded unlearned topic words once`() {
        val cards = buildSession(
            mode = SessionMode.LEARN,
            topicWords = listOf(" New Word ", "new   word", "learned", "absent"),
            availableWords = setOf("NEW WORD", "learned", "unrelated"),
            progress = listOf(Progress(" LEARNED ", strength = 10.0, ratedAtMillis = 0L)),
            nowMillis = 0L,
            seed = 3,
            limit = 10,
        )

        assertEquals(listOf(StudyCard("new word", isNew = true)), cards)
    }

    @Test
    fun `mixed session places a full review limit before a separate new limit`() {
        val cards = buildSession(
            mode = SessionMode.MIXED,
            topicWords = listOf("new one", "new two", "new three"),
            availableWords = setOf("review one", "review two", "review three", "new one", "new two", "new three"),
            progress = listOf(
                Progress("review three", strength = 30.0, ratedAtMillis = 0L),
                Progress("review one", strength = 10.0, ratedAtMillis = 0L),
                Progress("review two", strength = 20.0, ratedAtMillis = 0L),
            ),
            nowMillis = 0L,
            seed = 4,
            limit = 2,
        )

        assertEquals(
            listOf(
                StudyCard("review one", isNew = false),
                StudyCard("review two", isNew = false),
                StudyCard("new one", isNew = true),
                StudyCard("new two", isNew = true),
            ),
            cards,
        )
    }

    @Test
    fun `zero limit returns an empty session`() {
        assertEquals(
            emptyList<StudyCard>(),
            buildSession(
                mode = SessionMode.MIXED,
                topicWords = listOf("new"),
                availableWords = setOf("new", "review"),
                progress = listOf(Progress("review", 1.0, 0L)),
                nowMillis = 0L,
                seed = 0,
                limit = 0,
            ),
        )
    }

    @Test
    fun `negative session limit is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildSession(
                mode = SessionMode.LEARN,
                topicWords = emptyList(),
                availableWords = emptySet(),
                progress = emptyList(),
                nowMillis = 0L,
                seed = 0,
                limit = -1,
            )
        }
    }
}
