package com.majchrosoft.homelibrary.integration

import com.majchrosoft.homelibrary.di.initKoin
import com.majchrosoft.homelibrary.domain.repository.AuthRepository
import com.majchrosoft.homelibrary.domain.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LoginHamletTest : KoinTest {
    private val authRepository: AuthRepository by inject()
    private val itemRepository: ItemRepository by inject()

    @BeforeTest
    fun setUp() {
        initKoin()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testLoginAndFindHamlet() =
        runTest {
            val email = "user-31@example.test"
            val password = "Test1234!"

            val result = authRepository.signInWithEmail(email, password)
            assertTrue(result.isSuccess, "Login failed: ${result.exceptionOrNull()?.message}")

            val user = result.getOrThrow()
            println("[DEBUG_LOG] Logged in as user: ${user.id}")

            val items = itemRepository.observeMyLibrary(user.id).first()
            println("[DEBUG_LOG] Found ${items.size} items in library")

            val hamletFound = items.any { it.item.title.contains("Hamlet", ignoreCase = true) }

            if (!hamletFound) {
                println("[DEBUG_LOG] Hamlet NOT found. Available items:")
                items.forEach { println("[DEBUG_LOG] - ${it.item.title} by ${it.item.author}") }
            }

            assertTrue(hamletFound, "Hamlet not found in user's library")
        }
}
