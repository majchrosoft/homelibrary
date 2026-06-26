package com.majchrosoft.homelibrary.data.firebase

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

// Firebase Auth
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

    fun get(query: FirebaseQueryJs): Promise<FirebaseDataSnapshotJs>

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

// Snapshot iteration using Firebase's forEach() API
typealias ChildHandler = (key: String, value: JsAny?) -> Unit

@JsName("forEachChild")
@JsFun(
    """(snapshot, handler) => {
    if (snapshot.forEach) {
        snapshot.forEach(function(childSnapshot) {
            handler(childSnapshot.key, childSnapshot.val());
        });
    }
}""",
)
external fun forEachChild(
    snapshot: FirebaseDataSnapshotJs,
    handler: ChildHandler,
): Unit

fun snapshotToMap(snapshot: FirebaseDataSnapshotJs): Map<String, JsAny?> {
    val map = mutableMapOf<String, JsAny?>()
    forEachChild(snapshot) { key, value ->
        map[key] = value
    }
    return map
}

// JSON helpers
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

// Promise helpers
@JsFun(
    """(self, onSuccess, onFailure) => {
    self.then(
        function(value) {
            var result = (value === undefined) ? null : value;
            onSuccess(result);
        },
        onFailure
    );
    return null;
}""",
)
external fun <T : JsAny?> thenPromiseVoid(
    promise: Promise<T>,
    onSuccess: (T) -> Unit,
    onFailure: (JsAny) -> Unit,
)

suspend fun <T : JsAny?> Promise<T>.await(): T? =
    suspendCoroutine { cont ->
        thenPromiseVoid(
            this,
            { s: T -> cont.resume(s) },
            { f: JsAny -> cont.resumeWith(Result.failure(Exception(f.toString()))) },
        )
    }

suspend fun <T : JsAny> Promise<T>.awaitNonnull(): T =
    suspendCoroutine { cont ->
        thenPromiseVoid(
            this,
            { s: T -> cont.resume(s) },
            { f: JsAny -> cont.resumeWith(Result.failure(Exception(f.toString()))) },
        )
    }

// JS object helpers
@JsFun("""(obj, name, value) => { obj[name] = value; return obj; }""")
external fun setJsPropRaw(
    obj: JsAny,
    name: String,
    value: JsAny?,
): JsAny

fun setJsProp(
    obj: JsAny,
    name: String,
    value: Any?,
) {
    @Suppress("UNCHECKED_CAST")
    val jsValue = value as? JsAny?
    setJsPropRaw(obj, name, jsValue)
}

fun String.toJsString(): JsString = this.toJsString()

fun JsString.toKotlinString(): String = this.toString()
