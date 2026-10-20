package com.statussplitter

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight breadcrumb logger for diagnosing device-specific issues.
 * Writes timestamped events to app external storage for user reporting.
 */
object BreadcrumbLogger {
    private const val TAG = "SS-Breadcrumb"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    private fun getLogFile(context: Context): File {
        // Use external files dir so it persists and user can access it
        val dir = context.getExternalFilesDir("logs") ?: context.filesDir
        dir.mkdirs()
        return File(dir, "statussplitter_log.txt")
    }
    
    /**
     * Log a breadcrumb event with optional data.
     * @param context Any context (for file access)
     * @param event Short description of the event
     * @param data Optional additional data to log
     */
    fun log(context: Context, event: String, data: String? = null) {
        try {
            val timestamp = dateFormat.format(Date())
            val thread = Thread.currentThread().name
            val line = buildString {
                append("[$timestamp] [$thread] $event")
                if (data != null) append(" | $data")
                append("\n")
            }
            
            // Write to log file
            getLogFile(context).appendText(line)
            
            // Also log to logcat
            Log.d(TAG, event + if (data != null) " | $data" else "")
        } catch (e: Exception) {
            // Silently fail - logging should never crash the app
            Log.w(TAG, "Failed to write breadcrumb: ${e.message}")
        }
    }
    
    /**
     * Log an exception with full stack trace.
     */
    fun logException(context: Context, event: String, throwable: Throwable) {
        log(context, event, "${throwable.javaClass.simpleName}: ${throwable.message}")
        try {
            getLogFile(context).appendText(throwable.stackTraceToString() + "\n\n")
        } catch (e: Exception) { /* ignore */ }
    }
    
    /**
     * Clear the log file (call on app start if needed).
     */
    fun clear(context: Context) {
        try {
            getLogFile(context).writeText("")
        } catch (e: Exception) { /* ignore */ }
    }
    
    /**
     * Get log file path for user reporting.
     */
    fun getLogPath(context: Context): String = getLogFile(context).absolutePath
}
