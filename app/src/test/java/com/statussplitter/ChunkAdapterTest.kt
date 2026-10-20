package com.statussplitter

import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regressions for recycled-view listener bugs in ChunkAdapter (v1.3.7 stale position bug). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChunkAdapterTest {
    private fun repo(vararg sizes: Long): MutableList<VideoChunk> =
        sizes.mapIndexed { i, s -> VideoChunk(i, "c$i.mp4", i * 1000L, (i + 1) * 1000L, 1000L, s) }.toMutableList()

    @Test
    fun `play invokes the preview callback`() {
        val activity = Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val chunks = repo(10, 20)
        var preview: VideoChunk? = null
        val adapter = ChunkAdapter(chunks, { preview = it }, { _, _ -> })
        val holder = adapter.onCreateViewHolder(activity.findViewById<RecyclerView>(R.id.rv_chunks), 0)
        adapter.onBindViewHolder(holder, 1)
        holder.btnPlay.performClick()
        assertEquals(chunks[1], preview)
        activity.finish()
    }

    @Test
    fun `reorder click uses current identity after move`() {
        val activity = Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val chunks = repo(10, 20, 30)
        var from = -1
        val adapter = ChunkAdapter(chunks, {}, { f, _ -> from = f })
        val holder = adapter.onCreateViewHolder(activity.findViewById<RecyclerView>(R.id.rv_chunks), 0)
        adapter.onBindViewHolder(holder, 1)
        adapter.moveItem(1, 2)
        holder.btnUp.performClick()
        assertEquals(2, from)
        activity.finish()
    }

    @Test
    fun `rebinding does not fire previous chunk listener and corrupt its state`() {
        val activity = Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val chunks = repo(10, 20, 30)
        val adapter = ChunkAdapter(chunks, {}, { _, _ -> })
        val h0 = adapter.onCreateViewHolder(activity.findViewById<RecyclerView>(R.id.rv_chunks), 0)
        adapter.onBindViewHolder(h0, 0)
        h0.itemView.findViewById<CheckBox>(R.id.cb_select).isChecked = false
        assertFalse(chunks[0].selected)
        // Recycle: same holder rebound to chunk B must not re-fire chunk A's listener.
        adapter.onBindViewHolder(h0, 1)
        assertFalse("Rebind corrupted the previously bound chunk via stale listener", chunks[0].selected)
        assertTrue(chunks[1].selected)
        activity.finish()
    }

    @Test
    fun `checkbox listener reflects the recycled holder position not the stale one`() {
        val activity = Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val chunks = repo(10, 20, 30)
        val adapter = ChunkAdapter(chunks, {}, { _, _ -> })
        val rv = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_chunks)
        rv.adapter = adapter
        rv.measure(0, 0)
        rv.layout(0, 0, 100, 1000)

        val h0 = adapter.onCreateViewHolder(rv, 0)
        adapter.onBindViewHolder(h0, 0)
        val h1 = adapter.onCreateViewHolder(rv, 0)
        adapter.onBindViewHolder(h1, 1)
        // Simulate recycling: holder h0 is rebound to position 2.
        adapter.onBindViewHolder(h0, 2)

        val cb = h0.itemView.findViewById<CheckBox>(R.id.cb_select)
        cb.isChecked = false
        assertFalse("Toggle after rebind must update chunk at the CURRENT position", chunks[2].selected)
        assertTrue("Old position must be untouched", chunks[0].selected)
        activity.finish()
    }

    @Test
    fun `moveItem keeps checkbox state in sync after reorder`() {
        val activity = Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val chunks = repo(10, 20, 30)
        chunks[0].selected = false
        val adapter = ChunkAdapter(chunks, {}, { _, _ -> })
        adapter.moveItem(0, 2)
        assertEquals("Order updated", 2, chunks.indexOfFirst { !it.selected })
        activity.finish()
    }
}
