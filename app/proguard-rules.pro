# ExamBPPP Proguard rules
-keep class id.go.kemendikdasmen.exambppp.bridge.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
