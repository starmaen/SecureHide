package com.star.securehide.core

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku
import java.io.File
import java.io.RandomAccessFile

object AppHiderManager {

    // فحص الروت
    fun isRoot(): Boolean = Shell.isAppGrantedRoot() == true

    // فحص Shizuku
    fun isShizukuAvailable(): Boolean {
        return try {
            if (Shizuku.pingBinder()) {
                val perm = Shizuku.checkSelfPermission()
                perm == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // إخفاء التطبيق (روت أو Shizuku)
    fun setAppHidden(packageName: String, hide: Boolean): Boolean {
        val action = if (hide) "disable-user --user 0" else "enable"
        val command = "pm $action $packageName"

        return when {
            isRoot() -> {
                val result = Shell.cmd(command).exec()
                result.isSuccess
            }
            Shizuku.pingBinder() -> {
                try {
                    // استدعاء آمن لتجنب مشاكل الصلاحيات البرمجية أثناء البناء
                    val newProcessMethod = Shizuku::class.java.getMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    val process = newProcessMethod.invoke(
                        null,
                        arrayOf("sh", "-c", command),
                        null,
                        null
                    ) as Process
                    process.waitFor() == 0
                } catch (e: Exception) {
                    false
                }
            }
            else -> false
        }
    }
}
