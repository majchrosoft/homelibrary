package com.majchrosoft.homelibrary.integration

import com.majchrosoft.homelibrary.domain.model.Bookcase
import com.majchrosoft.homelibrary.domain.model.Item
import com.majchrosoft.homelibrary.domain.model.ItemDetails
import com.majchrosoft.homelibrary.domain.model.User
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.BookcaseRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import com.majchrosoft.homelibrary.presentation.library.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MockHamletTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class MockAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<User?>(null)
        override val currentUser: Flow<User?> = _currentUser

        override suspend fun getBearerToken(): String? = "mock-token"

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<User> {
            if (email == "user-31@example.test" && password == "Test1234!") {
                val user = User(id = "user-31", email = email)
                _currentUser.value = user
                return Result.success(user)
            }
            return Result.failure(Exception("Invalid credentials"))
        }

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
            displayName: String?,
        ): Result<User> = TODO()

        override suspend fun sendPasswordReset(email: String): Result<Unit> = TODO()

        override suspend fun signOut() {
            _currentUser.value = null
        }
    }

    class MockItemRepository(
        private val items: List<Item>,
    ) : ItemRepository {
        override fun observeMyLibrary(ownerId: String): Flow<List<Item>> = flowOf(items.filter { it.ownerId == ownerId })

        override fun observeSharedCatalog(
            query: String?,
            limit: Int,
        ): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun getById(
            ownerId: String,
            itemId: String,
        ): Item? = null

        override suspend fun add(item: Item): Result<Item> = TODO()

        override suspend fun update(item: Item): Result<Unit> = TODO()

        override suspend fun delete(
            ownerId: String,
            itemId: String,
        ): Result<Unit> = TODO()
    }

    class MockBookcaseRepository : BookcaseRepository {
        override fun observeMine(ownerId: String): Flow<List<Bookcase>> = flowOf(emptyList())

        override suspend fun getById(
            ownerId: String,
            bookcaseId: String,
        ): Bookcase? = null

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
    fun testLoginAndFindHamletWithMocks() =
        runTest {
            val hamlet =
                Item(
                    id = "item-1",
                    ownerId = "user-31",
                    item = ItemDetails(title = "Hamlet", author = "William Shakespeare"),
                )

            val authRepo = MockAuthRepository()
            val itemRepo = MockItemRepository(listOf(hamlet))
            val bookcaseRepo = MockBookcaseRepository()

            val viewModel = LibraryViewModel(itemRepo, bookcaseRepo, authRepo)

            // 1. Sign in
            authRepo.signInWithEmail("user-31@example.test", "Test1234!")

            // 2. Wait for state update and check items
            val state = viewModel.state.first { it.items.isNotEmpty() }

            val hamletFound = state.items.any { it.item.title == "Hamlet" }
            assertTrue(hamletFound, "Hamlet should be found in the library")
        }
}
