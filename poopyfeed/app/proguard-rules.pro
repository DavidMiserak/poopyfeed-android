# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Retrofit and OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
  @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx serialization (@Serializable models used by Retrofit converter)
# kotlinx-serialization-core bundles rules in META-INF/proguard/kotlinx-serialization.pro,
# but we add explicit project-level rules to protect model serializers under R8 full mode.
-dontwarn kotlinx.serialization.**

# Keep generated $$serializer objects for all model classes (includedescriptorclasses
# also keeps referenced type descriptor classes, required for generic types like PaginatedResponse<T>)
-keep,includedescriptorclasses class net.poopyfeed.pf.data.models.**$$serializer { *; }

# Modern conditional approach: keep Companion and serializer() only on @Serializable classes
-if @kotlinx.serialization.Serializable class net.poopyfeed.pf.data.models.**
-keepclassmembers class net.poopyfeed.pf.data.models.<1> {
    static net.poopyfeed.pf.data.models.<1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
