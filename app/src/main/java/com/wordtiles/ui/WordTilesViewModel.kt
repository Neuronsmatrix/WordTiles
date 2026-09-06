package com.wordtiles.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wordtiles.core.*
import com.wordtiles.data.Catalog
import com.wordtiles.data.Topic
import com.wordtiles.data.WordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

sealed interface Page {
    data object Explore : Page
    data object Collection : Page
    data class TopicDetail(val id: String) : Page
    data class Entry(val word: String) : Page
    data object Study : Page
    data object Quiz : Page
}

data class AppState(
    val pages: List<Page> = listOf(Page.Explore),
    val entries: Map<String, WordEntry> = emptyMap(),
    val progress: Map<String, Progress> = emptyMap(),
    val excluded: Set<String> = emptySet(),
    val loading: Boolean = true,
    val loadingWord: String? = null,
    val downloadingTopic: String? = null,
    val downloadStatus: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val entryError: String? = null,
    val session: StudySession? = null,
    val quizIndex: Int = 0,
    val quizSelection: Set<String> = emptySet(),
    val quizResult: QuizResult? = null,
    val quizRated: Boolean = false,
) {
    val page: Page get() = pages.last()
}

class WordTilesViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WordRepository = WordRepository(application),
) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    private var lookupJob: Job? = null
    private var lookupGeneration = 0L
    private var downloadJob: Job? = null

    init { reload() }

    private fun reload() = viewModelScope.launch {
        try {
            val snapshot = repository.snapshot()
            mutable.update { it.copy(entries = snapshot.entries, progress = snapshot.progress,
                excluded = snapshot.excluded, loading = false) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            mutable.update { it.copy(loading = false, message = "Could not open your collection. ${error.message.orEmpty()}") }
        }
    }

    fun navigate(page: Page) {
        if (mutable.value.saving || mutable.value.page == page) return
        mutable.update { it.copy(pages = it.pages + page, entryError = null) }
    }

    fun switchTab(page: Page) {
        if (mutable.value.saving) return
        invalidateLookup()
        mutable.update { it.copy(pages = listOf(page), loadingWord = null, entryError = null) }
    }

    fun back() {
        if (mutable.value.saving) return
        invalidateLookup()
        mutable.update {
            if (it.pages.size <= 1) {
                it.copy(loadingWord = null, entryError = null)
            } else {
                it.copy(pages = it.pages.dropLast(1), loadingWord = null, entryError = null)
            }
        }
    }

    fun dismissMessage() { mutable.update { it.copy(message = null) } }

    fun openWord(raw: String, refresh: Boolean = false) {
        val word = canonicalWord(raw)
        if (word.isBlank() || mutable.value.saving) return
        navigate(Page.Entry(word))
        val generation = invalidateLookup()
        if (!refresh && word in mutable.value.entries) {
            mutable.update { it.copy(loadingWord = null, entryError = null) }
            return
        }
        lookupJob = viewModelScope.launch {
            if (generation != lookupGeneration) return@launch
            mutable.update { it.copy(loadingWord = word, entryError = null) }
            try {
                val entry = repository.lookup(word, refresh)
                if (generation == lookupGeneration) {
                    mutable.update { it.copy(entries = it.entries + (word to entry)) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == lookupGeneration) {
                    mutable.update {
                        it.copy(entryError = error.message ?: "Dictionary unavailable. Try again when connected.")
                    }
                }
            } finally {
                if (generation == lookupGeneration) {
                    mutable.update { it.copy(loadingWord = null) }
                    lookupJob = null
                }
            }
        }
    }

    private fun invalidateLookup(): Long {
        lookupJob?.cancel()
        lookupJob = null
        lookupGeneration += 1
        return lookupGeneration
    }

    fun downloadTopic(topic: Topic) {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            val missing = topic.words.map(::canonicalWord).distinct().filter { it !in mutable.value.entries }
            mutable.update { it.copy(downloadingTopic = topic.id) }
            var failed = 0
            try {
                missing.forEachIndexed { index, word ->
                    mutable.update { it.copy(downloadStatus = "${index + 1} / ${missing.size} · $word") }
                    try {
                        val entry = repository.lookup(word)
                        mutable.update { it.copy(entries = it.entries + (word to entry)) }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        failed++
                    }
                }
                mutable.update { it.copy(message = if (failed == 0) "Topic is ready for offline learning."
                    else "Saved ${missing.size - failed} entries. $failed unavailable; you can retry or study the saved words.") }
            } finally {
                mutable.update { it.copy(downloadingTopic = null, downloadStatus = "") }
            }
        }
    }

    fun cancelDownload() { downloadJob?.cancel() }

    fun startSession(mode: SessionMode, topic: Topic? = null) {
        val current = mutable.value
        if (current.loading || current.saving) return
        val cards = buildSession(mode, topic?.words.orEmpty(), current.entries.keys - current.excluded,
            current.progress.values, System.currentTimeMillis(), Random.nextInt())
        mutable.update { it.copy(session = StudySession(cards), pages = it.pages + Page.Study) }
    }

    fun reveal() { mutable.update { it.copy(session = it.session?.reveal()) } }

    fun skipCard() {
        if (!mutable.value.saving) mutable.update { it.copy(session = it.session?.skip()) }
    }

    fun rateCard(rating: Int) {
        val session = mutable.value.session ?: return
        if (!session.revealed || session.finished || mutable.value.saving) return
        saveRating(session.current!!.word, rating) {
            mutable.update { it.copy(session = session.afterRating()) }
        }
    }

    fun markKnown(word: String) = saveRating(word, 5)

    fun rateEntry(word: String, rating: Int) = saveRating(word, rating)

    private fun saveRating(word: String, rating: Int, afterSave: () -> Unit = {}) {
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                // Quiz targets can be new: obtain their complete entry before enrolling them.
                if (word !in mutable.value.entries) {
                    val entry = repository.lookup(word)
                    mutable.update { it.copy(entries = it.entries + (word to entry)) }
                }
                val progress = repository.rateWord(word, rating)
                mutable.update { it.copy(progress = it.progress + (progress.word to progress)) }
                afterSave()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(message = "Rating was not saved. ${error.message.orEmpty()}") }
            } finally {
                mutable.update { it.copy(saving = false) }
            }
        }
    }

    fun toggleExcluded(word: String) {
        if (mutable.value.saving) return
        val excluded = word !in mutable.value.excluded
        mutable.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.excludeWord(word, excluded)
                mutable.update { it.copy(excluded = if (excluded) it.excluded + word else it.excluded - word) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(message = "Could not update this word. ${error.message.orEmpty()}") }
            } finally { mutable.update { it.copy(saving = false) } }
        }
    }

    fun selectQuiz(id: String) {
        if (mutable.value.quizResult != null) return
        mutable.update { it.copy(quizSelection = if (id in it.quizSelection) it.quizSelection - id else it.quizSelection + id) }
    }

    fun checkQuiz() {
        val current = mutable.value
        if (current.quizResult != null || current.quizSelection.isEmpty()) return
        val question = Catalog.quizzes[current.quizIndex]
        mutable.update { it.copy(quizResult = grade(question, current.quizSelection)) }
    }

    fun rateQuiz(rating: Int) {
        val current = mutable.value
        if (current.quizResult == null || current.quizRated) return
        saveRating(Catalog.quizzes[current.quizIndex].targetWord, rating) {
            mutable.update { it.copy(quizRated = true) }
        }
    }

    fun nextQuiz() {
        if (mutable.value.saving) return
        mutable.update { it.copy(quizIndex = (it.quizIndex + 1) % Catalog.quizzes.size,
            quizSelection = emptySet(), quizResult = null, quizRated = false) }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
