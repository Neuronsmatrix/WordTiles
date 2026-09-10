package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wordtiles.core.SessionMode
import com.wordtiles.core.strengthAt

@Composable
fun CollectionScreen(state: AppState, viewModel: WordTilesViewModel, now: Long) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val words = state.collection.filterValues { when (filter) { "Saved" -> it.saved; "Starred" -> it.starred; else -> it.included } }.keys
        .sortedWith(compareBy<String> { state.progress[it]?.let { progress -> strengthAt(progress, now) } ?: Double.NEGATIVE_INFINITY }.thenBy { it })
    val eligible = state.collectionWords.count { it in state.entries && it !in state.excluded }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            SectionLabel("Your growing vocabulary")
            Text("Keep the connections.", style = MaterialTheme.typography.displaySmall)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${state.collectionWords.size} collected · ${state.collection.values.count { it.starred }} starred", style = MaterialTheme.typography.titleLarge)
                    Text("Confidence fades by one point each day. Reviews begin with your weakest words.")
                    Button(onClick = { viewModel.startSession(SessionMode.REVIEW) }, enabled = eligible > 0,
                        modifier = Modifier.fillMaxWidth()) { Text("Review · up to ${minOf(10, eligible)} words") }
                    Text("All progress and saved entries stay on this phone.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Saved", "Starred").forEach { label ->
                    FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
                }
            }
        }
        if (words.isEmpty()) item {
            EmptyState(if (filter == "Starred") "Your favorites belong here." else "Room to grow.", "Save or star a word from its dictionary entry. Browsing topics does not add words here.")
        }
        items(words, key = { it }) { word -> WordRow(word, state, now) { viewModel.openWord(word) } }
    }
}
