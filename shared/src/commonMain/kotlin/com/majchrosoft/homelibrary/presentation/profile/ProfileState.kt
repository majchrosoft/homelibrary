package com.majchrosoft.homelibrary.presentation.profile

import com.majchrosoft.homelibrary.domain.model.User

data class ProfileState(
    val user: User? = null,
    val itemCount: Int = 0,
    val bookcaseCount: Int = 0,
    val shareableCount: Int = 0,
)

sealed interface ProfileIntent {
    data object SignOut : ProfileIntent
}
