package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StudyScreen(state: AppState, viewModel: WordTilesViewModel) {
    val session = state.session ?: return
    val card = session.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (card == null) {
            item {
                SectionLabel(if (session.cards.isEmpty()) "Nothing queued" else "Session complete")
                EmptyState(if (session.cards.isEmpty()) "A place to begin." else "A little more connected.",
                    if (session.cards.isEmpty()) "Download a topic to learn new words. Words you rate will be ready for review here."
                    else "${session.rated} words rated · ${session.skipped} skipped. Your progress is saved on this phone.")
                Button(onClick = viewModel::back, modifier = Modifier.fillMaxWidth()) { Text("Back to exploring") }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel(if (card.isNew) "New word" else "Review")
                    Text("${session.index + 1} / ${session.cards.size}", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { session.index.toFloat() / session.cards.size }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Card(shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 42.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(card.word, Modifier.fillMaxWidth(), style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center)
                        if (!session.revealed) Text("Bring its meanings to mind.", Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (session.revealed) {
                state.entries[card.word]?.let { entry -> item { EntryBody(entry) } }
                item { RatingButtons(!state.saving, viewModel::rateCard) }
                if (state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            } else item {
                Button(onClick = viewModel::reveal, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Reveal all meanings")
                }
            }
            item {
                TextButton(onClick = viewModel::skipCard, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip for this session")
                }
            }
        }
    }
}
