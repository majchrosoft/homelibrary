package com.majchrosoft.homelibrary.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.ItemQuality
import com.majchrosoft.homelibrary.domain.model.ItemType
import com.majchrosoft.homelibrary.presentation.item.ItemEditIntent
import com.majchrosoft.homelibrary.presentation.item.ItemEditViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(itemId: String?) {
    val viewModel = koinInject<ItemEditViewModel> { parametersOf(itemId) }
    val navigator = koinInject<Navigator>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navigator.back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "Add item" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.dispatch(ItemEditIntent.TitleChanged(it)) },
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.author,
                onValueChange = { viewModel.dispatch(ItemEditIntent.AuthorChanged(it)) },
                label = { Text("Author") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            EnumDropdown(
                label = "Type",
                values = ItemType.entries,
                selected = state.type,
                toLabel = { it.name.lowercase() },
                onSelect = { viewModel.dispatch(ItemEditIntent.TypeChanged(it)) },
            )

            EnumDropdown(
                label = "Quality",
                values = ItemQuality.entries,
                selected = state.quality,
                toLabel = { it.name.lowercase() },
                onSelect = { viewModel.dispatch(ItemEditIntent.QualityChanged(it)) },
            )

            BookcaseDropdown(
                bookcases = state.bookcases,
                selectedId = state.selectedBookcaseId,
                onSelect = { viewModel.dispatch(ItemEditIntent.BookcaseSelected(it)) },
            )

            OutlinedTextField(
                value = state.isbn,
                onValueChange = { viewModel.dispatch(ItemEditIntent.IsbnChanged(it)) },
                label = { Text("ISBN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.publisher,
                onValueChange = { viewModel.dispatch(ItemEditIntent.PublisherChanged(it)) },
                label = { Text("Publisher") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.publishedYear,
                    onValueChange = { viewModel.dispatch(ItemEditIntent.PublishedYearChanged(it)) },
                    label = { Text("Year") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.pages,
                    onValueChange = { viewModel.dispatch(ItemEditIntent.PagesChanged(it)) },
                    label = { Text("Pages") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.language,
                onValueChange = { viewModel.dispatch(ItemEditIntent.LanguageChanged(it)) },
                label = { Text("Language") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.coverUrl,
                onValueChange = { viewModel.dispatch(ItemEditIntent.CoverUrlChanged(it)) },
                label = { Text("Cover URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.dispatch(ItemEditIntent.NotesChanged(it)) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Share on public catalog", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Allows other users to borrow this item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.shareable,
                    onCheckedChange = { viewModel.dispatch(ItemEditIntent.ShareableChanged(it)) },
                )
            }

            if (state.errorMessage != null) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.dispatch(ItemEditIntent.Save) },
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (itemId == null) "Add to library" else "Save changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    values: List<T>,
    selected: T,
    toLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = toLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open $label")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(toLabel(value)) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookcaseDropdown(
    bookcases: List<Bookcase>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = bookcases.firstOrNull { it.id == selectedId }?.name ?: "—"
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Bookcase") },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open bookcase picker")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("— None —") }, onClick = {
                onSelect(null)
                expanded = false
            })
            bookcases.forEach { bookcase ->
                DropdownMenuItem(
                    text = { Text(bookcase.name.ifBlank { "Untitled" }) },
                    onClick = {
                        onSelect(bookcase.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
