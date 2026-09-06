package com.wordtiles.data

import android.content.Context
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

    private fun entry(word: String, definition: String) = WordEntry(
        word = word,
        meanings = listOf(Meaning("adjective", listOf(Definition(definition)))),
    )
}
