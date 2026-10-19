# ===================================================================
# FULL OBFUSCATION & REPACKAGING CONFIGURATION (ALAS RETRO MUSIC)
# ===================================================================
# Custom Unicode Obfuscation Dictionary
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Repackage semua class yang diobfuscate ke direktori root
-repackageclasses
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute ""
-optimizationpasses 5

# Strip logging calls and string leakage in production DEX
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int println(...);
}

# Strip Kotlin compiler null parameter validation string constants
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
}

# Preserve stack traces line numbers
-keepattributes SourceFile,LineNumberTable

# Entry points AndroidManifest yang dipanggil langsung oleh OS:
-keep public class com.example.MainActivity
-keep public class com.example.SRMusicApp
-keep public class com.example.core.playback.MusicService

# Media3 Session Service (A)
# Mengandalkan consumer rules bawaan Media3 AAR. Cukup keep Session Service & Session.
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.session.MediaSession { *; }
-dontwarn androidx.media3.**

# Jellyfin Media3 Native Decoder (B)
# Persempit keep hanya pada package FFmpeg renderer dan JNI bindings yang dipakai.
-keep class org.jellyfin.media3.ffmpeg.** { *; }
-dontwarn org.jellyfin.media3.**

# Keep JNI Methods
-keepclasseswithmembernames class * {
    native <methods>;
}



# TagLib JNI bindings
-keep class com.kyant.taglib.** { *; }
-dontwarn com.kyant.taglib.**

# Keep Compose view essentials
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }

# Room Database & Entities (D)
# Room men-generate implementasi langsung saat compile-time via KSP.
-keep class com.example.core.data.database.AppDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}
-dontwarn androidx.room.paging.**

# Keep enums and entries for Kotlin 1.9+ and Android (E)
# Preserve enum reflection methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** getEntries();
    public static **[] $VALUES;
}

# Pertahankan enum yang dipersistensikan ke SharedPreferences atau Settings
-keep enum com.example.core.model.NowPlayingProgressStyle { *; }
-keep enum com.example.core.model.NowPlayingTheme { *; }
-keep enum com.example.core.model.TrackInfoAlignment { *; }
-keep enum com.example.core.model.AppThemeMode { *; }
-keep enum com.example.core.model.AppSortCriteria { *; }
-keep enum com.example.core.model.AppFolderStyle { *; }
-keep enum com.example.core.model.AppGridMode { *; }
-keep enum com.example.ui.common.theme.ThemeMode { *; }
-keep enum com.example.ui.navigation.components.TabItem { *; }

# Parcelable CREATORs (F)
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
