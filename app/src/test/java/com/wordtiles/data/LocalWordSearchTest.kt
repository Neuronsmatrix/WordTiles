package com.wordtiles.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalWordSearchTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `bundled WordNet phrase corrects repeated character typo`() = runTest {
        val results = LocalWordSearch(context).suggest("loook for")

        assertEquals("look for", results.firstOrNull())
    }

    @Test
    fun `bundled search includes a WordNet lemma outside the starter catalogue`() = runTest {
        val results = LocalWordSearch(context).suggest("aardwlf")

        assertEquals("aardwolf", results.firstOrNull())
        assertTrue(Catalog.topics.none { "aardwolf" in it.words })
    }

    @Test
    fun `additional cached and relation words participate in the same ranking`() = runTest {
        val results = LocalWordSearch(context).suggest(
            query = "qourblax",
            additionalWords = listOf("quorblax", "unrelated"),
        )

        assertEquals("quorblax", results.firstOrNull())
    }

    @Test
    fun `empty query does not load or invent suggestions`() = runTest {
        assertEquals(emptyList<String>(), LocalWordSearch(context).suggest("  \t "))
    }
}
