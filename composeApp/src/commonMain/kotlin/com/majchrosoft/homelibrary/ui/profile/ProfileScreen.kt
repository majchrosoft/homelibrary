package com.majchrosoft.homelibrary.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majchrosoft.homelibrary.presentation.navigation.Navigator
import com.majchrosoft.homelibrary.presentation.profile.ProfileIntent
import com.majchrosoft.homelibrary.presentation.profile.ProfileViewModel
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    Napier.d { "ProfileScreen: Composition" }
    val viewModel = koinInject<ProfileViewModel>()
    val navigator = koinInject<Navigator>()
    val state by viewModel.state.collectAsState()
    
    Napier.d { "ProfileScreen: state.isInitialLoading=${state.isInitialLoading}, user=${state.user?.id}" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isInitialLoading) {
                Text("Loading...", style = MaterialTheme.typography.headlineSmall)
            } else {
                Text(
                    state.user?.displayName?.takeIf { it.isNotBlank() }
                        ?: state.user?.email?.ifBlank { "—" }
                        ?: "Signed out",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            state.user?.email?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your library", style = MaterialTheme.typography.titleMedium)
                    StatRow("Items", state.itemCount.toString())
                    StatRow("Bookcases", state.bookcaseCount.toString())
                    StatRow("Shared on catalog", state.shareableCount.toString())
                }
            }

            OutlinedButton(
                onClick = { viewModel.dispatch(ProfileIntent.SignOut) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out") }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
