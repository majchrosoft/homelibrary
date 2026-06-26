package com.majchrosoft.homelibrary.presentation.profile

import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.MviViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
        Napier.d { "ProfileViewModel: init" }
        authRepository.currentUser
            .distinctUntilChanged()
            .onEach { user -> Napier.d("ProfileViewModel: authRepository.currentUser emitted: ${user?.id}", tag = "DEBUG app") }
            .flatMapLatest { user ->
                if (user == null) {
                    Napier.d { "ProfileViewModel: user is null, emitting empty" }
                    flowOf(Triple(null, emptyList(), emptyList()))
                } else {
                    Napier.d { "ProfileViewModel: user is ${user.id}, observing library and bookcases" }
                    combine(
                        itemRepository
                            .observeMyLibrary(user.id)
                            .onEach { Napier.d { "ProfileViewModel: itemRepository.observeMyLibrary emitted ${it.size} items" } },
                        bookcaseRepository
                            .observeMine(user.id)
                            .onEach { Napier.d { "ProfileViewModel: bookcaseRepository.observeMine emitted ${it.size} bookcases" } },
                    ) { items, bookcases -> Triple(user, items, bookcases) }
                }
            }.onEach { (user, items, bookcases) ->
                Napier.d { "ProfileViewModel: Updating state for ${user?.id} with ${items.size} items" }
                setState {
                    it.copy(
                        user = user,
                        isInitialLoading = false,
                        itemCount = items.size,
                        bookcaseCount = bookcases.size,
                        shareableCount = items.count { i -> i.item.shareable },
                    )
                }
            }.launchIn(scope)
    }

    override fun initialState() = ProfileState()

    override fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.SignOut -> scope.launch { authRepository.signOut() }
        }
    }
}
