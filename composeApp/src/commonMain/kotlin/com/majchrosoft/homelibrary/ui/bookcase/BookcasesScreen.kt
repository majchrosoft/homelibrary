package com.majchrosoft.homelibrary.ui.bookcase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.presentation.bookcase.BookcasesIntent
import com.majchrosoft.homelibrary.presentation.bookcase.BookcasesViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.navigation.Screen
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookcasesScreen() {
    val viewModel = koinInject<BookcasesViewModel>()
    val navigator = koinInject<Navigator>()
    val state by viewModel.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<Bookcase?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookcases") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navigator.push(Screen.BookcaseEdit(bookcaseId = null)) }) {
                Icon(Icons.Default.Add, contentDescription = "Add bookcase")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> SelectionContainer {
                Text(
                    text = "Error: ${state.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(padding).padding(16.dp),
                )
            }

            state.bookcases.isEmpty() -> Text(
                "No bookcases yet — tap + to add a shelf, box, or room.",
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.bookcases, key = { it.id }) { bookcase ->
                    BookcaseCard(
                        bookcase,
                        onClick = { navigator.push(Screen.BookcaseEdit(bookcaseId = bookcase.id)) },
                        onDelete = { pendingDelete = bookcase },
                    )
                }
            }
        }
    }

    pendingDelete?.let { bookcase ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dispatch(BookcasesIntent.Delete(bookcase.id))
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            title = { Text("Delete ${bookcase.name.ifBlank { "this bookcase" }}?") },
            text = { Text("Items in this bookcase will keep their reference but show as unassigned.") },
        )
    }
}

@Composable
private fun BookcaseCard(bookcase: Bookcase, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bookcase.name.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                bookcase.location?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                bookcase.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete bookcase")
            }
        }
    }
}
