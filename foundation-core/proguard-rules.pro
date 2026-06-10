# release AAR 自身混淆规则。
# foundation-core 会被宿主继承，不能让 R8 改写访问级别或重新 final 化生命周期方法。
-dontoptimize

# 对外 API 的类名与 public/protected 成员必须保持稳定，否则引用方无法用语义化 API 编译。
-keep class com.holderzone.foundation.core.** {
    public protected *;
}

# XML inflate、Fragment 恢复、Kotlin/序列化反射场景需要保留基础元信息。
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# 自定义 View 可能由 XML 反射创建，需要保留构造方法。
-keepclassmembers class com.holderzone.foundation.core.ui.widget.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# Fragment/DialogFragment 可能由 FragmentManager 或宿主导航恢复实例。
-keepclassmembers class com.holderzone.foundation.core.ui.** extends androidx.fragment.app.Fragment {
    public <init>();
}

-keep class com.yuu.android.component.logbook.** { *; }
-dontwarn com.yuu.android.component.logbook.**
