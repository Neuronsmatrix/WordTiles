package com.wordtiles.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyTopicSelectionTest {
    private val topicIds = (1..20).map { "topic-$it" }

    @Test
    fun `selection is deterministic distinct and bounded`() {
        val day = LocalDate.of(2026, 9, 7)

        val first = selectDailyTopicIds(day, topicIds, count = 10)
        val repeated = selectDailyTopicIds(day, topicIds, count = 10)

        assertEquals(first, repeated)
        assertEquals(10, first.size)
        assertEquals(10, first.toSet().size)
    }

    @Test
    fun `selection does not depend on catalog declaration order`() {
        val day = LocalDate.of(2026, 9, 7)

        assertEquals(
            selectDailyTopicIds(day, topicIds, count = 10),
            selectDailyTopicIds(day, topicIds.reversed(), count = 10),
        )
    }

    @Test
    fun `consecutive days never retain the full selection`() {
        val first = selectDailyTopicIds(LocalDate.of(2026, 9, 7), topicIds, count = 10)
        val next = selectDailyTopicIds(LocalDate.of(2026, 9, 8), topicIds, count = 10)

        assertNotEquals(first, next)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selection rejects a catalog smaller than the requested count`() {
        selectDailyTopicIds(LocalDate.of(2026, 9, 7), listOf("one", "two"), count = 3)
    }
}
