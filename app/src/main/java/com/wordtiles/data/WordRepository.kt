package com.wordtiles.data

import android.content.Context
import com.wordtiles.core.Progress
import com.wordtiles.core.WordEntry
import com.wordtiles.core.canonicalWord
import com.wordtiles.core.rate
import java.io.Closeable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class Snapshot(
    val entries: Map<String, WordEntry>,
    val progress: Map<String, Progress>,
    val excluded: Set<String>,
)

class WordRepository private constructor(
    private val store: WordStore,
    private val provider: DictionaryProvider,
    private val nowMillis: () -> Long,
    private val ioDispatcher: CoroutineDispatcher,
) : Closeable {
    constructor(context: Context) : this(
        store = WordStore(context.applicationContext),
        provider = FreeDictionaryProvider(),
        nowMillis = System::currentTimeMillis,
        ioDispatcher = Dispatchers.IO,
    )

    internal constructor(
        context: Context,
        provider: DictionaryProvider,
        nowMillis: () -> Long,
        ioDispatcher: CoroutineDispatcher,
    ) : this(
        store = WordStore(context.applicationContext),
        provider = provider,
        nowMillis = nowMillis,
        ioDispatcher = ioDispatcher,
    )

    private val storeMutex = Mutex()
    @Volatile private var closed = false

    suspend fun snapshot(): Snapshot = withContext(ioDispatcher) {
        storeMutex.withLock {
            ensureOpen()
            Snapshot(
                entries = store.entries(),
                progress = store.progress(),
                excluded = store.excluded(),
            )
        }
    }

    suspend fun lookup(word: String, refresh: Boolean = false): WordEntry {
        val key = canonicalKey(word)
        if (!refresh) {
            val cached = withContext(ioDispatcher) {
                storeMutex.withLock {
                    ensureOpen()
                    store.entries()[key]
                }
            }
            if (cached != null) return cached
        }

        val downloaded = withContext(ioDispatcher) { provider.lookup(key) }
        currentCoroutineContext().ensureActive()
        val normalized = downloaded.copy(word = key)
        withContext(ioDispatcher) {
            storeMutex.withLock {
                ensureOpen()
                store.saveEntry(normalized)
            }
        }
        return normalized
    }

    suspend fun rateWord(word: String, rating: Int): Progress = withContext(ioDispatcher) {
        val key = canonicalKey(word)
        storeMutex.withLock {
            ensureOpen()
            val updated = rate(
                word = key,
                rating = rating,
                nowMillis = nowMillis(),
                previous = store.progress()[key],
            )
            store.saveProgress(updated)
            updated
        }
    }

    suspend fun excludeWord(word: String, excluded: Boolean) = withContext(ioDispatcher) {
        val key = canonicalKey(word)
        storeMutex.withLock {
            ensureOpen()
            store.setExcluded(key, excluded)
        }
    }

    override fun close() {
        runBlocking {
            storeMutex.withLock {
                if (!closed) {
                    closed = true
                    store.close()
                }
            }
        }
    }

    private fun canonicalKey(word: String): String = canonicalWord(word).also {
        require(it.isNotEmpty()) { "word must not be blank" }
    }

    private fun ensureOpen() {
        check(!closed) { "WordRepository is closed." }
    }
}
