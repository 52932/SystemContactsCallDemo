# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# todo 混淆设置
-keep public class com.example.** {*;}


-keepattributes Exceptions,InnerClasses

-keepattributes Signature

# im Lib
-keep class io.rong.** {*;}
-keep class cn.rongcloud.** {*;}
-keep class * implements io.rong.imlib.model.MessageContent {*;}
-dontwarn io.rong.push.**
-dontnote com.xiaomi.**
-dontnote com.google.android.gms.gcm.**
-dontnote io.rong.**

#rtc lib
-keep public class cn.rongcloud.rtc.** {*;}
-keep public class cn.rongcloud.rtclib.** {*;}
#
-keep public class cn.rongcloud.calllib.** {*;}
-keep public class cn.rongcloud.callplus.** {*;}

-ignorewarnings

