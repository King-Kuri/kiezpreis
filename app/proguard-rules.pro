# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class de.kiezpreis.** {
    *** Companion;
}
-keepclasseswithmembers class de.kiezpreis.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
