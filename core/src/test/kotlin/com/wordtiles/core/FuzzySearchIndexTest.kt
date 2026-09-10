package com.wordtiles.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FuzzySearchIndexTest {
    @Test
    fun `exact match is normalized and ranked first`() {
        val index = FuzzySearchIndex(listOf("lookup", "look for", "look forward"))

        assertEquals(
            listOf("look for", "look forward"),
            index.suggest("  LOOK\t  FOR  ", limit = 2),
        )
    }

    @Test
    fun `prefix completions are shortest first with stable lexical ties`() {
        val index = FuzzySearchIndex(listOf("application", "appraise", "apple", "apply", "banana"))

        assertEquals(
            listOf("apple", "apply", "appraise", "application"),
            index.suggest("app"),
        )
    }

    @Test
    fun `insertion deletion substitution and adjacent transposition find the intended word`() {
        val index = FuzzySearchIndex(listOf("look for", "bridge", "planet", "stone"))

        val cases = mapOf(
            "loook for" to "look for",
            "brdge" to "bridge",
            "plamet" to "planet",
            "tsone" to "stone",
        )

        cases.forEach { (query, expected) ->
            assertEquals(expected, index.suggest(query).firstOrNull())
        }
    }

    @Test
    fun `a misspelling is never returned as an invented exact candidate`() {
        val index = FuzzySearchIndex(listOf("apple"))

        assertEquals(listOf("apple"), index.suggest("applf"))
    }

    @Test
    fun `limit empty nonsense and overlong queries are bounded`() {
        val index = FuzzySearchIndex(listOf("alpha", "alpine", "alps", "beta"))

        assertEquals(listOf("alps", "alpha"), index.suggest("al", limit = 2))
        assertEquals(emptyList<String>(), index.suggest("al", limit = 0))
        assertEquals(emptyList<String>(), index.suggest("   "))
        assertEquals(emptyList<String>(), index.suggest("zzzzzzzz"))
        assertEquals(emptyList<String>(), index.suggest("a".repeat(101)))
    }

    @Test
    fun `duplicate and blank source entries are discarded`() {
        val index = FuzzySearchIndex(listOf(" Bridge ", "bridge", "", "  "))

        assertEquals(listOf("bridge"), index.suggest("bridge"))
    }
}
