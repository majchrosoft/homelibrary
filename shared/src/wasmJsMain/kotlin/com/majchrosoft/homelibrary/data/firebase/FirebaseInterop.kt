package com.majchrosoft.homelibrary.data.firebase

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

@JsName("window.firebaseAuth")
external val firebaseAuth: FirebaseAuthJs

@JsName("window.firebaseAuthUtils")
external val firebaseAuthUtils: FirebaseAuthUtilsJs

external interface FirebaseAuthJs : JsAny {
    val currentUser: FirebaseUserJs?
}

external interface FirebaseAuthUtilsJs {
    fun onAuthStateChanged(
        auth: FirebaseAuthJs,
        callback: (FirebaseUserJs?) -> Unit,
    ): () -> Unit

    fun signInWithEmailAndPassword(
        auth: FirebaseAuthJs,
        email: String,
        password: String,
    ): Promise<UserCredentialJs>

    fun createUserWithEmailAndPassword(
        auth: FirebaseAuthJs,
        email: String,
        password: String,
    ): Promise<UserCredentialJs>

    fun sendPasswordResetEmail(
        auth: FirebaseAuthJs,
        email: String,
    ): Promise<JsAny?>

    fun signOut(auth: FirebaseAuthJs): Promise<JsAny?>

    fun updateProfile(
        user: FirebaseUserJs,
        profile: JsAny,
    ): Promise<JsAny?>

    fun getIdToken(user: FirebaseUserJs): Promise<JsString>
}

external interface UserCredentialJs : JsAny {
    val user: FirebaseUserJs?
}

external interface FirebaseUserJs : JsAny {
    val uid: String
    val email: String?
    val displayName: String?
    val photoURL: String?
}

// Realtime Database
external interface FirebaseDbJs : JsAny

external interface FirebaseDbUtilsJs : JsAny {
    fun ref(
        db: FirebaseDbJs,
        path: String?,
    ): FirebaseDataReferenceJs

    fun onValue(
        query: FirebaseQueryJs,
        callback: (FirebaseDataSnapshotJs) -> Unit,
    ): () -> Unit

    fun push(ref: FirebaseDataReferenceJs): FirebaseDataReferenceJs

    fun set(
        ref: FirebaseDataReferenceJs,
        value: JsAny?,
    ): Promise<JsAny?>

    fun update(
        ref: FirebaseDataReferenceJs,
        value: JsAny?,
    ): Promise<JsAny?>

    fun remove(ref: FirebaseDataReferenceJs): Promise<JsAny?>

    fun query(
        ref: FirebaseDataReferenceJs,
        vararg constraints: FirebaseQueryConstraintJs,
    ): FirebaseQueryJs

    fun orderByChild(path: String): FirebaseQueryConstraintJs

    fun limitToFirst(limit: Int): FirebaseQueryConstraintJs
}

external interface FirebaseQueryJs : JsAny

external interface FirebaseDataReferenceJs : FirebaseQueryJs {
    val key: String?
}

external interface FirebaseQueryConstraintJs : JsAny

external interface FirebaseDataSnapshotJs : JsAny {
    val key: String?
    val ref: FirebaseDataReferenceJs

    fun exists(): Boolean

    @JsName("val")
    fun getValue(): JsAny?
}

fun getFirebaseAuth(): FirebaseAuthJs? = js("window.firebaseAuth")

fun getFirebaseAuthUtils(): FirebaseAuthUtilsJs? = js("window.firebaseAuthUtils")

fun getFirebaseDb(): FirebaseDbJs? = js("window.firebaseDb")

fun getFirebaseDbUtils(): FirebaseDbUtilsJs? = js("window.firebaseDbUtils")

// ─── Snapshot interop helpers (wasm-compatible) ──
// Kotlin/Wasm forbids js.Array<T>, .length, and inline js() in
// nested scopes. All JS interaction must live in top-level @JsFun
// definitions or top-level property initializers.

