# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Billing
-keep class com.android.billingclient.** { *; }

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }

# Data modelleri (Gson/Firestore serileştirmesi için)
-keep class com.editlyai.app.data.model.** { *; }
