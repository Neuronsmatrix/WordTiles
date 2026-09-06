package com.wordtiles.core

import org.junit.Assert.assertEquals
import org.junit.Test

class QuizTest {
    private val question = QuizQuestion(
        id = "seek-alternatives",
        targetWord = "seek",
        definition = "Try to find or obtain something.",
        example = "They plan to ___ advice before acting.",
        options = listOf(
            QuizOption("seek", "seek", "Fits this construction."),
            QuizOption("look-for", "look for", "A suitable alternative in this question."),
            QuizOption("search", "search", "Needs a different object construction here."),
        ),
        correctIds = setOf("seek", "look-for"),
    )

    @Test
    fun `selecting every and only correct option is correct`() {
        assertEquals(
            QuizResult(correct = true, missed = emptySet(), extra = emptySet()),
            grade(question, selected = setOf("seek", "look-for")),
        )
    }

    @Test
    fun `omitting a correct option reports it as missed`() {
        assertEquals(
            QuizResult(correct = false, missed = setOf("look-for"), extra = emptySet()),
            grade(question, selected = setOf("seek")),
        )
    }

    @Test
    fun `wrong replacement reports both missed and extra option ids`() {
        assertEquals(
            QuizResult(correct = false, missed = setOf("look-for"), extra = setOf("search")),
            grade(question, selected = setOf("seek", "search")),
        )
    }
}
