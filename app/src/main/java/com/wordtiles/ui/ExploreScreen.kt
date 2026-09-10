package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordtiles.core.SessionMode
import com.wordtiles.data.Catalog
import com.wordtiles.data.Topic

@Composable
fun ExploreScreen(state: AppState, viewModel: WordTilesViewModel, now: Long) {
    val dailyTopics = state.dailyTopics?.topicIds.orEmpty().mapNotNull { id -> Catalog.topics.find { it.id == id } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            if (state.searchQuery.isBlank()) {
            SectionLabel("A vocabulary of connections")
            Spacer(Modifier.height(8.dp))
            Text("Follow your curiosity.", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text("Explore a topic, uncover its words, and keep what you learn.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else Text("Find a word.", style = MaterialTheme.typography.headlineMedium)
        }
        item { SearchField(state, viewModel) }
        if (state.searchQuery.isNotBlank()) {
            item {
                SectionLabel("Dictionary suggestions")
                if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                state.searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!state.searching && state.suggestions.isEmpty() && state.searchError == null) {
                    Text("No close matches. Try another spelling or look up the exact text.", Modifier.padding(top = 8.dp))
                }
            }
            items(state.suggestions, key = { it }) { word ->
                Surface(onClick = { viewModel.openWord(word) }, shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(word, style = MaterialTheme.typography.titleMedium)
                        Text("→", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                TextButton(onClick = { viewModel.openWord(state.searchQuery) }) { Text("Look up “${state.searchQuery.trim()}” exactly") }
            }
        } else {
            item {
                SectionLabel("Today's 10 topics")
                Spacer(Modifier.height(6.dp))
                Text("A fresh selection each day. Save or star the words you want to keep.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (dailyTopics.isEmpty()) item { Text("Preparing today's topics…") }
            items(dailyTopics, key = { it.id }) { topic ->
                TopicCard(topic, state) { viewModel.navigate(Page.TopicDetail(topic.id)) }
            }
        }
        item {
            OutlinedCard(onClick = { viewModel.navigate(Page.Quiz) }, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Practice in context")
                    Text("One meaning. More than one answer.", style = MaterialTheme.typography.titleLarge)
                    Text("Try a small set of checked questions about similar expressions.")
                    Text("Open quiz →", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: Topic, state: AppState, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(topic.title, style = MaterialTheme.typography.titleLarge)
            Text(topic.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${topic.words.size} words · ${topic.words.count { it in state.progress }} studied · ${topic.words.count { it in state.collectionWords }} collected",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun TopicScreen(topic: Topic, state: AppState, viewModel: WordTilesViewModel, now: Long) {
    val saved = topic.words.count { it in state.entries }
    val newAvailable = topic.words.count { it in state.entries && it !in state.progress && it !in state.excluded }
    val reviewing = state.collectionWords.any { it in state.entries && it !in state.excluded }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            SectionLabel("Topic neighborhood")
            Text(topic.title, style = MaterialTheme.typography.displaySmall)
            Text(topic.subtitle, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedButton(onClick = { viewModel.navigate(Page.Graph(topicId = topic.id)) }) { Text("View topic graph") }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$saved / ${topic.words.size} entries on your phone", style = MaterialTheme.typography.titleMedium)
                    if (state.downloadingTopic == topic.id) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(state.downloadStatus)
                        TextButton(onClick = viewModel::cancelDownload) { Text("Stop download") }
                    } else if (saved < topic.words.size) {
                        Text("Download entries to browse and study offline. Only words you save, star, or rate join Collection.")
                        Button(onClick = { viewModel.downloadTopic(topic) }, enabled = state.downloadingTopic == null) {
                            Text(if (saved == 0) "Download topic" else "Download missing entries")
                        }
                    }
                    Button(onClick = { viewModel.startSession(SessionMode.MIXED, topic) },
                        enabled = (reviewing || newAvailable > 0) && !state.loading,
                        modifier = Modifier.fillMaxWidth()) { Text("Review + new topic") }
                    OutlinedButton(onClick = { viewModel.startSession(SessionMode.LEARN, topic) },
                        enabled = newAvailable > 0 && !state.loading,
                        modifier = Modifier.fillMaxWidth()) { Text("Pure learn · $newAvailable new") }
                    Text("Reviews draw from your whole collection. Each batch has up to 10 reviews and 10 new words.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionLabel("Words in this topic") }
        items(topic.words, key = { it }) { word ->
            WordRow(word, state, now) { viewModel.openWord(word) }
        }
    }
}

@Composable
fun WordRow(word: String, state: AppState, now: Long, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(word, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(if (state.collection[word]?.starred == true) "★ Starred" else if (state.collection[word]?.saved == true) "Saved" else if (word in state.entries) "Downloaded" else "Get entry", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (word in state.excluded) Text("Excluded from study", style = MaterialTheme.typography.labelSmall)
            else ConfidenceBadge(state.progress[word], now)
        }
    }
}
