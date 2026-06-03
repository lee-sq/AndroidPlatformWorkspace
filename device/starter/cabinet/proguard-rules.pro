# release AAR 自身混淆规则。
# Starter 是推荐给业务方调用的显式初始化入口，需要保留类名和公开成员。
-keep class com.holderzone.device.starter.cabinet.** {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
