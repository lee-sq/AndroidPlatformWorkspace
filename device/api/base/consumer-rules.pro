# 引用方 App 开启混淆时会自动合并本文件。
# device-api-base 是业务方直接调用的公共 API，需要保持类名和 public/protected 成员稳定。
-keep class com.holderzone.device.api.base.** {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
