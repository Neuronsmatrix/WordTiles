package com.wordtiles.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StudyScreen(state: AppState, viewModel: WordTilesViewModel) {
    val session = state.session ?: return
    val card = session.current
    if (card == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionLabel(if (session.cards.isEmpty()) "Nothing queued" else "Session complete")
            EmptyState(if (session.cards.isEmpty()) "A place to begin." else "A little more connected.",
                if (session.cards.isEmpty()) "Save or star words to review them, or download a topic to learn something new."
                else "${session.rated} words rated · ${session.skipped} skipped. Your progress is saved on this phone.")
            Button(onClick = viewModel::back, modifier = Modifier.fillMaxWidth()) { Text("Back to exploring") }
        }
        return
    }
    val rotation by animateFloatAsState(if (session.revealed) 180f else 0f, tween(280), label = "Card flip")
    val answerVisible = rotation >= 90f
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel(if (card.isNew) "New word" else "Review")
            Text("${session.index + 1} / ${session.cards.size}", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { session.index.toFloat() / session.cards.size }, modifier = Modifier.fillMaxWidth())
        RatingButtons(session.revealed && !state.saving, viewModel::rateCard)
        if (!session.revealed) Text("Tap the card to reveal its meanings before rating.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(onClick = viewModel::flipCard, enabled = !state.saving, shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().testTag("study-card")
                        .semantics {
                            stateDescription = if (session.revealed) "Meanings revealed. Tap to show word." else "Word face. Tap to reveal meanings."
                            onClick(label = if (session.revealed) "Show word" else "Reveal meanings") {
                                if (state.saving) false else { viewModel.flipCard(); true }
                            }
                        }
                        .graphicsLayer { rotationY = rotation; cameraDistance = 18f * density },
                    colors = CardDefaults.cardColors(containerColor = if (answerVisible)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 240.dp).graphicsLayer { rotationY = if (answerVisible) 180f else 0f }
                        .padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(card.word, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                        if (answerVisible) {
                            Text("All meanings · tap to flip back", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            state.entries[card.word]?.let { EntryBody(it) }
                        } else {
                            Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                                Text("Bring its meanings to mind.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("↻  Tap to flip", Modifier.align(Alignment.CenterHorizontally),
                                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = viewModel::skipCard, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text("Skip for this session") }
            }
        }
    }
}
