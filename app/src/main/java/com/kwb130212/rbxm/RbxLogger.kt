package com.kwb130212.rbxm

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lightweight local log for troubleshooting. No Roblox credentials or screen contents are stored. */
object RbxLogger {
    private const val FILE_NAME = "rbxm.log"
    private const val MAX_BYTES = 512 * 1024L
    private val lock = Any()

    fun info(context: Context, message: String) = write(context, "INFO", message)
    fun warn(context: Context, message: String) = write(context, "WARN", message)
    fun error(context: Context, message: String, throwable: Throwable? = null) {
        val detail = throwable?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: ""
        write(context, "ERROR", message + detail)
    }

    fun read(context: Context): String = synchronized(lock) {
        logFile(context).takeIf { it.exists() }?.readText() ?: ""
    }

    fun clear(context: Context) = synchronized(lock) {
        logFile(context).delete()
    }

    private fun write(context: Context, level: String, message: String) = synchronized(lock) {
        val file = logFile(context)
        rotateIfNeeded(file)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        file.appendText("$timestamp [$level] $message\n")
    }

    private fun logFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun rotateIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_BYTES) {
            val backup = File(file.parentFile, "$FILE_NAME.1")
            backup.delete()
            file.renameTo(backup)
        }
    }
}