typealias OnEntryHandler = (key: String, entryValueJs: JsAny?) -> Boolean

/**
 * Calls [handler] for each *own* enumerable property key in [snapshot].
 * Implemented entirely on the JS side so [js()] lives only at the top level.
 */
@JsFun("""(snapshot, handler) -> {
    var result = true;
    Object.keys(snapshot).forEach(function(key) {
        if (!handler.call(undefined, key, snapshot[key])) result = false;
    });
    return result;
}""")
external fun forEachOwnEntry(
    snapshot: FirebaseDataSnapshotJs,
    handler: OnEntryHandler,
): Boolean

/**
 * Returns true if [snapshot] has at least one own enumerable key.
 */
@JsFun("(snapshot) -> Object.keys(snapshot).length > 0")
external fun snapshotHasOwnKeys(snapshot: FirebaseDataSnapshotJs): Boolean

/**
 * Resolve a [FirebaseDataSnapshotJs] to a Kotlin map of (key, jsonString).
 * Value is JSON‑stringified on the JS side and returned as a Kotlin List of
 * [Pair] — no js.Array exposure on the Kotlin side.
 */
data class SnapshotEntry(val key: String, val jsonValue: String?)

@JsFun("""(snapshot) -> {
    var keys = Object.keys(snapshot);
    var entries = [];
    for (var i = 0; i < keys.length; i++) {
        var k = keys[i];
        var v = snapshot[k];
        entries.push([k, JSON.stringify(v)]);
    }
    return entries;
}""")
external fun snapshotToEntries(snapshot: FirebaseDataSnapshotJs): List<SnapshotEntry>

/** Snapshot helper for WasmAuthRepository: displayName setter */
fun setDisplayNameJs(
    obj: JsAny,
    name: String,
) {
    js("obj['displayName'] = name")
}

// ─── JSON helpers ──────────────────────────────────────────────────────

@JsFun("(x) => JSON.stringify(x)")
external fun stringify(x: JsAny?): String

@JsFun("(x) => JSON.parse(x)")
external fun parse(x: String): JsAny?

object JSON {
    fun stringify(x: JsAny?): String =
        com.majchrosoft.homelibrary.data.firebase
            .stringify(x)

    fun parse(x: String): JsAny? =
        com.majchrosoft.homelibrary.data.firebase
            .parse(x)
}

// ─── Snapshot helpers (wasm-compatible) ──────────────────
// Kotlin/Wasm doesn't allow js.Array<T> in external function
// signatures. Instead we use mutable lists as output collectors.

typealias SnapshotEntryHandler = (key: String, valueJs: JsAny) -> Unit

/**
 * Calls [handler] for each own enumerable property key in [snapshot].
 * Implemented on the JS side so [js()] is only used at top level.
 */
external fun forEachSnapshotKeys(
    snapshot: FirebaseDataSnapshotJs,
    handler: SnapshotEntryHandler,
)

/**
 * Returns true if [snapshot] has at least one own enumerable property.
 */
external fun snapshotHasOwnKeys(snapshot: FirebaseDataSnapshotJs): Boolean

/**
 * Set `displayName` on a plain JS object.
 */
@JsName("displayName")
external var JsAny.displayName: JsString?

/**
 * Create a plain JS object from key-value pairs.
 */
@JsName("")
external fun createJsPlainObject(): JsAny

/**
 * Set a property by name on a plain JS object.
 */
external fun setJsProp(
    obj: JsAny,
    name: String,
    value: Any?,
)

suspend fun <T : JsAny?> Promise<T>.await(): T =
    suspendCoroutine { continuation ->
        then({
            continuation.resume(it)
            null
        }, {
            continuation.resumeWith(Result.failure(Exception(it.toString())
            null
        })
    }

fun String.toJsString(): JsString = this.toJsString()
fun JsString.toKotlinString(): String = this.toString()
