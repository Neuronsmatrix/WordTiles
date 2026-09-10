package com.wordtiles.data

import android.content.Context
import com.wordtiles.core.FuzzySearchIndex
import com.wordtiles.core.canonicalWord
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads and searches the bundled headword list without making a network request. */
class LocalWordSearch(context: Context) {
    private val applicationContext = context.applicationContext
    private val loadMutex = Mutex()

    @Volatile
    private var loadedIndex: FuzzySearchIndex? = null

    suspend fun suggest(
        query: String,
        additionalWords: Collection<String> = emptyList(),
    ): List<String> {
        val normalized = canonicalWord(query)
        if (normalized.isEmpty() || normalized.length > MAX_QUERY_LENGTH) return emptyList()

        currentCoroutineContext().ensureActive()
        val bundled = bundledIndex()
        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val bundledResults = bundled.suggest(normalized, RESULT_LIMIT)
            if (additionalWords.isEmpty()) return@withContext bundledResults

            val additionalResults = FuzzySearchIndex(additionalWords).suggest(normalized, RESULT_LIMIT)
            currentCoroutineContext().ensureActive()
            FuzzySearchIndex(bundledResults + additionalResults).suggest(normalized, RESULT_LIMIT)
        }
    }

    private suspend fun bundledIndex(): FuzzySearchIndex {
        loadedIndex?.let { return it }
        return loadMutex.withLock {
            loadedIndex ?: withContext(Dispatchers.IO) {
                val words = ArrayList<String>(EXPECTED_WORD_COUNT)
                applicationContext.assets.open(LEXICON_ASSET).use { input ->
                    GZIPInputStream(input).bufferedReader(Charsets.UTF_8).use { reader ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            words += reader.readLine() ?: break
                        }
                    }
                }
                FuzzySearchIndex(words)
            }.also { loadedIndex = it }
        }
    }

    private companion object {
        const val LEXICON_ASSET = "search/wordnet-3.0-lemmas.txt.gzip"
        const val EXPECTED_WORD_COUNT = 150_000
        const val RESULT_LIMIT = 8
        const val MAX_QUERY_LENGTH = 100
    }
}
