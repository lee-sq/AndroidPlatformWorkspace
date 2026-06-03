package android_serialport_api

import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Auth：ligen26
 * CrateTime：2026/6/2 16:48
 * Description: native 串口壳类，保持 android_serialport_api 包名以适配旧版 libserial_port.so 的 JNI 符号。
 */
class SerialPort(
    device: File,
    baudRate: Int,
    flags: Int,
) {
    private val mFd: FileDescriptor
    private val mFileInputStream: FileInputStream
    private val mFileOutputStream: FileOutputStream

    val inputStream: FileInputStream
        get() = mFileInputStream

    val outputStream: FileOutputStream
        get() = mFileOutputStream

    init {
        ensureDevicePermission(device)
        // 这里的 open 方法名和包名需要与旧 native so 的 JNI 符号保持一致。
        mFd = open(device.absolutePath, baudRate, flags)
            ?: throw IOException("Native serial open returned null for ${device.absolutePath}")
        mFileInputStream = FileInputStream(mFd)
        mFileOutputStream = FileOutputStream(mFd)
    }

    private fun ensureDevicePermission(device: File) {
        if (device.canRead() && device.canWrite()) {
            return
        }

        try {
            // Android 串口设备文件常需要 root 修改权限；失败时让上层探测流程感知异常。
            val process = Runtime.getRuntime().exec("/system/bin/su")
            val command = "chmod 666 ${device.absolutePath}\nexit\n"
            process.outputStream.use { output ->
                output.write(command.toByteArray())
                output.flush()
            }
            if (process.waitFor() != 0 || !device.canRead() || !device.canWrite()) {
                throw SecurityException("No read/write permission for ${device.absolutePath}")
            }
        } catch (exception: Exception) {
            throw SecurityException(
                "Failed to grant serial port permission for ${device.absolutePath}",
                exception,
            )
        }
    }

    external fun close()

    private companion object {
        init {
            System.loadLibrary("serial_port")
        }

        @JvmStatic
        private external fun open(
            path: String,
            baudRate: Int,
            flags: Int,
        ): FileDescriptor?
    }
}
