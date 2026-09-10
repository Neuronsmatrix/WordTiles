package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wordtiles.data.Catalog

@Composable
fun GraphScreen(page: Page.Graph, state: AppState, viewModel: WordTilesViewModel, now: Long) {
    var offset by rememberSaveable(page) { mutableIntStateOf(0) }
    val topic = Catalog.topics.find { it.id == page.topicId }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            SectionLabel("Your vocabulary map")
            Text(page.word ?: topic?.title ?: "Connections.", style = MaterialTheme.typography.displaySmall)
            Text("Follow a topic or a word to explore its neighborhood.", Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (page.word != null) {
            val entry = state.entries[page.word]
            if (entry != null) item {
                WordNeighborhood(entry, state, now, viewModel::openWord)
                Button(onClick = { viewModel.openWord(page.word) }) { Text("Open dictionary entry") }
            } else item { TextButton(onClick = { viewModel.openWord(page.word) }) { Text("Look up this word") } }
        } else {
            val words = topic?.words ?: Catalog.topics.map { it.title }
            val pages = maxOf(1, (words.size + 11) / 12)
            val visible = words.drop(offset * 12).take(12)
            item {
                NeighborhoodGraph(topic?.title ?: "WordTiles", visible, if (topic == null) emptyMap() else state.progress, now) { word ->
                    if (topic == null) viewModel.navigate(Page.Graph(topicId = Catalog.topics.first { it.title == word }.id))
                    else viewModel.openWord(word)
                }
            }
            if (pages > 1) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { offset-- }, enabled = offset > 0) { Text("Previous") }
                    Text("${offset + 1} / $pages", Modifier.padding(12.dp))
                    TextButton(onClick = { offset++ }, enabled = offset + 1 < pages) { Text("Next") }
                }
            }
            if (topic != null) item {
                Button(onClick = { viewModel.navigate(Page.TopicDetail(topic.id)) }, modifier = Modifier.fillMaxWidth()) { Text("Open topic") }
            }
        }
    }
}
