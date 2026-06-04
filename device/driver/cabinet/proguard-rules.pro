# release AAR 自身混淆规则。
# 业务方可能按 README 手动注册具体 driver，保留 driver 入口类和公开成员。
-keep,includedescriptorclasses class com.holderzone.device.driver.cabinet.**.*Driver {
    public protected *;
}

-keep class com.xingx.** { *; }
-keep class com.kongqw.serialportlibrary.** { *; }
-keep class com.june.serialport.** { *; }

-dontwarn com.xingx.**
-dontwarn com.kongqw.serialportlibrary.**
-dontwarn com.june.serialport.**

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
