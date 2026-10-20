package com.statussplitter
import org.junit.Assert.*
import org.junit.Test

class SyncPlanTest {
    private fun plan(sync: LongArray, duration: Long, max: Long): List<LongRange> {
        val method = VideoSplitter.javaClass.methods.firstOrNull { it.name == "planRanges" }
        assertNotNull("Splitter needs a sync-aligned range planner instead of arbitrary timestamps", method)
        @Suppress("UNCHECKED_CAST")
        return method!!.invoke(VideoSplitter, sync, duration, max) as List<LongRange>
    }
    @Test fun cutsOnlyOnSyncFramesWithoutGapsOrOverlap() {
        assertEquals(listOf(0L until 80000L, 80000L until 160000L, 160000L until 190000L),
            plan(longArrayOf(0, 40000, 80000, 120000, 160000), 190000, 90000))
    }
    @Test fun rejectsGopLongerThanLimitInsteadOfCorruptingVideo() {
        try { plan(longArrayOf(0, 100000), 140000, 90000); fail("Must reject uncuttable GOP") }
        catch (e: java.lang.reflect.InvocationTargetException) { assertTrue(e.cause is IllegalArgumentException) }
    }
}
