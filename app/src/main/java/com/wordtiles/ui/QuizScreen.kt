package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.wordtiles.data.Catalog

@Composable
fun QuizScreen(state: AppState, viewModel: WordTilesViewModel) {
    val question = Catalog.quizzes[state.quizIndex]
    val result = state.quizResult
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            SectionLabel("Context practice · ${state.quizIndex + 1} / ${Catalog.quizzes.size}")
            Text("Find every fit.", style = MaterialTheme.typography.displaySmall)
            Text("Select all expressions that match both the meaning and the sentence.",
                Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(question.definition, style = MaterialTheme.typography.titleLarge)
                    Text("“${question.example}”", fontStyle = FontStyle.Italic)
                }
            }
        }
        items(question.options, key = { it.id }) { option ->
            val isAnswer = option.id in question.correctIds
            val chosen = option.id in state.quizSelection
            val color = when {
                result != null && isAnswer -> MaterialTheme.colorScheme.primaryContainer
                result != null && chosen -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
            Surface(shape = RoundedCornerShape(16.dp), color = color) {
                Column(Modifier.fillMaxWidth().toggleable(value = chosen, enabled = result == null,
                    role = Role.Checkbox, onValueChange = { viewModel.selectQuiz(option.id) }).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = chosen, onCheckedChange = null, enabled = result == null)
                        Text(option.text, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    if (result != null) {
                        Text(if (isAnswer) "Suitable answer" else "Does not fit here", style = MaterialTheme.typography.labelMedium)
                        Text(option.explanation, Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        if (result == null) item {
            Button(onClick = viewModel::checkQuiz, enabled = state.quizSelection.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()) { Text("Check answers") }
        } else {
            item {
                Text(if (result.correct) "All the right connections." else "Look at the distinctions.",
                    style = MaterialTheme.typography.headlineMedium)
                Text("${result.missed.size} missed · ${result.extra.size} extra selections",
                    Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                if (!state.quizRated) {
                    Text("Optional: update your confidence in “${question.targetWord}”.",
                        Modifier.padding(bottom = 12.dp))
                    RatingButtons(!state.saving, viewModel::rateQuiz)
                    if (question.targetWord !in state.entries) Text("Its full dictionary entry will be downloaded before saving your first rating.",
                        Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                } else Text("Confidence saved.", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::nextQuiz, enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()) { Text("Next question") }
            }
        }
        item {
            Text("A small, curated practice set. Synonyms are checked in context; they are not always interchangeable.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
