package com.majchrosoft.homelibrary.presentation.auth

import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : MviViewModel<AuthState, AuthIntent>() {
    init {
        authRepository.currentUser
            .onEach { user ->
                setState { it.copy(user = user) }
            }
            .launchIn(scope)
    }

    override fun initialState() = AuthState()

    override fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.SignIn -> signIn(intent.email, intent.password)
            is AuthIntent.SignUp -> signUp(intent.email, intent.password, intent.displayName)
            AuthIntent.SignOut -> signOut()
            AuthIntent.DismissError -> setState { it.copy(errorMessage = null) }
        }
    }

    private fun signIn(
        email: String,
        password: String,
    ) {
        scope.launch {
            setState { it.copy(isLoading = true, errorMessage = null) }
            authRepository
                .signInWithEmail(email, password)
                .onSuccess { setState { it.copy(isLoading = false) } }
                .onFailure { e -> setState { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    private fun signUp(
        email: String,
        password: String,
        displayName: String?,
    ) {
        scope.launch {
            setState { it.copy(isLoading = true, errorMessage = null) }
            authRepository
                .signUpWithEmail(email, password, displayName)
                .onSuccess { setState { it.copy(isLoading = false) } }
                .onFailure { e -> setState { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    private fun signOut() {
        scope.launch { authRepository.signOut() }
    }
}
