# 引用方 App 开启混淆时会自动合并本文件。
# 业务方可能按 README 手动注册具体 driver，保留 driver 入口类和公开成员。
-keep,includedescriptorclasses class com.holderzone.device.driver.cabinet.**.*Driver {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
