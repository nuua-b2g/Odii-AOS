# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android_SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes Signature
-keepattributes SetJavaScriptEnabled
-keepattributes JavascriptInterface
-keepattributes InlinedApi
-keepattributes SourceFile,LineNumberTable

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class **.*$SmarttourInterface {
    *;
}
-keepclassmembers class **.*$JavaScriptInterface {
    *;
}

-keep public class **.*$SmarttourInterface
-keep public class **.*$JavaScriptInterface

-dontnote sun.misc.Unsafe
-keep class com.google.android.gms.** { *; }
-dontwarn com.squareup.okhttp.**

#추가 'org.apache.commons
-keep class org.apache.commons.** { *; }

# 추가
-dontwarn okhttp3.**
-dontwarn okio.**
-dontnote okhttp3.**
-dontwarn com.google.android.gms.**

-keep public class com.google.android.gms.* { public *; }

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}