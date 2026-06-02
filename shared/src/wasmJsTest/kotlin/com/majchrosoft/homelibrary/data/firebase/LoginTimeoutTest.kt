package com.majchrosoft.homelibrary.data.firebase

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginTimeoutTest {
    @Test
    fun testLoginTimeoutsWhenNoFirebase() =
        runTest {
            println("[DEBUG_LOG] Starting testLoginTimeoutsWhenNoFirebase")

            // Ensure no Firebase from previous tests
            js("window.firebaseAuth = null; window.firebaseAuthUtils = null; window.__FIREBASE_READY__ = false;")

            val repository = WasmAuthRepository()

            println("[DEBUG_LOG] Calling signInWithEmail (should timeout in 5s for test)")
            // Temporarily override timeout via reflection or just accept that it takes 15s
            val result = repository.signInWithEmail("dummy@example.com", "password")

            println("[DEBUG_LOG] Result: $result")

            assertTrue(result.isFailure, "Result should be failure due to timeout")
            val message = result.exceptionOrNull()?.message ?: ""
            println("[DEBUG_LOG] Failure message: $message")
            assertTrue(
                message.contains("Firebase Auth not initialized after"),
                "Expected timeout error message, got: $message",
            )
        }

    @Test
    fun testLoginDoesNotTimeoutWhenFirebaseReady() =
        runTest {
            println("[DEBUG_LOG] Starting testLoginDoesNotTimeoutWhenFirebaseReady")

            injectFirebaseConfig()
            loadFirebaseSdk()

            // Wait for __FIREBASE_READY__
            val startTime = Clock.System.now().toEpochMilliseconds()
            while (!isFirebaseReady() && (Clock.System.now().toEpochMilliseconds() - startTime < 15000)) {
                delay(500)
            }
            assertTrue(isFirebaseReady(), "Firebase should be ready")

            val repository = WasmAuthRepository()
            val result = repository.signInWithEmail("dummy@example.com", "wrongpassword")

            println("[DEBUG_LOG] Result: $result")

            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message ?: ""
                assertFalse(
                    message.contains("not initialized after timeout"),
                    "Should NOT fail with initialization timeout. Got: $message",
                )
            }
        }
}
