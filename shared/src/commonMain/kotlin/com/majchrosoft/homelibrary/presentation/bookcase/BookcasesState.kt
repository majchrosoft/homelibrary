package com.majchrosoft.homelibrary.presentation.bookcase

import com.majchrosoft.homelibrary.domain.model.Bookcase

data class BookcasesState(
    val isLoading: Boolean = true,
    val bookcases: List<Bookcase> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface BookcasesIntent {
    data class Delete(val bookcaseId: String) : BookcasesIntent
    data object DismissError : BookcasesIntent
}
