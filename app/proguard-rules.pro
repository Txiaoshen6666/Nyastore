# Keep kotlinx-serialization JSON mapping for GitHub models
-keepclassmembers class com.example.githubappstore.data.model.** {
    *;
}
# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
# kotlinx-serialization
-keepattributes *Annotation*
