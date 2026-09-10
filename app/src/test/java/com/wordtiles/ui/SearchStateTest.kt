package com.wordtiles.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.wordtiles.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class SearchStateTest {
    private lateinit var app: Application
    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var repository: WordRepository

    @Before fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.deleteDatabase(WordStore.DATABASE_NAME)
        scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        repository = WordRepository(app, DictionaryProvider { error("Search must not request dictionary definitions.") }, { 0L }, Dispatchers.Unconfined)
    }

    @After fun cleanup() {
        repository.close()
        Dispatchers.resetMain()
        app.deleteDatabase(WordStore.DATABASE_NAME)
    }

    @Test fun `late results never replace a newer query or resurrect a cleared search`() {
        val old = CompletableDeferred<List<String>>()
        val current = CompletableDeferred<List<String>>()
        val viewModel = WordTilesViewModel(app, repository, StandardTestDispatcher(scheduler)) { query, _ ->
            withContext(NonCancellable) { if (query == "loook") old.await() else current.await() }
        }
        scheduler.advanceUntilIdle()
        viewModel.updateSearch("loook", true)
        scheduler.runCurrent()
        viewModel.updateSearch("seek", true)
        scheduler.runCurrent()
        current.complete(listOf("seek"))
        scheduler.runCurrent()
        assertEquals(listOf("seek"), viewModel.state.value.suggestions)
        viewModel.updateSearch("")
        old.complete(listOf("look"))
        scheduler.advanceUntilIdle()
        assertEquals("", viewModel.state.value.searchQuery)
        assertTrue(viewModel.state.value.suggestions.isEmpty())
        assertFalse(viewModel.state.value.searching)
        assertTrue(viewModel.state.value.collection.isEmpty())
    }

    @Test fun `typing is debounced and does not save any suggested word`() {
        var searches = 0
        val viewModel = WordTilesViewModel(app, repository, StandardTestDispatcher(scheduler)) { _, _ -> searches++; listOf("look for") }
        scheduler.advanceUntilIdle()
        viewModel.updateSearch("lo")
        scheduler.advanceTimeBy(100)
        viewModel.updateSearch("loook for")
        scheduler.advanceTimeBy(249)
        scheduler.runCurrent()
        assertEquals(0, searches)
        scheduler.advanceUntilIdle()
        assertEquals(1, searches)
        assertEquals(listOf("look for"), viewModel.state.value.suggestions)
        assertTrue(viewModel.state.value.collection.isEmpty())
    }
}
