# Add project specific ProGuard rules here.
-keep class org.web3j.** { *; }
-dontwarn org.web3j.**
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Strip logging and stack traces in release (smaller APK, no log leakage)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** println(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace(...);
}
