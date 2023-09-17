-keep class androidx.** {*;}
-keep class android.** {*;}
-keep class com.google.accompanist.** {*;}
-dontoptimize

# The Android pre-handler for exceptions is loaded reflectively (via ServiceLoader).
-keep class kotlinx.coroutines.experimental.android.AndroidExceptionPreHandler.** { *; }

-ignorewarnings
-dontwarn org.jetbrains.**
-dontnote org.jetbrains.**
-dontwarn okhttp3.internal.**
-dontnote okhttp3.internal.**
-dontwarn kotlinx.**
-dontnote kotlinx.**
-dontwarn kotlin.**
-dontnote kotlin.**
-dontwarn com.android.apksigner.**
-dontnote com.android.apksigner.**
-dontwarn com.android.apksigner.**
-dontnote com.android.apksigner.**