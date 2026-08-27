# ============================================================
# kotlinx-serialization: keep @Serializable model classes so the
# generated serializers (and their class names) survive R8. Without
# this, release builds parse JSON fine at compile time but crash at
# runtime with SerializerNotFoundException / MissingFieldException.
# ============================================================
-keepattributes Signature, InnerClasses, *Annotation*, EnclosingMethod
-dontnote kotlinx.serialization.**

# Keep the whole class (name + members), not just members, so the
# companion serializer and its descriptor are not renamed/removed.
-keep class com.example.githubappstore.data.model.** { *; }

# ============================================================
# Retrofit / OkHttp
# Retrofit's own consumer rules keep interfaces annotated with
# @retrofit2.http.*, but keeping the package explicitly is safer.
# ============================================================
-keepattributes Signature, ExceptionInInitializerError, InnerClasses
-keep class com.example.githubappstore.data.remote.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn org.semver4j.**

# ============================================================
# Room (entities / DAOs / database). The room-runtime AAR already
# ships keep rules for these, but keeping the package explicitly
# avoids any R8 surprise with the KSP-generated implementations.
# ============================================================
-keep class com.example.githubappstore.data.cache.** { *; }
