package com.statussplitter

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SetupActivity: AppCompatActivity() {
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_setup)
        findViewById<TextView>(R.id.tv_setup_instructions).text = """
            SUPERVISED SHARING

            1. Select a video using the system picker. No broad media permission is needed.
            2. Split, preview each clip, choose clips, and reorder if needed.
            3. Share one clip. In WhatsApp select My status, review audience and preview, then tap Send yourself.
            4. Check My status. Return here and confirm only if you verified it posted.
            5. Share the next clip. If cancelled or uncertain, retry the same clip or leave it unconfirmed.

            Full Auto is unavailable because posting cannot be verified. The app never taps Send or uses coordinate gestures. Accessibility and overlay permissions are not needed; disable old StatusSplitter accessibility access if previously enabled.

            Supported input: H.264 video with optional AAC audio and monotonic timestamps. Clips are sync-aligned, at most 90 seconds and 16 MiB. No compression is implemented: oversized or unsupported input fails clearly; re-export at a lower bitrate without B-frames.

            Reopening saved clips after process death never resumes posting automatically. Verify My status before retrying to avoid duplicates.
        """.trimIndent()
        findViewById<Button>(R.id.btn_open_a11y_settings).apply { text="Back"; setOnClickListener { finish() } }
    }
}
