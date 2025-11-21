# Add project specific ProGuard rules here.

# ============================================================================
# Room Database Rules
# ============================================================================
# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * {
    *;
}
-keep @androidx.room.Dao class * {
    *;
}

# Keep Room entity fields and constructors
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}

# Keep Room DAO methods
-keepclassmembers @androidx.room.Dao interface * {
    *;
}

# Don't warn about Room paging
-dontwarn androidx.room.paging.**

# Keep specific entity classes
-keep class com.example.shortblocker.data.SettingsEntity { *; }
-keep class com.example.shortblocker.data.BlockLogEntity { *; }
-keep class com.example.shortblocker.data.AppDatabase { *; }
-keep class com.example.shortblocker.data.SettingsDao { *; }
-keep class com.example.shortblocker.data.BlockLogDao { *; }

# ============================================================================
# Kotlin Rules
# ============================================================================
# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Kotlin reflection
-keep class kotlin.reflect.** { *; }
-keep interface kotlin.reflect.** { *; }

# Keep data classes (used throughout the app)
-keep class com.example.shortblocker.data.AppContext { *; }
-keep class com.example.shortblocker.data.DetectionResult { *; }
-keep class com.example.shortblocker.data.AppSettings { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# Accessibility Service Rules
# ============================================================================
# Keep AccessibilityService and related classes
-keep class com.example.shortblocker.service.BlockerAccessibilityService { *; }
-keep class * extends android.accessibilityservice.AccessibilityService {
    *;
}

# ============================================================================
# Dependency Injection (Koin) Rules
# ============================================================================
# Keep Koin modules
-keep class org.koin.** { *; }
-keep class com.example.shortblocker.di.** { *; }
-keep class com.example.shortblocker.**Module { *; }
-keep class com.example.shortblocker.**ModuleKt { *; }

# ============================================================================
# Android Components Rules
# ============================================================================
# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep activities
-keep class * extends android.app.Activity
-keep class * extends androidx.appcompat.app.AppCompatActivity

# Keep services
-keep class * extends android.app.Service
-keep class * extends android.app.IntentService

# ============================================================================
# Serialization Rules
# ============================================================================
# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# General Android Rules
# ============================================================================
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep setters and getters for View fields
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# Keep onClick methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ============================================================================
# Obfuscation Rules
# ============================================================================
# Rename packages to reduce APK size
-repackageclasses 'com.example.shortblocker'

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ============================================================================
# Warnings to Ignore
# ============================================================================
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
-dontwarn kotlin.reflect.jvm.internal.**
