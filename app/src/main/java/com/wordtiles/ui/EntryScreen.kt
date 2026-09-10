package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.wordtiles.core.WordEntry
import com.wordtiles.data.Catalog

@Composable
fun EntryScreen(word: String, state: AppState, viewModel: WordTilesViewModel, now: Long) {
    val entry = state.entries[word]
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            SectionLabel("Dictionary & connections")
            Text(word, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            ConfidenceBadge(state.progress[word], now)
        }
        if (state.loadingWord == word) item {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Getting the complete entry…", Modifier.padding(top = 8.dp))
        }
        state.entryError?.let { error -> item {
            Text(error, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { viewModel.openWord(word, true) }) { Text("Retry lookup") }
        } }
        if (entry != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = state.collection[word]?.saved == true, enabled = !state.saving,
                        onClick = { viewModel.toggleSaved(word) }, label = { Text(if (state.collection[word]?.saved == true) "Saved" else "Save word") })
                    FilterChip(selected = state.collection[word]?.starred == true, enabled = !state.saving,
                        onClick = { viewModel.toggleStarred(word) }, label = { Text(if (state.collection[word]?.starred == true) "★ Starred" else "☆ Star word") })
                }
                RatingButtons(!state.saving) { rating -> viewModel.rateEntry(word, rating) }
            }
            item { EntryBody(entry, viewModel::openWord) }
            item {
                OutlinedButton(onClick = { viewModel.navigate(Page.Graph(word = word)) }) { Text("View connections graph") }
            }
            item {

                OutlinedButton(onClick = { viewModel.toggleExcluded(word) }, enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (word in state.excluded) "Include in study again" else "Exclude from study")
                }
                TextButton(onClick = { viewModel.openWord(word, true) }, enabled = state.loadingWord != word) {
                    Text("Refresh dictionary entry")
                }
            }
        }
        val topics = Catalog.topics.filter { word in it.words }
        if (topics.isNotEmpty()) item {
            SectionLabel("Also belongs to")
            topics.forEach { topic ->
                TextButton(onClick = { viewModel.navigate(Page.TopicDetail(topic.id)) }) { Text("${topic.title} →") }
            }
        }
    }
}

@Composable
fun EntryBody(entry: WordEntry, onWord: ((String) -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (entry.phonetic.isNotBlank()) Text(entry.phonetic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        entry.meanings.forEach { meaning ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel(meaning.partOfSpeech.ifBlank { "Meaning" })
                meaning.definitions.forEachIndexed { index, definition ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${index + 1}. ${definition.text}", style = MaterialTheme.typography.bodyLarge)
                        if (definition.example.isNotBlank()) Text("“${definition.example}”",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                        if (onWord != null) {
                            Relations("Synonyms for this meaning", definition.synonyms, onWord)
                            Relations("Opposites for this meaning", definition.antonyms, onWord)
                        }
                    }
                }
                if (onWord != null) {
                    Relations("Related synonyms · ${meaning.partOfSpeech}", meaning.synonyms, onWord)
                    Relations("Related opposites · ${meaning.partOfSpeech}", meaning.antonyms, onWord)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (entry.sources.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Dictionary sources")
                entry.sources.forEach { source ->
                    if (source.url.startsWith("https://") || source.url.startsWith("http://")) {
                        TextButton(onClick = { runCatching { uriHandler.openUri(source.url) } },
                            contentPadding = PaddingValues(vertical = 4.dp)) {
                            Text(if (source.url == "https://freedictionaryapi.com/") "Dictionary data via FreeDictionaryAPI.com"
                                else source.url.removePrefix("https://").removePrefix("http://"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (source.licenseName.isNotBlank()) {
                        if (source.licenseUrl.startsWith("https://") || source.licenseUrl.startsWith("http://"))
                            TextButton(onClick = { runCatching { uriHandler.openUri(source.licenseUrl) } },
                                contentPadding = PaddingValues(0.dp)) { Text(source.licenseName, style = MaterialTheme.typography.labelSmall) }
                        else Text(source.licenseName, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Relations(label: String, words: List<String>, onWord: (String) -> Unit) {
    if (words.isEmpty()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            words.distinct().forEach { word -> AssistChip(onClick = { onWord(word) }, label = { Text(word) }) }
        }
    }
}
