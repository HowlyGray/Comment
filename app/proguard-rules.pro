# Add project specific ProGuard rules here.

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ==================== Data Models ====================
# Keep model classes used with Firestore serialization
-keep class com.memoryshare.app.data.model.** { *; }

# ==================== Firebase ====================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ==================== Agora SDK ====================
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# ==================== Kotlin Coroutines ====================
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ==================== Coil ====================
-dontwarn coil.**

# ==================== General ====================
# Keep enums used in when() expressions
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R class members
-keepclassmembers class **.R$* {
    public static <fields>;
}
