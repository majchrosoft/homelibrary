package com.majchrosoft.homelibrary.presentation.auth

import com.majchrosoft.homelibrary.domain.model.User

data class AuthState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AuthIntent {
    data class SignIn(val email: String, val password: String) : AuthIntent
    data class SignUp(val email: String, val password: String, val displayName: String?) : AuthIntent
    data object SignOut : AuthIntent
    data object DismissError : AuthIntent
}
