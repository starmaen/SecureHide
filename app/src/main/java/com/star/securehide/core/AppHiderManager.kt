package com.star.securehide.core

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku
import java.io.File
import java.io.RandomAccessFile

object AppHiderManager {

    // فحص هل الروت متاح ومقبول
    fun isRoot(): Boolean = Shell.isAppGrantedRoot() == true

    // فحص هل Shizuku متاح ويعمل
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    // إخفاء التطبيق (يدعم روت أو Shizuku)
    fun setAppHidden(packageName: String, hide: Boolean): Boolean {
        val action = if (hide) "disable-user --user 0" else "enable"
        val command = "pm $action $packageName"

        return when {
            // 1. إذا كان الروت متاحاً
            isRoot() -> {
                val result = Shell.cmd(command).exec()
                result.isSuccess
            }
            // 2. إذا كان Shizuku متاحاً (بدون روت)
            Shizuku.pingBinder() -> {
                try {
                    val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                    process.waitFor() == 0
                } catch (e: Exception) {
                    false
                }
            }
            else -> false
        }
    }
}
