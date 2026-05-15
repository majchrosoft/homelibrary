package com.majchrosoft.homelibrary.presentation.bookcase

data class BookcaseEditState(
    val editingBookcaseId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,

    val name: String = "",
    val description: String = "",
    val location: String = "",
) {
    val isValid: Boolean get() = name.isNotBlank()
}

sealed interface BookcaseEditIntent {
    data class NameChanged(val value: String) : BookcaseEditIntent
    data class DescriptionChanged(val value: String) : BookcaseEditIntent
    data class LocationChanged(val value: String) : BookcaseEditIntent
    data object Save : BookcaseEditIntent
    data object DismissError : BookcaseEditIntent
}
