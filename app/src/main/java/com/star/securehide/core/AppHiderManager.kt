package com.star.securehide.core

import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.RandomAccessFile

object AppHiderManager {

    fun isRoot(): Boolean = Shell.isAppGrantedRoot() == true

    fun setAppHiddenRoot(packageName: String, hide: Boolean): Boolean {
        val action = if (hide) "disable-user --user 0" else "enable"
        val result = Shell.cmd("pm $action $packageName").exec()
        return result.isSuccess
    }

    fun toggleHideFile(file: File): Boolean {
        return try {
            val parent = file.parentFile ?: return false
            val newName = if (file.name.startsWith(".")) {
                file.name.removePrefix(".")
            } else {
                ".${file.name}"
            }
            val target = File(parent, newName)
            scrambleHeader(file)
            file.renameTo(target)
        } catch (e: Exception) {
            false
        }
    }

    private fun scrambleHeader(file: File) {
        if (!file.exists() || file.length() < 16) return
        RandomAccessFile(file, "rw").use { raf ->
            val buffer = ByteArray(16)
            raf.read(buffer)
            for (i in buffer.indices) {
                buffer[i] = (buffer[i].toInt() xor 0xFF).toByte()
            }
            raf.seek(0)
            raf.write(buffer)
        }
    }
}
