# Compose
-keep class androidx.compose.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase / GitLive
-keep class dev.gitlive.firebase.** { *; }
-keep class com.google.firebase.** { *; }

# Domain models — preserve Serializable types and their fields
-keep @kotlinx.serialization.Serializable class com.majchrosoft.homelibrary.** { *; }
