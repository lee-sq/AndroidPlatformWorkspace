# release AAR 自身混淆规则。
# 柜体 API 是业务方直接调用的 ABI 边界，保留语义化类名和公开成员。
-keep class com.holderzone.device.api.cabinet.** {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
