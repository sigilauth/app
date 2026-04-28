# Sigil Auth ProGuard Rules

# Keep cryptographic providers
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep Android Keystore classes
-keep class android.security.keystore.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Retrofit interfaces
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Room entities
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase

# Keep data classes used in JSON serialization
-keep class com.sigilauth.app.network.models.** { *; }
-keep class com.sigilauth.app.data.models.** { *; }

# Keep biometric classes
-keep class androidx.biometric.** { *; }

# AGPL-3.0 license notice preservation
-keepattributes SourceFile,LineNumberTable
