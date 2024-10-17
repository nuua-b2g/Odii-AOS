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

# Don't note duplicate definition (Legacy Apche Http Client)
-dontnote android.net.http.*
-dontnote org.apache.http.**


# Add when compile with JDK 1.7
-keepattributes EnclosingMethod

# 난독화 추가 적용 : 예외 처리 필요 (2021.04.12)
-keep class androidx.mediarouter.app.MediaRouteActionProvider { public <init>(...); }

# 난독화 (전체 서브클래스)
-keep class kto.smarttour.** { *; }

#-keep class com.example.Person { <fields>; } GSON 사용자 추가파일 추가

#-keep class kto.smarttour.network.response.dao.** { *; }
#-keep public class MyClass