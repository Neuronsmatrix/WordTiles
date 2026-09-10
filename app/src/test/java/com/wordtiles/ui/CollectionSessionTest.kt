package com.wordtiles.ui

import com.wordtiles.core.*
import com.wordtiles.data.CollectionMark
import org.junit.Assert.*
import org.junit.Test

class CollectionSessionTest {
    private val entry = WordEntry("cogent", meanings = listOf(Meaning("adjective", listOf(Definition("Convincing.")))))

    @Test fun `review includes starred unrated words but excludes merely cached entries`() {
        val state = AppState(entries = mapOf("cogent" to entry, "ephemeral" to entry.copy(word = "ephemeral")),
            collection = mapOf("cogent" to CollectionMark(starred = true)))
        assertEquals(listOf(StudyCard("cogent", true)), collectionSession(state, SessionMode.REVIEW, emptyList(), 0, 42))
    }

    @Test fun `removing collection marks excludes a word even if it has old progress`() {
        val state = AppState(entries = mapOf("cogent" to entry), progress = mapOf("cogent" to Progress("cogent", 1.0, 0)))
        assertTrue(collectionSession(state, SessionMode.REVIEW, emptyList(), 0, 42).isEmpty())
    }

    @Test fun `mixed mode does not repeat an unrated starred word in the new topic phase`() {
        val state = AppState(entries = mapOf("cogent" to entry), collection = mapOf("cogent" to CollectionMark(starred = true)))
        assertEquals(listOf(StudyCard("cogent", true)), collectionSession(state, SessionMode.MIXED, listOf("cogent"), 0, 42))
    }

    @Test fun `pure learn can use downloaded topic words without saving the suggestion`() {
        val state = AppState(entries = mapOf("cogent" to entry))
        assertEquals(listOf(StudyCard("cogent", true)), collectionSession(state, SessionMode.LEARN, listOf("cogent"), 0, 42))
        assertTrue(state.collection.isEmpty())
    }
}
