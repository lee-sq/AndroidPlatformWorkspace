# release AAR 自身混淆规则。
# device-api-base 是所有设备模块和业务方的 ABI 边界，保留语义化类名和公开成员。
-keep class com.holderzone.device.api.base.** {
    public protected *;
}

# 保留 Kotlin 泛型、sealed/data class、注解等元信息，避免业务方反射或类型推断场景缺失签名。
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
