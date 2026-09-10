package com.wordtiles.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.wordtiles.core.Definition
import com.wordtiles.core.Meaning
import com.wordtiles.core.Progress
import com.wordtiles.core.Source
import com.wordtiles.core.WordEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WordStoreTest {
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
    fun `entries and progress survive closing and reopening the database`() {
        val entry = WordEntry(
            word = "  Perfunctory ",
            phonetic = "/pəˈfʌŋktəri/",
            meanings = listOf(
                Meaning(
                    partOfSpeech = "adjective",
                    definitions = listOf(
                        Definition(
                            text = "Carried out with minimal effort.",
                            example = "A perfunctory inspection.",
                        ),
                    ),
                ),
            ),
            sources = listOf(Source("https://example.test/perfunctory", "CC BY", "https://example.test/license")),
            rawResponse = """[{"word":"perfunctory","license":{"name":"CC BY"}}]""",
        )
        WordStore(context).use { store ->
            store.saveEntry(entry)
            store.saveProgress(Progress(" PERFUNCTORY ", 20.0, 1234L, reviews = 2))
            store.setExcluded(" Perfunctory ", true)
        }

        WordStore(context).use { reopened ->
            assertEquals(entry.copy(word = "perfunctory"), reopened.entries().getValue("perfunctory"))
            assertEquals(Progress("perfunctory", 20.0, 1234L, 2), reopened.progress().getValue("perfunctory"))
            assertEquals(setOf("perfunctory"), reopened.excluded())
        }
    }

    @Test
    fun `saving the same canonical keys updates rows without erasing unrelated data`() {
        val original = entry("abstruse", "Difficult to understand.")
        val replacement = entry(" ABSTRUSE ", "Obscure or difficult to understand.")
        val other = entry("liminal", "At a boundary or transition.")

        WordStore(context).use { store ->
            store.saveEntry(original)
            store.saveEntry(other)
            store.saveProgress(Progress("abstruse", 10.0, 10L, 1))

            store.saveEntry(replacement)
            store.saveProgress(Progress(" ABSTRUSE ", 40.0, 20L, 2))

            assertEquals(replacement.copy(word = "abstruse"), store.entries().getValue("abstruse"))
            assertEquals(other, store.entries().getValue("liminal"))
            assertEquals(Progress("abstruse", 40.0, 20L, 2), store.progress().getValue("abstruse"))
        }
    }

    @Test
    fun `excluding and including a word toggles only its canonical key`() {
        WordStore(context).use { store ->
            store.setExcluded("  Seek  Out ", true)
            store.setExcluded("retain", true)
            assertTrue("seek out" in store.excluded())

            store.setExcluded("SEEK\tOUT", false)
            assertFalse("seek out" in store.excluded())
            assertEquals(setOf("retain"), store.excluded())
        }
    }

    @Test
    fun `version one migration preserves legacy data and saves only studied words`() {
        val cachedOnly = entry("cached", "Available offline.").copy(rawResponse = "cached raw")
        val studied = entry("studied", "Previously rated.").copy(rawResponse = "studied raw")
        createVersionOneDatabase(cachedOnly, studied)

        WordStore(context).use { migrated ->
            assertEquals(setOf("cached", "studied"), migrated.entries().keys)
            assertEquals("cached raw", migrated.entries().getValue("cached").rawResponse)
            assertEquals("studied raw", migrated.entries().getValue("studied").rawResponse)
            assertEquals(Progress("studied", 20.0, 1234L, 3), migrated.progress().getValue("studied"))
            assertEquals(setOf("cached"), migrated.excluded())
            assertEquals(mapOf("studied" to CollectionMark(saved = true)), migrated.collection())
        }
    }

    @Test
    fun `saved and starred flags toggle independently and survive reopening`() {
        WordStore(context).use { store ->
            store.saveEntry(entry("  Seek  Out ", "Find after searching."))
            store.setSaved("SEEK OUT", true)
            store.setStarred(" seek\tout ", true)
            store.setSaved("seek out", false)
            assertEquals(CollectionMark(starred = true), store.collection().getValue("seek out"))
        }

        WordStore(context).use { reopened ->
            assertEquals(CollectionMark(starred = true), reopened.collection().getValue("seek out"))
            reopened.setStarred("seek out", false)
            assertEquals(emptyMap<String, CollectionMark>(), reopened.collection())
        }
    }

    @Test
    fun `adding a collection mark requires a cached entry`() {
        WordStore(context).use { store ->
            try {
                store.setSaved("missing", true)
                throw AssertionError("a missing cached entry must not be added to collection")
            } catch (_: IllegalArgumentException) {
                // Expected: only cached dictionary entries can be explicitly collected.
            }
            assertEquals(emptyMap<String, CollectionMark>(), store.collection())
        }
    }

    @Test
    fun `daily topic selection survives closing and reopening`() {
        val selection = DailyTopics(
            day = "2026-09-07",
            topicIds = listOf("rhetoric", "systems", "inquiry"),
        )
        WordStore(context).use { store ->
            assertEquals(null, store.dailyTopics())
            store.saveDailyTopics(selection)
        }

        WordStore(context).use { reopened ->
            assertEquals(selection, reopened.dailyTopics())
        }
    }

    private fun createVersionOneDatabase(cachedOnly: WordEntry, studied: WordEntry) {
        val database = context.openOrCreateDatabase(WordStore.DATABASE_NAME, Context.MODE_PRIVATE, null)
        database.execSQL(
            "CREATE TABLE entries (word TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL)",
        )
        database.execSQL(
            "CREATE TABLE progress (word TEXT PRIMARY KEY NOT NULL, strength REAL NOT NULL, rated_at INTEGER NOT NULL, reviews INTEGER NOT NULL DEFAULT 1)",
        )
        database.execSQL("CREATE TABLE excluded (word TEXT PRIMARY KEY NOT NULL)")
        database.insertOrThrow("entries", null, entryValues(cachedOnly))
        database.insertOrThrow("entries", null, entryValues(studied))
        database.execSQL(
            "INSERT INTO progress(word, strength, rated_at, reviews) VALUES (?, ?, ?, ?)",
            arrayOf("studied", 20.0, 1234L, 3),
        )
        database.execSQL("INSERT INTO excluded(word) VALUES (?)", arrayOf("cached"))
        database.version = 1
        database.close()
    }

    private fun entryValues(entry: WordEntry) = ContentValues().apply {
        put("word", entry.word)
        put(
            "payload",
            """{"word":"${entry.word}","meanings":[{"partOfSpeech":"adjective","definitions":[{"text":"${entry.meanings.single().definitions.single().text}"}]}],"rawResponse":"${entry.rawResponse}"}""",
        )
    }

    private fun entry(word: String, definition: String) = WordEntry(
        word = word,
        meanings = listOf(Meaning("adjective", listOf(Definition(definition)))),
    )
}
