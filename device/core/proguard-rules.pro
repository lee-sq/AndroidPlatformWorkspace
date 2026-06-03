# release AAR 自身混淆规则。
# native so 写死了 android_serialport_api.SerialPort 的 JNI 符号，类名和 native 方法名不能被改写。
-keep class android_serialport_api.SerialPort { *; }

# device-core 是业务方运行时入口，保留公开 API；包内私有实现仍允许 R8 混淆。
-keep class com.holderzone.device.core.** {
    public protected *;
}

-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
