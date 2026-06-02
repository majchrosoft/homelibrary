package com.majchrosoft.homelibrary.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.presentation.library.LibraryIntent
import com.majchrosoft.homelibrary.presentation.library.LibraryViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.navigation.Screen
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    Napier.d { "LibraryScreen: Composition" }
    val viewModel = koinInject<LibraryViewModel>()
    val navigator = koinInject<Navigator>()
    val state by viewModel.state.collectAsState()
    
    Napier.d { "LibraryScreen: state.isLoading=${state.isLoading}, items=${state.items.size}" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    IconButton(onClick = { navigator.push(Screen.SharedCatalog) }) {
                        Icon(Icons.Default.Public, contentDescription = "Shared catalog")
                    }
                    IconButton(onClick = { navigator.push(Screen.Bookcases) }) {
                        Icon(Icons.Default.ViewModule, contentDescription = "Bookcases")
                    }
                    IconButton(onClick = { navigator.push(Screen.Profile) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navigator.push(Screen.ItemEdit(itemId = null)) }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.dispatch(LibraryIntent.QueryChanged(it)) },
                label = { Text("Search title, author, ISBN") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
            )

            if (state.bookcases.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedBookcaseId == null,
                            onClick = { viewModel.dispatch(LibraryIntent.BookcaseSelected(null)) },
                            label = { Text("All") },
                        )
                    }
                    items(state.bookcases, key = { it.id }) { bookcase ->
                        FilterChip(
                            selected = state.selectedBookcaseId == bookcase.id,
                            onClick = { viewModel.dispatch(LibraryIntent.BookcaseSelected(bookcase.id)) },
                            label = { Text(bookcase.name.ifBlank { "Untitled" }) },
                        )
                    }
                }
            }

            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.errorMessage != null -> SelectionContainer {
                    Text(
                        text = "Error: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                state.filtered.isEmpty() -> Text(
                    "No items yet — tap + to add one.",
                    modifier = Modifier.padding(16.dp),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.filtered, key = { it.id }) { item ->
                        ItemRow(item, onClick = { navigator.push(Screen.ItemDetail(item.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: Item, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.item.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
            if (item.item.author.isNotBlank()) {
                Text(item.item.author, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Type: ${item.item.type.name.lowercase()}  ·  Quality: ${item.item.quality.name.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (item.borrow.isBorrowed) {
                Text(
                    "On loan${item.borrow.borrowedBy?.let { " — $it" } ?: ""}",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
