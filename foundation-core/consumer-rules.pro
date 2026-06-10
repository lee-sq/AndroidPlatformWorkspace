# 引用方 App 开启混淆时会自动合并本文件。
# foundation-core 的公共 API 需要保持稳定，便于 XML、Fragment 恢复和宿主代码继续按语义化类名访问。
-keep class com.holderzone.foundation.core.** {
    public protected *;
}

# Logbook 云端/文件日志策略需要在宿主 App 混淆时保持稳定。
-keep class com.yuu.android.component.logbook.** { *; }
-dontwarn com.yuu.android.component.logbook.**

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers class com.holderzone.foundation.core.ui.widget.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

-keepclassmembers class com.holderzone.foundation.core.ui.** extends androidx.fragment.app.Fragment {
    public <init>();
}

# 保留 Android Log 类（部分代码直接使用 Log）
-keep class android.util.Log { *; }
