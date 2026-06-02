package com.majchrosoft.homelibrary.presentation.library

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.model.User
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelLoadingTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class MockAuthRepository(
        user: User? = null,
    ) : AuthRepository {
        override val currentUser: Flow<User?> = MutableStateFlow(user)

        override suspend fun getBearerToken(): String? = null

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<User> = TODO()

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
            displayName: String?,
        ): Result<User> = TODO()

        override suspend fun sendPasswordReset(email: String): Result<Unit> = TODO()

        override suspend fun signOut() = TODO()
    }

    class MockItemRepository(
        private val itemsFlow: Flow<List<Item>>,
    ) : ItemRepository {
        override fun observeMyLibrary(ownerId: String): Flow<List<Item>> = itemsFlow

        override fun observeSharedCatalog(
            query: String?,
            limit: Int,
        ): Flow<List<Item>> = TODO()

        override suspend fun getById(
            ownerId: String,
            itemId: String,
        ): Item? = TODO()

        override suspend fun add(item: Item): Result<Item> = TODO()

        override suspend fun update(item: Item): Result<Unit> = TODO()

        override suspend fun delete(
            ownerId: String,
            itemId: String,
        ): Result<Unit> = TODO()
    }

    class MockBookcaseRepository(
        private val bookcasesFlow: Flow<List<Bookcase>>,
    ) : BookcaseRepository {
        override fun observeMine(ownerId: String): Flow<List<Bookcase>> = bookcasesFlow

        override suspend fun getById(
            ownerId: String,
            bookcaseId: String,
        ): Bookcase? = TODO()

        override suspend fun add(
            ownerId: String,
            bookcase: Bookcase,
        ): Result<Bookcase> = TODO()

        override suspend fun update(
            ownerId: String,
            bookcase: Bookcase,
        ): Result<Unit> = TODO()

        override suspend fun delete(
            ownerId: String,
            bookcaseId: String,
        ): Result<Unit> = TODO()
    }

    @Test
    fun `isLoading becomes false if repositories start with empty emission`() =
        runTest {
            val user = User(id = "user-1", email = "test@example.com")
            val authRepo = MockAuthRepository(user)

            // Repositories that never emit anything, but the ViewModel now uses onStart
            val itemRepo = MockItemRepository(emptyFlow())
            val bookcaseRepo = MockBookcaseRepository(emptyFlow())

            val viewModel = LibraryViewModel(itemRepo, bookcaseRepo, authRepo)

            assertFalse(viewModel.state.value.isLoading, "Should not be loading because onStart emits initial value")
        }

    @Test
    fun `isLoading becomes false when both repositories emit`() =
        runTest {
            val user = User(id = "user-1", email = "test@example.com")
            val authRepo = MockAuthRepository(user)

            val itemRepo = MockItemRepository(MutableStateFlow(emptyList()))
            val bookcaseRepo = MockBookcaseRepository(MutableStateFlow(emptyList()))

            val viewModel = LibraryViewModel(itemRepo, bookcaseRepo, authRepo)

            assertFalse(viewModel.state.value.isLoading, "Should not be loading after emissions")
        }
}
