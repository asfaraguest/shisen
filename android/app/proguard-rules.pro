# ==============================================================================
# Menatap ProGuard / R8 Optimization & Obfuscation Configuration
# ==============================================================================

# General Optimization Configuration
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod

# Menatap Application Packages
-keep class id.menatap.app.MenatapApplication { *; }
-keep class id.menatap.app.data.model.** { *; }
-keep class id.menatap.app.data.db.** { *; }
-keep class id.menatap.app.data.service.** { *; }
-keep class id.menatap.app.data.subtitle.** { *; }
-keep class id.menatap.app.util.** { *; }
-keep class id.menatap.app.ui.** { *; }

# OkHttp 4.x Configuration
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Jsoup Configuration
-keep public class org.jsoup.** { public *; }
-dontwarn org.jsoup.**

# AndroidX Media3 (ExoPlayer, HLS, OkHttp DataSource, UI, Session)
-keep class androidx.media3.exoplayer.** { *; }
-keep interface androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep interface androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep interface androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Tukaani XZ Compression
-keep class org.tukaani.xz.** { *; }
-dontwarn org.tukaani.xz.**

# Keep custom Views for XML layout inflation
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
