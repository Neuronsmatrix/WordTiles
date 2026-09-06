package com.wordtiles.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.wordtiles.MainActivity
import com.wordtiles.core.Definition
import com.wordtiles.core.Meaning
import com.wordtiles.core.WordEntry
import com.wordtiles.data.WordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LearningFlowTest {
    @get:Rule(order = 0)
    val seed = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase(WordStore.DATABASE_NAME)
            WordStore(context).use { store ->
                store.saveEntry(WordEntry("cogent", meanings = listOf(Meaning("adjective", listOf(
                    Definition("Clear and convincing.", "A cogent argument."),
                    Definition("Compelling the mind to assent."),
                )))))
                store.saveEntry(WordEntry("equivocal", meanings = listOf(Meaning("adjective", listOf(
                    Definition("Open to more than one interpretation."),
                )))))
            }
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    @Config(qualifiers = "w411dp-h891dp-mdpi")
    fun revealSurvivesRotationAndRatingPersistsTheWholeWord() {
        awaitExplore()
        capture("explore")
        compose.onAllNodesWithText("Rhetoric & Argument").onFirst().performClick()
        capture("topic")
        scrollToText("Pure learn · 2 new").performClick()
        compose.onNodeWithText("1 · Unsure").assertDoesNotExist()
        compose.onNodeWithText("Reveal all meanings").performClick()
        scrollToText("1. Clear and convincing.").assertExists()
        compose.onNodeWithText("2. Compelling the mind to assent.").assertExists()

        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Reveal all meanings").assertDoesNotExist()
        scrollToText("5").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("equivocal").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("equivocal").assertIsDisplayed()
        scrollToText("Skip for this session").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("SESSION COMPLETE").fetchSemanticsNodes().isNotEmpty()
        }
        WordStore(ApplicationProvider.getApplicationContext()).use { store ->
            assertEquals(setOf("cogent"), store.progress().keys)
            assertEquals(100.0, store.progress().getValue("cogent").strength, 0.0)
            assertEquals(1, store.progress().getValue("cogent").reviews)
            assertEquals(2, store.entries().getValue("cogent").meanings.single().definitions.size)
        }
    }

    @Test fun skippingNeverEnrollsTheNewWord() {
        awaitExplore()
        compose.onAllNodesWithText("Rhetoric & Argument").onFirst().performClick()
        scrollToText("Pure learn · 2 new").performClick()
        scrollToText("Skip for this session").performClick()
        compose.onNodeWithText("equivocal").assertIsDisplayed()
        scrollToText("Skip for this session").performClick()
        compose.onNodeWithText("SESSION COMPLETE").assertExists()
        WordStore(ApplicationProvider.getApplicationContext()).use { store ->
            assertTrue(store.progress().isEmpty())
        }
    }

    @Test fun quizAcceptsBothSuitableExpressionsAndExplainsTheDistractor() {
        awaitExplore()
        compose.onNodeWithText("Practice").performClick()
        scrollToText("seek").performClick()
        scrollToText("look for").performClick()
        scrollToText("Check answers").performClick()
        scrollToText("All the right connections.").assertIsDisplayed()
        compose.onNodeWithText("0 missed · 0 extra selections").assertExists()
        scrollToText("Next question").performClick()
        compose.onNodeWithText("Find every fit.").assertIsDisplayed()
        WordStore(ApplicationProvider.getApplicationContext()).use { store ->
            assertTrue(store.progress().isEmpty())
        }
    }

    private fun scrollToText(text: String): SemanticsNodeInteraction {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        return compose.onNodeWithText(text)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File("build/reports/screenshots").apply { mkdirs() }
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(directory, "$name.png").outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    private fun awaitExplore() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Follow your curiosity.").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
