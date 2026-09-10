package com.wordtiles.ui

import com.wordtiles.core.StudyCard
import org.junit.Assert.*
import org.junit.Test

class StudySessionTest {
    @Test fun `tapping twice returns to question without recording a review`() {
        val session = StudySession(listOf(StudyCard("demolish", false)))
        val answer = session.flip()
        assertTrue(answer.revealed)
        val question = answer.flip()
        assertFalse(question.revealed)
        assertEquals(0, question.rated)
        assertThrows(IllegalStateException::class.java) { question.afterRating() }
    }

    @Test fun `unrevealed card cannot be advanced and silently lost`() {
        val session = StudySession(listOf(StudyCard("demolish", true)))
        assertThrows(IllegalStateException::class.java) { session.afterRating() }
        assertEquals("demolish", session.current?.word)
    }

    @Test fun `rating advances and hides the next word entry`() {
        val session = StudySession(listOf(StudyCard("demolish", false), StudyCard("refurbish", true)))
        val next = session.reveal().afterRating()
        assertEquals("refurbish", next.current?.word)
        assertFalse(next.revealed)
        assertEquals(1, next.rated)
        assertFalse(next.finished)
    }

    @Test fun `skipped word completes without being counted as learned`() {
        val session = StudySession(listOf(StudyCard("demolish", true))).skip()
        assertTrue(session.finished)
        assertEquals(0, session.rated)
        assertEquals(1, session.skipped)
        assertNull(session.current)
    }

    @Test fun `rating last card finishes exactly once`() {
        val session = StudySession(listOf(StudyCard("demolish", true))).reveal().afterRating()
        assertTrue(session.finished)
        assertThrows(IllegalStateException::class.java) { session.afterRating() }
    }
}
