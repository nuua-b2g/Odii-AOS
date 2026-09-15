# Begin: Proguard rules for Firebase

# Authentication
-keepattributes *Annotation*

-keepattributes Signature

-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.w3c.dom.**