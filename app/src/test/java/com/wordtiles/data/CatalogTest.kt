package com.wordtiles.data

import com.wordtiles.core.canonicalWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun `starter catalog has eight substantial advanced topics and intentional overlap`() {
        assertTrue(Catalog.topics.size >= 8)
        assertTrue(Catalog.topics.all { it.words.size >= 10 })
        assertTrue(Catalog.topics.flatMap { it.words }.all { it == canonicalWord(it) })
        val memberships = Catalog.topics.flatMap { it.words }.groupingBy { it }.eachCount()
        assertTrue(memberships.values.any { it > 1 })
    }

    @Test
    fun `curated quizzes have valid identifiers answer keys and explanations`() {
        assertTrue(Catalog.quizzes.size >= 6)
        Catalog.quizzes.forEach { question ->
            assertTrue(question.options.size >= 3)
            assertEquals(question.options.size, question.options.map { it.id }.toSet().size)
            assertTrue(question.correctIds.isNotEmpty())
            assertTrue(question.correctIds.all { id -> question.options.any { it.id == id } })
            assertTrue(question.options.all { it.explanation.isNotBlank() })
        }
    }
}
