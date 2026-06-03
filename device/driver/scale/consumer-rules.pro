# 引用方 App 开启混淆时会自动合并本文件。
# 业务方可能按 README 手动注册具体 driver，保留 driver 入口类和公开成员。
-keep,includedescriptorclasses class com.holderzone.device.driver.scale.**.*Driver {
    public protected *;
}

# 常量对象可能被业务方读取或用于日志/诊断，保留公开成员。
-keep class com.holderzone.device.driver.scale.constant.** {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
