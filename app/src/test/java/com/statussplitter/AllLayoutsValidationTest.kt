package com.statussplitter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Real regression guard for the v1.3.1/v1.3.2 'android:height' bug:
 * every shipped layout must inflate at runtime and produce a non-empty hierarchy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllLayoutsValidationTest {
    private val layouts = listOf(
        "activity_main", "activity_chunk_review", "activity_posting", "activity_setup", "item_chunk"
    )

    @Test
    fun `every shipped layout inflates at runtime with a non-empty root`() {
        val activity = Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val context = activity // Activity context carries the app theme required by ?attr drawables
        for (name in layouts) {
            val id = context.resources.getIdentifier(name, "layout", context.packageName)
            assertTrue("Missing layout $name", id != 0)
            val root = LayoutInflater.from(context).inflate(id, null, false)
            assertTrue(
                "Layout $name must produce a non-empty view hierarchy (inflation regression guard)",
                root is ViewGroup && root.childCount > 0
            )
        }
        assertEquals(5, layouts.size)
    }
}
