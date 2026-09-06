package com.wordtiles.core

data class QuizOption(
    val id: String,
    val text: String,
    val explanation: String,
)

data class QuizQuestion(
    val id: String,
    val targetWord: String,
    val definition: String,
    val example: String,
    val options: List<QuizOption>,
    val correctIds: Set<String>,
)

data class QuizResult(
    val correct: Boolean,
    val missed: Set<String>,
    val extra: Set<String>,
)

fun grade(question: QuizQuestion, selected: Set<String>): QuizResult = QuizResult(
    correct = selected == question.correctIds,
    missed = question.correctIds - selected,
    extra = selected - question.correctIds,
)
