package com.statussplitter

import android.content.Context
import android.content.Intent

/** Compatibility facade: automation deliberately unavailable without verifiable posting. */
class AutoPostManager private constructor() {
    companion object {
        private val instance=AutoPostManager()
        fun get(context: Context)=instance
        const val ACTION_PROG="com.ss.PROG"
        const val ACTION_DONE="com.ss.DONE"
        const val ACTION_ERR="com.ss.ERROR"
        const val ACTION_STOPPED="com.ss.STOPPED"
        const val EXTRA_MSG="msg"
        const val EXTRA_ERR_CODE="err"
        const val EXTRA_HELP="help"
        val isRunning=false
    }
    interface AutoPostListener {
        fun onProgress(idx:Int, tot:Int, msg:String, step:String) {}
        fun onComplete(msg:String) {}
        fun onError(msg:String, code:Int, needsHelp:Boolean) {}
        fun onStopped() {}
    }
    private var listener: AutoPostListener?=null
    fun setListener(value:AutoPostListener?):AutoPostManager { listener=value; return this }
    fun startFullAuto(chunks:List<VideoChunk>) {
        listener?.onError("Automatic posting cannot be verified. Use supervised sharing and confirmation.",3,true)
    }
    fun stop() { StatusPostAccessibilityService.instance?.stop() }
    fun resume() { startFullAuto(emptyList()) }
    fun handleBroadcast(intent:Intent?) {
        // No broadcast, timeout, or accessibility event proves that a status was posted.
    }
}
