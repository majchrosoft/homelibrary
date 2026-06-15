package com.majchrosoft.homelibrary.ui.bookcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majchrosoft.homelibrary.presentation.bookcase.BookcaseEditIntent
import com.majchrosoft.homelibrary.presentation.bookcase.BookcaseEditViewModel
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookcaseEditScreen(bookcaseId: String?) {
    val viewModel = koinInject<BookcaseEditViewModel> { parametersOf(bookcaseId) }
    val navigator = koinInject<Navigator>()
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val hasNavigated = remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved && !hasNavigated.value) {
            hasNavigated.value = true
            navigator.back()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clear()
        }
    }

    // If navigation has already happened, don't render anything
    if (hasNavigated.value) {
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (bookcaseId == null) "Add bookcase" else "Edit bookcase") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.dispatch(BookcaseEditIntent.NameChanged(it)) },
                label = { Text("Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.location,
                onValueChange = { viewModel.dispatch(BookcaseEditIntent.LocationChanged(it)) },
                label = { Text("Location (e.g. Living room)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.dispatch(BookcaseEditIntent.DescriptionChanged(it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            state.errorMessage?.let { errorMessage ->
                SelectionContainer {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            Button(
                onClick = { viewModel.dispatch(BookcaseEditIntent.Save) },
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(if (bookcaseId == null) "Create bookcase" else "Save changes")
            }
        }
    }
}
