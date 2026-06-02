package com.majchrosoft.homelibrary.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.presentation.item.ItemDetailIntent
import com.majchrosoft.homelibrary.presentation.item.ItemDetailViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.navigation.Screen
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(itemId: String) {
    val viewModel = koinInject<ItemDetailViewModel> { parametersOf(itemId) }
    val navigator = koinInject<Navigator>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.deleted) {
        if (state.deleted) navigator.back()
    }

    var confirmDelete by remember { mutableStateOf(false) }
    var borrowDialog by remember { mutableStateOf(false) }
    var borrowedBy by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.item?.title?.ifBlank { "Item" } ?: "Item") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.item != null) {
                        IconButton(onClick = { navigator.push(Screen.ItemEdit(itemId = itemId)) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.item != null -> ItemDetailContent(
                item = state.item!!,
                bookcaseName = state.bookcaseName,
                error = state.errorMessage,
                contentPadding = padding,
                onBorrowToggle = {
                    if (state.item!!.borrow.isBorrowed) {
                        viewModel.dispatch(ItemDetailIntent.ToggleBorrow(null))
                    } else {
                        borrowedBy = ""
                        borrowDialog = true
                    }
                },
                onShareableToggle = { viewModel.dispatch(ItemDetailIntent.ToggleShareable) },
            )

            else -> Text(
                text = state.errorMessage ?: "Item not found",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.dispatch(ItemDetailIntent.Delete)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            title = { Text("Delete this item?") },
            text = { Text("This will remove it from your library. The action can't be undone.") },
        )
    }

    if (borrowDialog) {
        AlertDialog(
            onDismissRequest = { borrowDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    borrowDialog = false
                    viewModel.dispatch(ItemDetailIntent.ToggleBorrow(borrowedBy.trim()))
                }) { Text("Mark as borrowed") }
            },
            dismissButton = { TextButton(onClick = { borrowDialog = false }) { Text("Cancel") } },
            title = { Text("Who's borrowing it?") },
            text = {
                OutlinedTextField(
                    value = borrowedBy,
                    onValueChange = { borrowedBy = it },
                    label = { Text("Borrower (name or user id)") },
                    singleLine = true,
                )
            },
        )
    }
}

@Composable
private fun ItemDetailContent(
    item: Item,
    bookcaseName: String?,
    error: String?,
    contentPadding: PaddingValues,
    onBorrowToggle: () -> Unit,
    onShareableToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(item.item.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.headlineSmall)
        if (item.item.author.isNotBlank()) {
            Text(item.item.author, style = MaterialTheme.typography.titleMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(item.item.type.name.lowercase()) })
            AssistChip(onClick = {}, label = { Text(item.item.quality.name.lowercase()) })
            bookcaseName?.let { AssistChip(onClick = {}, label = { Text(it) }) }
        }

        HorizontalDivider()

        DetailRow("ISBN", item.item.isbn)
        DetailRow("Publisher", item.item.publisher)
        DetailRow("Published", item.item.publishedYear?.toString())
        DetailRow("Language", item.item.language)
        DetailRow("Pages", item.item.pages?.toString())
        DetailRow("Notes", item.item.notes)

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Available to lend on the public catalog", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Other users can see and request this item.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = item.item.shareable, onCheckedChange = { onShareableToggle() })
        }

        HorizontalDivider()

        if (item.borrow.isBorrowed) {
            Text("Currently on loan", style = MaterialTheme.typography.titleSmall)
            item.borrow.borrowedBy?.let { Text("Borrower: $it") }
            item.borrow.borrowedAt?.let { Text("Since: ${it}") }
            Button(onClick = onBorrowToggle, modifier = Modifier.fillMaxWidth()) {
                Text("Mark as returned")
            }
        } else {
            OutlinedButton(onClick = onBorrowToggle, modifier = Modifier.fillMaxWidth()) {
                Text("Mark as borrowed")
            }
        }

        if (error != null) {
            SelectionContainer {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(end = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
