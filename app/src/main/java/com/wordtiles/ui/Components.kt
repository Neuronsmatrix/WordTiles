package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wordtiles.core.Progress
import com.wordtiles.core.strengthAt
import java.util.Locale

fun confidenceText(progress: Progress?, now: Long): String = when {
    progress == null -> "Not studied"
    strengthAt(progress, now) <= 0 -> "Needs review"
    else -> "Strength ${String.format(Locale.ROOT, "%.1f", strengthAt(progress, now))}"
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(Locale.ROOT), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
}

@Composable
fun ConfidenceBadge(progress: Progress?, now: Long) {
    Surface(shape = RoundedCornerShape(12.dp), color = if (progress == null)
        MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer) {
        Text(confidenceText(progress, now), Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun RatingButtons(enabled: Boolean, onRate: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("How sure are you about this word?", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { rating ->
                FilledTonalButton(onClick = { onRate(rating) }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(0.dp)) { Text(rating.toString()) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1 · Unsure", style = MaterialTheme.typography.labelSmall)
            Text("5 · Very sure", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun EmptyState(title: String, explanation: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
