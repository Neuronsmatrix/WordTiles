package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wordtiles.core.WordEntry
import com.wordtiles.core.canonicalWord

private data class RelationGroup(val label: String, val words: List<String>)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordNeighborhood(entry: WordEntry, state: AppState, now: Long, onWord: (String) -> Unit) {
    val groups = remember(entry) {
        buildList {
            entry.meanings.forEachIndexed { index, meaning ->
                val label = "${meaning.partOfSpeech} ${index + 1}"
                if (meaning.synonyms.isNotEmpty()) add(RelationGroup("$label · synonyms", meaning.synonyms))
                if (meaning.antonyms.isNotEmpty()) add(RelationGroup("$label · opposites", meaning.antonyms))
                meaning.definitions.forEachIndexed { definitionIndex, definition ->
                    val sense = "$label.${definitionIndex + 1}"
                    if (definition.synonyms.isNotEmpty()) add(RelationGroup("$sense · synonyms", definition.synonyms))
                    if (definition.antonyms.isNotEmpty()) add(RelationGroup("$sense · opposites", definition.antonyms))
                }
            }
        }
    }
    if (groups.isEmpty()) {
        Text("This entry has no synonym or opposite links yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var selected by rememberSaveable(entry.word) { mutableIntStateOf(0) }
    var page by rememberSaveable(entry.word, selected) { mutableIntStateOf(0) }
    val group = groups[selected.coerceIn(groups.indices)]
    val words = group.words.map(::canonicalWord).distinct().filter { it != canonicalWord(entry.word) }
    val pages = maxOf(1, (words.size + 11) / 12)
    val visiblePage = page.coerceIn(0, pages - 1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Explore its connections")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEachIndexed { index, relation ->
                FilterChip(selected = selected == index, onClick = { selected = index; page = 0 },
                    label = { Text(relation.label) })
            }
        }
        NeighborhoodGraph(entry.word, words.drop(visiblePage * 12).take(12), state.progress, now, onWord)
        if (pages > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { page = visiblePage - 1 }, enabled = visiblePage > 0) { Text("Previous") }
            Text("${visiblePage + 1} / $pages", Modifier.padding(12.dp))
            TextButton(onClick = { page = visiblePage + 1 }, enabled = visiblePage + 1 < pages) { Text("Next") }
        }
        Text("${group.label}. A connection suggests a related use; context determines whether words can be exchanged.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
