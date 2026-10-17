# Smart Meal Planner AI ProGuard Rules

# Keep TensorFlow Lite models & native libraries
-keep class org.tensorflow.lite.** { *; }

# Keep Room database & entities
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Firebase models
-keepattributes *Annotation*
-keepattributes Signature
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Keep Data Models
-keep class com.smartmealplanner.ai.model.** { *; }
