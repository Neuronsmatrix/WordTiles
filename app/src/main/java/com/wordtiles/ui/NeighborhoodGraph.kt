package com.wordtiles.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordtiles.core.Progress
import com.wordtiles.core.strengthAt

/** A bounded local graph. Composable nodes retain accessible click and text semantics. */
@Composable
fun NeighborhoodGraph(
    title: String,
    words: List<String>,
    progress: Map<String, Progress>,
    now: Long,
    onWord: (String) -> Unit,
) {
    val shown = words.take(12)
    val rows = (shown.size + 1) / 2
    val height = maxOf(240, rows * 80 + 40).dp
    val edgeColor = MaterialTheme.colorScheme.outlineVariant
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(height).padding(12.dp)) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                shown.forEachIndexed { index, _ ->
                    val row = index / 2
                    val point = Offset(size.width * (if (index % 2 == 0) 0.17f else 0.83f),
                        size.height * ((row + 0.5f) / maxOf(rows, 1)))
                    drawLine(edgeColor, center, point, strokeWidth = 2.dp.toPx())
                }
            }
            Surface(Modifier.align(Alignment.Center).width(86.dp).heightIn(min = 76.dp),
                shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    Text(title, Modifier.padding(10.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            shown.forEachIndexed { index, word ->
                val learned = progress[word]
                val color = when {
                    learned == null -> MaterialTheme.colorScheme.surface
                    strengthAt(learned, now) <= 10 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                Surface(onClick = { onWord(word) }, shape = RoundedCornerShape(16.dp), color = color,
                    tonalElevation = 1.dp,
                    modifier = Modifier.offset(
                        x = if (index % 2 == 0) 0.dp else w - w * 0.33f,
                        y = h * ((index / 2 + 0.5f) / maxOf(rows, 1)) - 30.dp,
                    ).width(w * 0.33f).heightIn(min = 60.dp)) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text(word, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                        Text(if (learned == null) "new" else if (strengthAt(learned, now) <= 0) "review" else "learning",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
