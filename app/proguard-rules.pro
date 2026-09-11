# AGENTS.md Build Hardening (release): strip every android.util.Log call so no
# PII or tokens leak through logcat from the production binary.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Room — generated *_Impl classes and schema bindings must stay reachable
-keep class * extends androidx.room.RoomDatabase

# Hilt — entry points and @HiltViewModel factories resolved at runtime
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep class dagger.hilt.** { *; }

# SQLCipher — JNI-bound classes loaded by name by the native library must not
# be renamed or stripped, otherwise encrypted DB open fails at runtime
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**