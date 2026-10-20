package com.statussplitter

import android.content.Intent
import android.widget.Button
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], shadows=[WindowsFileProviderShadow::class])
class RepairFlowTest {
    @After fun clean() { ChunkRepository.clear() }
    private fun clips() {
        val dir = File(RuntimeEnvironment.getApplication().filesDir,"chunks").apply { mkdirs() }
        ChunkRepository.chunks = (0..1).map { i ->
            val f = File(dir,"p$i.mp4").apply { writeBytes(byteArrayOf(1,2,3)) }
            VideoChunk(i,f.path,0,1000,1000,3)
        }.toMutableList()
    }
    @Test fun reviewPreviewLaunchesReadableContentUri() {
        clips()
        val a=Robolectric.buildActivity(ChunkReviewActivity::class.java).setup().get()
        val rv=a.findViewById<RecyclerView>(R.id.rv_chunks)
        val adapter=rv.adapter as ChunkAdapter
        val h=adapter.onCreateViewHolder(rv,0)
        adapter.onBindViewHolder(h,0)
        h.btnPlay.performClick()
        val intent=shadowOf(a).nextStartedActivity
        assertNotNull("Preview must open a real video player",intent)
        assertEquals(Intent.ACTION_VIEW,intent.action)
        assertEquals("content",intent.data?.scheme)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
    @Test fun recreationDoesNotReshareOrForgetPendingConfirmation() {
        clips()
        val c=Robolectric.buildActivity(PostingActivity::class.java).setup()
        c.get().findViewById<Button>(R.id.btn_next_chunk).performClick()
        shadowOf(c.get()).nextStartedActivity
        c.recreate()
        assertNull(shadowOf(c.get()).nextStartedActivity)
        assertTrue(c.get().findViewById<Button>(R.id.btn_next_chunk).text.toString().contains("confirm",true))
        assertEquals(0,c.get().findViewById<ProgressBar>(R.id.progress_posting).progress)
        c.pause().stop().destroy()
    }
    @Test fun managerRejectsDoneBroadcastWithoutVerification() {
        val mgr=AutoPostManager.get(RuntimeEnvironment.getApplication())
        var completed=false
        mgr.setListener(object: AutoPostManager.AutoPostListener {
            override fun onComplete(msg:String) { completed=true }
        })
        mgr.handleBroadcast(Intent(AutoPostManager.ACTION_DONE))
        assertFalse("Unverified broadcast must not claim completion",completed)
        mgr.setListener(null)
    }
    @Test fun repositoryRestoresSelectionAndOrderFromDisk() {
        clips()
        ChunkRepository.chunks.reverse()
        ChunkRepository.chunks[0].selected=false
        val save=ChunkRepository.javaClass.methods.firstOrNull { it.name=="save" }
        val load=ChunkRepository.javaClass.methods.firstOrNull { it.name=="restore" }
        assertNotNull("Process death requires durable repository",save)
        assertNotNull(load)
        val app=RuntimeEnvironment.getApplication()
        save!!.invoke(ChunkRepository,app)
        ChunkRepository.chunks=mutableListOf()
        load!!.invoke(ChunkRepository,app)
        assertEquals(2,ChunkRepository.chunks.size)
        assertTrue(ChunkRepository.chunks[0].filePath.endsWith("p1.mp4"))
        assertFalse(ChunkRepository.chunks[0].selected)
    }
}
