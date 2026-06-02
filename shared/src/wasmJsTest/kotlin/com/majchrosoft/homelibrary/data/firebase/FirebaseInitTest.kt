package com.majchrosoft.homelibrary.data.firebase

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun injectFirebaseConfig(): Unit =
    js(
        """{
    window.__FIREBASE_CONFIG__ = {
        apiKey: "AIzaSyBNhO3yOhCo9AkY5VU1aT2n4jhMwT5EuDQ",
        authDomain: "homelibrary-dev.firebaseapp.com",
        databaseURL: "https://homelibrary-dev-default-rtdb.europe-west1.firebasedatabase.app",
        projectId: "homelibrary-dev",
        storageBucket: "homelibrary-dev.firebasestorage.app",
        messagingSenderId: "85678593763",
        appId: "1:85678593763:web:9415646f9a0a3281e257d6"
    };
}""",
    )

fun loadFirebaseSdk(): Unit =
    js(
        """{
    if (!window.firebaseAuth) {
        console.log("[DEBUG_LOG] Loading Firebase SDK dynamically");
        const script = document.createElement('script');
        script.type = 'module';
        script.innerHTML = `
            import { initializeApp } from "https://www.gstatic.com/firebasejs/10.11.1/firebase-app.js";
            import { getAuth } from "https://www.gstatic.com/firebasejs/10.11.1/firebase-auth.js";
            import { getDatabase, ref, get } from "https://www.gstatic.com/firebasejs/10.11.1/firebase-database.js";
            
            async function init() {
                try {
                    console.log("[DEBUG_LOG] initializeApp start");
                    const app = initializeApp(window.__FIREBASE_CONFIG__);
                    const auth = getAuth(app);
                    const db = getDatabase(app);
                    
                    window.firebaseApp = app;
                    window.firebaseAuth = auth;
                    window.firebaseDb = db;
                    window.firebaseAuthUtils = {
                        onAuthStateChanged: (a, cb) => { console.log("onAuthStateChanged stub"); return () => {}; },
                        signInWithEmailAndPassword: (auth, email, password) => {
                            console.log("[DEBUG_LOG] signInWithEmailAndPassword stub called with:", email);
                            return Promise.reject("auth/invalid-credential");
                        }
                    };
                    
                    console.log("[DEBUG_LOG] Firebase initialized successfully in dynamic script");
                    window.__FIREBASE_READY__ = true;
                } catch (e) {
                    console.error("[DEBUG_LOG] Failed to initialize Firebase:", e);
                    window.__FIREBASE_ERROR__ = e.toString();
                }
            }
            init();
        `;
        document.head.appendChild(script);
    }
}""",
    )

fun isFirebaseReady(): Boolean = js("!!window.__FIREBASE_READY__")

fun getFirebaseError(): String? = js("window.__FIREBASE_ERROR__ || null")

fun getWindowAuth(): JsAny? = js("window.firebaseAuth")

fun isFirebaseDbPresent(): Boolean = js("!!window.firebaseDb")

class FirebaseInitTest {
    @Test
    fun testFirebaseInitialization() =
        runTest {
            println("[DEBUG_LOG] Starting FirebaseInitTest")

            injectFirebaseConfig()
            loadFirebaseSdk()

            val startTime = Clock.System.now().toEpochMilliseconds()

            // Wait for __FIREBASE_READY__
            while (!isFirebaseReady() &&
                (Clock.System.now().toEpochMilliseconds() - startTime < 15000)
            ) {
                val error = getFirebaseError()
                if (error != null) {
                    println("[DEBUG_LOG] Firebase Error detected: $error")
                    break
                }
                delay(500)
            }

            assertTrue(isFirebaseReady(), "Firebase should be ready. Error: ${getFirebaseError()}")

            // Use JS to check what's on window
            val windowAuth = getWindowAuth()
            println("[DEBUG_LOG] window.firebaseAuth: $windowAuth")
            assertNotNull(windowAuth, "window.firebaseAuth should be set")
        }

    @Test
    fun testDatabaseConnection() =
        runTest {
            println("[DEBUG_LOG] Starting testDatabaseConnection")
            injectFirebaseConfig()
            loadFirebaseSdk()

            val startTime = Clock.System.now().toEpochMilliseconds()
            while (!isFirebaseReady() && (Clock.System.now().toEpochMilliseconds() - startTime < 15000)) {
                delay(500)
            }
            assertTrue(isFirebaseReady(), "Firebase should be ready for DB test")

            val dbReady = isFirebaseDbPresent()
            assertTrue(dbReady, "Firebase DB should be initialized")
            println("[DEBUG_LOG] Firebase DB initialized successfully")
        }
}
