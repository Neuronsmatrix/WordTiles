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
    var showAll by rememberSaveable { mutableStateOf(false) }
    val words = if (showAll) state.entries.keys.sorted() else state.progress.values
        .sortedBy { strengthAt(it, now) }.map { it.word }
    val eligible = state.progress.keys.count { it in state.entries && it !in state.excluded }
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
                    Text("${state.progress.size} studied · ${state.entries.size} downloaded", style = MaterialTheme.typography.titleLarge)
                    Text("Confidence fades by one point each day. Reviews begin with your weakest words.")
                    Button(onClick = { viewModel.startSession(SessionMode.REVIEW) }, enabled = eligible > 0,
                        modifier = Modifier.fillMaxWidth()) { Text("Review · up to ${minOf(10, eligible)} words") }
                    Text("All progress and saved entries stay on this phone.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showAll, onClick = { showAll = false }, label = { Text("Weakest first") })
                FilterChip(selected = showAll, onClick = { showAll = true }, label = { Text("All downloaded") })
            }
        }
        if (words.isEmpty()) item {
            EmptyState("Room to grow.", "Explore a topic and download its words. Reveal an entry and rate your confidence to start learning.")
        }
        items(words, key = { it }) { word -> WordRow(word, state, now) { viewModel.openWord(word) } }
    }
}
