package com.statussplitter

import android.content.Intent
import android.widget.Button
import android.widget.ProgressBar
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [WindowsFileProviderShadow::class])
class PostingRegressionTest {
    @After fun cleanup() { ChunkRepository.clear() }

    private fun newPostingActivity(mode: ChunkRepository.PostingMode): PostingActivity {
        ChunkRepository.selectedMode = mode
        val dir = File(RuntimeEnvironment.getApplication().filesDir, "chunks").apply { mkdirs() }
        val clip = File(dir, "test.mp4").apply { writeBytes(byteArrayOf(1,2,3)) }
        ChunkRepository.chunks = mutableListOf(VideoChunk(0, clip.path, 0, 1000, 1000, 3))
        return Robolectric.buildActivity(PostingActivity::class.java).setup().get()
    }

    @Test fun sharingDoesNotCountAsConfirmedPosting() {
        val activity = newPostingActivity(ChunkRepository.PostingMode.MANUAL_ASSIST)
        activity.findViewById<Button>(R.id.btn_next_chunk).performClick()
        assertNotNull("Explicit share should launch a recipient app", shadowOf(activity).nextStartedActivity)
        assertEquals("Launching share must not increment confirmed count", 0,
            activity.findViewById<ProgressBar>(R.id.progress_posting).progress)
        activity.finish()
    }

    @Test fun fullAutoFailsClosedWithoutAttemptingAnExternalLaunch() {
        val activity = newPostingActivity(ChunkRepository.PostingMode.FULL_AUTO)
        assertNull("Unverifiable full-auto must stay in app", shadowOf(activity).nextStartedActivity)
        assertTrue(activity.findViewById<android.widget.TextView>(R.id.tv_posting_status).text.toString().contains("confirmation", true))
        assertFalse(AutoPostManager.isRunning)
        activity.findViewById<Button>(R.id.btn_next_chunk).performClick()
        assertEquals("Fallback is the supervised flow: share intent out, no progress jump", 0,
            activity.findViewById<ProgressBar>(R.id.progress_posting).progress)
        activity.finish()
    }

    @Test fun accessibilityEntryPointCannotStartUnverifiedAutomation() {
        val service = Robolectric.buildService(StatusPostAccessibilityService::class.java).create().get()
        service.startFullAuto(listOf("clip.mp4"))
        assertFalse("No verified WhatsApp integration: service must fail closed", service.posting)
        service.stop()
    }
}
