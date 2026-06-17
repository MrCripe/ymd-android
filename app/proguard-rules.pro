# Keep kotlinx serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.mrcriper.ymd.**$$serializer { *; }
-keepclassmembers class com.mrcriper.ymd.** {
    *** Companion;
}
-keepclasseswithmembers class com.mrcriper.ymd.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$* { *; }

# Room
-keep class androidx.room.** { *; }

# Ktor
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# jaudiotagger / vorbis
-dontwarn org.gagravarr.**
-dontwarn net.jthink.**
