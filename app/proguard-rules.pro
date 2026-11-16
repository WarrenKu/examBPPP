# ExamBPPP Proguard rules
-keep class id.go.zee.kemendikdasmen.exambppp.bridge.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
