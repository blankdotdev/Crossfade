# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/mohannad/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# GSON
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.annotations.** { *; }

# Keep our API models
-keep class com.blankdev.crossfade.api.** { *; }

# Keep our Data models
-keep class com.blankdev.crossfade.data.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Retrofit
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
