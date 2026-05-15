package com.majchrosoft.homelibrary.presentation.profile

import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val itemRepository: ItemRepository,
    private val bookcaseRepository: BookcaseRepository,
) : MviViewModel<ProfileState, ProfileIntent>() {

    init {
        authRepository.currentUser
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(Triple(null, emptyList(), emptyList()))
                } else {
                    combine(
                        itemRepository.observeMyLibrary(user.id),
                        bookcaseRepository.observeMine(user.id),
                    ) { items, bookcases -> Triple(user, items, bookcases) }
                }
            }
            .onEach { (user, items, bookcases) ->
                setState {
                    it.copy(
                        user = user,
                        itemCount = items.size,
                        bookcaseCount = bookcases.size,
                        shareableCount = items.count { i -> i.item.shareable },
                    )
                }
            }
            .launchIn(scope)
    }

    override fun initialState() = ProfileState()

    override fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.SignOut -> scope.launch { authRepository.signOut() }
        }
    }
}
