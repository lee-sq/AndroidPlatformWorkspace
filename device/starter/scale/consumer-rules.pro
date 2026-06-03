# 引用方 App 开启混淆时会自动合并本文件。
# Starter 是推荐给业务方调用的显式初始化入口，需要保留类名和公开成员。
-keep class com.holderzone.device.starter.scale.** {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
