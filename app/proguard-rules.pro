# QBox proguard rules

# Gson 反射序列化（模型类）
-keep class com.qbox.tv.model.** { <fields>; }
-keep class com.qbox.tv.config.** { <fields>; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 运行时 jar 加载：DexClassLoader 内的代码不受此 R8 影响，
# 但 jar 会反射调用父加载器里的 okhttp/gson/sardine，保持这些库不被裁剪
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class com.thegrizzlylabs.sardineandroid.** { *; }
-dontwarn com.thegrizzlylabs.sardineandroid.**

# media3
-dontwarn androidx.media3.**
