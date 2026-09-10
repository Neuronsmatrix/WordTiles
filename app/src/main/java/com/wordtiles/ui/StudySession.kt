package com.wordtiles.ui

import com.wordtiles.core.StudyCard

data class StudySession(
    val cards: List<StudyCard>,
    val index: Int = 0,
    val revealed: Boolean = false,
    val rated: Int = 0,
    val skipped: Int = 0,
) {
    val current: StudyCard? get() = cards.getOrNull(index)
    val finished: Boolean get() = current == null

    fun reveal(): StudySession = if (finished) this else copy(revealed = true)

    fun flip(): StudySession = if (finished) this else copy(revealed = !revealed)

    fun afterRating(): StudySession {
        check(revealed && !finished) { "Reveal the current word before rating it." }
        return copy(index = index + 1, revealed = false, rated = rated + 1)
    }

    fun skip(): StudySession = if (finished) this else
        copy(index = index + 1, revealed = false, skipped = skipped + 1)
}
