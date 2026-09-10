package com.wordtiles.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wordtiles.core.Definition
import com.wordtiles.core.Meaning
import com.wordtiles.core.WordEntry
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(WordStore.DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(WordStore.DATABASE_NAME)
    }

    @Test
    fun `lookup caches and returns the requested canonical key rather than provider headword`() = runTest {
        val provider = FakeProvider(entry("provider headword", "A definition."))
        WordRepository(context, provider, { 100L }, Dispatchers.Unconfined).use { repository ->
            val first = repository.lookup("  SEEK   OUT ")
            val second = repository.lookup("seek out")

            assertEquals("seek out", first.word)
            assertEquals(first, second)
            assertEquals(listOf("seek out"), provider.requests)
            assertEquals(first, repository.snapshot().entries.getValue("seek out"))
            assertEquals(emptyMap<String, CollectionMark>(), repository.snapshot().collection)
        }
    }

    @Test
    fun `failed refresh leaves an existing cached entry intact`() = runTest {
        val provider = FakeProvider(entry("abstruse", "Original definition."))
        WordRepository(context, provider, { 100L }, Dispatchers.Unconfined).use { repository ->
            val cached = repository.lookup("abstruse")
            provider.failure = DictionaryNetworkException("Network unavailable.")

            try {
                repository.lookup("abstruse", refresh = true)
                fail("refresh should expose the network error")
            } catch (_: DictionaryNetworkException) {
                // Expected: only a successful, valid response may replace the cached entry.
            }
            assertEquals(cached, repository.snapshot().entries.getValue("abstruse"))
        }
    }

    @Test
    fun `ratings update atomically and exclusions are reflected in snapshots`() = runTest {
        var now = 1_000L
        val provider = FakeProvider(entry("liminal", "At a threshold."))
        WordRepository(context, provider, { now }, Dispatchers.Unconfined).use { repository ->
            repository.lookup("liminal")
            repository.setStarred("liminal", true)
            assertEquals(1, repository.rateWord(" LIMINAL ", 2).reviews)
            now = 2_000L
            val updated = repository.rateWord("liminal", 4)
            repository.excludeWord(" LIMINAL ", true)

            assertEquals(2, updated.reviews)
            assertEquals(40.0, updated.strength, 0.0)
            assertEquals(2_000L, updated.ratedAtMillis)
            assertEquals(setOf("liminal"), repository.snapshot().excluded)
            assertEquals(CollectionMark(saved = true, starred = true), repository.snapshot().collection.getValue("liminal"))

            repository.excludeWord("liminal", false)
            assertEquals(emptySet<String>(), repository.snapshot().excluded)
        }
    }

    @Test
    fun `repository toggles collection flags without changing progress`() = runTest {
        val provider = FakeProvider(entry("abstruse", "Difficult to understand."))
        WordRepository(context, provider, { 100L }, Dispatchers.Unconfined).use { repository ->
            repository.lookup("abstruse")
            repository.setSaved(" ABSTRUSE ", true)
            repository.setStarred("abstruse", true)
            repository.setSaved("abstruse", false)

            val snapshot = repository.snapshot()
            assertEquals(CollectionMark(starred = true), snapshot.collection.getValue("abstruse"))
            assertEquals(emptyMap<String, com.wordtiles.core.Progress>(), snapshot.progress)
        }
    }

    @Test
    fun `daily topics are stable for a day and refresh on the next local date`() = runTest {
        val provider = FakeProvider(entry("unused", "Unused."))
        WordRepository(context, provider, { 100L }, Dispatchers.Unconfined).use { repository ->
            val first = repository.dailyTopics(LocalDate.of(2026, 9, 7))
            val repeated = repository.dailyTopics(LocalDate.of(2026, 9, 7))
            val nextDay = repository.dailyTopics(LocalDate.of(2026, 9, 8))

            assertEquals(first, repeated)
            assertEquals("2026-09-07", first.day)
            assertEquals(10, first.topicIds.size)
            assertEquals(10, first.topicIds.toSet().size)
            assertTrue(first.topicIds.all { id -> Catalog.topics.any { it.id == id } })
            assertEquals("2026-09-08", nextDay.day)
            assertNotEquals(first.topicIds, nextDay.topicIds)
            assertEquals(0, provider.requests.size)
            val snapshot = repository.snapshot()
            assertEquals(nextDay, snapshot.dailyTopics)
            assertEquals(emptyMap<String, WordEntry>(), snapshot.entries)
            assertEquals(emptyMap<String, CollectionMark>(), snapshot.collection)
        }
    }

    @Test
    fun `closing during lookup prevents the eventual response from reopening storage`() = runTest {
        val response = CompletableDeferred<WordEntry>()
        val repository = WordRepository(
            context,
            DictionaryProvider { response.await() },
            { 100L },
            Dispatchers.Unconfined,
        )
        supervisorScope {
            val lookup = async(start = CoroutineStart.UNDISPATCHED) { repository.lookup("liminal") }

            repository.close()
            response.complete(entry("liminal", "At a threshold."))

            try {
                lookup.await()
                fail("a closed repository should reject an in-flight result")
            } catch (_: IllegalStateException) {
                // Expected: close wins before the downloaded entry can be saved.
            }
        }
        WordStore(context).use { assertEquals(emptyMap<String, WordEntry>(), it.entries()) }
    }

    private fun entry(word: String, definition: String) = WordEntry(
        word = word,
        meanings = listOf(Meaning("verb", listOf(Definition(definition)))),
    )

    private class FakeProvider(var response: WordEntry) : DictionaryProvider {
        val requests = mutableListOf<String>()
        var failure: DictionaryException? = null

        override suspend fun lookup(word: String): WordEntry {
            requests += word
            failure?.let { throw it }
            return response
        }
    }
}
