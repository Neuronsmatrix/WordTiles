package com.wordtiles.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.wordtiles.core.Definition
import com.wordtiles.core.Meaning
import com.wordtiles.core.WordEntry
import com.wordtiles.data.DictionaryNetworkException
import com.wordtiles.data.DictionaryProvider
import com.wordtiles.data.WordRepository
import com.wordtiles.data.WordStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LookupNavigationTest {
    private lateinit var application: Application
    private lateinit var mainScheduler: TestCoroutineScheduler
    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.deleteDatabase(WordStore.DATABASE_NAME)
        mainScheduler = TestCoroutineScheduler()
        mainDispatcher = StandardTestDispatcher(mainScheduler)
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        application.deleteDatabase(WordStore.DATABASE_NAME)
    }

    @Test
    fun `back from a failed lookup does not show its error on the previous cached entry`() {
        WordStore(application).use { it.saveEntry(entry("alpha")) }
        val repository = repository(DictionaryProvider { word ->
            throw DictionaryNetworkException("Offline while looking up $word.")
        })

        try {
            val viewModel = WordTilesViewModel(application, repository)
            mainScheduler.advanceUntilIdle()
            viewModel.openWord("alpha")
            viewModel.openWord("beta")
            mainScheduler.advanceUntilIdle()
            assertEquals("Offline while looking up beta.", viewModel.state.value.entryError)

            viewModel.back()

            assertEquals(Page.Entry("alpha"), viewModel.state.value.page)
            assertNull(viewModel.state.value.entryError)
            assertNull(viewModel.state.value.loadingWord)
        } finally {
            repository.close()
        }
    }

    @Test
    fun `switching tabs invalidates an unfinished lookup`() {
        val provider = DeferredProvider()
        val repository = repository(provider)

        try {
            val viewModel = WordTilesViewModel(application, repository)
            mainScheduler.advanceUntilIdle()
            viewModel.openWord("beta")
            mainScheduler.runCurrent()
            assertEquals("beta", viewModel.state.value.loadingWord)

            viewModel.switchTab(Page.Collection)
            provider.result.completeExceptionally(DictionaryNetworkException("Late failure."))
            mainScheduler.advanceUntilIdle()

            assertEquals(Page.Collection, viewModel.state.value.page)
            assertNull(viewModel.state.value.entryError)
            assertNull(viewModel.state.value.loadingWord)
        } finally {
            repository.close()
        }
    }

    @Test
    fun `canceled same-word request cannot clear the newer request loading state`() {
        val provider = SameWordProvider()
        val repository = repository(provider)

        try {
            val viewModel = WordTilesViewModel(application, repository)
            mainScheduler.advanceUntilIdle()
            viewModel.openWord("beta", refresh = true)
            mainScheduler.runCurrent()
            assertEquals("beta", viewModel.state.value.loadingWord)

            viewModel.openWord("beta", refresh = true)
            mainScheduler.runCurrent()
            assertEquals("beta", viewModel.state.value.loadingWord)

            provider.releaseFirstCancellation.complete(Unit)
            mainScheduler.runCurrent()
            val loadingAfterCanceledRequestFinished = viewModel.state.value.loadingWord

            provider.secondResult.complete(entry("beta"))
            mainScheduler.advanceUntilIdle()

            assertEquals("beta", loadingAfterCanceledRequestFinished)
            assertEquals(entry("beta"), viewModel.state.value.entries["beta"])
            assertNull(viewModel.state.value.loadingWord)
        } finally {
            repository.close()
        }
    }

    private fun repository(provider: DictionaryProvider) = WordRepository(
        context = application,
        provider = provider,
        nowMillis = { 0L },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun entry(word: String) = WordEntry(
        word = word,
        meanings = listOf(Meaning("noun", listOf(Definition("Definition for $word.")))),
    )

    private class DeferredProvider : DictionaryProvider {
        val result = CompletableDeferred<WordEntry>()

        override suspend fun lookup(word: String): WordEntry = result.await()
    }

    private class SameWordProvider : DictionaryProvider {
        val releaseFirstCancellation = CompletableDeferred<Unit>()
        val secondResult = CompletableDeferred<WordEntry>()
        private var calls = 0

        override suspend fun lookup(word: String): WordEntry {
            return when (calls++) {
                0 -> try {
                    awaitCancellation()
                } catch (error: kotlinx.coroutines.CancellationException) {
                    withContext(NonCancellable) { releaseFirstCancellation.await() }
                    throw error
                }
                else -> secondResult.await()
            }
        }
    }
}
