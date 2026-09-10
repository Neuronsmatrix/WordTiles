package com.wordtiles.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SearchField(state: AppState, viewModel: WordTilesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.searchQuery, onValueChange = { viewModel.updateSearch(it) },
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text("A word or phrase") }, shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.updateSearch("") },
                        modifier = Modifier.semantics { contentDescription = "Clear search" }) { Text("×") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.updateSearch(state.searchQuery, true) }))
            FilledTonalButton(onClick = { viewModel.updateSearch(state.searchQuery, true) },
                enabled = state.searchQuery.isNotBlank() && !state.loading,
                modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp)) { Text("Find") }
        }
    }
}
