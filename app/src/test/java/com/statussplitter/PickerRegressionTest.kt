package com.statussplitter

import android.content.Intent
import android.widget.Button
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PickerRegressionTest {
    @Test fun deniedBroadPermissionsDoNotBlockContentPicker() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val button = activity.findViewById<Button>(R.id.btn_pick_video)
        assertTrue("Document picker must remain usable without broad storage access", button.isEnabled)
        button.performClick()
        assertEquals(Intent.ACTION_GET_CONTENT, shadowOf(activity).nextStartedActivity.action)
        controller.pause().stop().destroy()
    }
}
