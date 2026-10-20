package com.statussplitter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/** Legacy component retained for upgrade compatibility. No UI observation or actions. */
class StatusPostAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: StatusPostAccessibilityService? = null; private set
        fun isRunning(): Boolean = instance != null
    }
    val posting: Boolean get() = false
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Intentionally no actions. */ }
    override fun onInterrupt() { stop() }
    override fun onDestroy() { stop(); instance = null; super.onDestroy() }
    fun startFullAuto(chunks: List<String>) {
        sendBroadcast(Intent(AutoPostManager.ACTION_ERR).setPackage(packageName).apply {
            putExtra(AutoPostManager.EXTRA_MSG, "Automatic posting cannot be verified. Return to StatusSplitter and use supervised sharing with confirmation.")
            putExtra(AutoPostManager.EXTRA_ERR_CODE, 3)
            putExtra(AutoPostManager.EXTRA_HELP, true)
        })
    }
    fun stop() { }
    fun resume() { startFullAuto(emptyList()) }
}
