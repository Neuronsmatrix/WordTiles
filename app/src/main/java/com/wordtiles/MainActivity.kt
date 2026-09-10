package com.wordtiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordtiles.data.Catalog
import com.wordtiles.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordTilesTheme {
                WordTilesApp(viewModel(factory = ViewModelProvider.AndroidViewModelFactory(application)))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordTilesApp(viewModel: WordTilesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeTab = when {
        state.page is Page.Graph -> "Graph"
        state.page == Page.Quiz -> "Practice"
        state.pages.first() is Page.Graph -> "Graph"
        state.pages.first() == Page.Collection -> "Collection"
        state.pages.first() == Page.Quiz -> "Practice"
        else -> "Explore"
    }
    val snackbar = remember { SnackbarHostState() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDailyTopics()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.page, state.progress) {
        while (true) { now = System.currentTimeMillis(); viewModel.refreshDailyTopics(); delay(60_000) }
    }
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbar.showSnackbar(message, withDismissAction = true)
            viewModel.dismissMessage()
        }
    }
    BackHandler(enabled = state.pages.size > 1) { viewModel.back() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text("wordtiles", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (state.pages.size > 1) TextButton(onClick = viewModel::back, enabled = !state.saving) { Text("‹ Back") }
                },
                actions = { Text("ON DEVICE", Modifier.padding(end = 20.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) })
        },
        bottomBar = {
            if (state.page != Page.Study) NavigationBar {
                NavigationBarItem(selected = activeTab == "Explore",
                    onClick = { viewModel.switchTab(Page.Explore) }, icon = { Text("✦") }, label = { Text("Explore") })
                NavigationBarItem(selected = activeTab == "Collection",
                    onClick = { viewModel.switchTab(Page.Collection) }, icon = { Text("▤") }, label = { Text("Collection") })
                NavigationBarItem(selected = activeTab == "Graph",
                    onClick = { viewModel.switchTab(Page.Graph()) }, icon = { Text("◎") }, label = { Text("Graph") })
                NavigationBarItem(selected = activeTab == "Practice",
                    onClick = { viewModel.switchTab(Page.Quiz) }, icon = { Text("◇") }, label = { Text("Practice") })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else key(state.page) {
                    when (val page = state.page) {
                        Page.Explore -> ExploreScreen(state, viewModel, now)
                        Page.Collection -> CollectionScreen(state, viewModel, now)
                        is Page.Graph -> GraphScreen(page, state, viewModel, now)
                        is Page.TopicDetail -> Catalog.topics.find { it.id == page.id }?.let { TopicScreen(it, state, viewModel, now) }
                        is Page.Entry -> EntryScreen(page.word, state, viewModel, now)
                        Page.Study -> key(state.session?.index) { StudyScreen(state, viewModel) }
                        Page.Quiz -> key(state.quizIndex) { QuizScreen(state, viewModel) }
                    }
                }
            }
        }
    }
}
