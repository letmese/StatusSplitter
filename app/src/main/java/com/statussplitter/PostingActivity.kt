package com.statussplitter

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class PostingActivity: AppCompatActivity() {
    private lateinit var tv:TextView
    private lateinit var button:Button
    private lateinit var progress:ProgressBar
    private var index=0
    private var pending=false
    private var blocked=false
    private var chunks:List<VideoChunk> = emptyList()
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        setContentView(R.layout.activity_posting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout_posting)) { v, ins ->
            val b=ins.getInsets(WindowInsetsCompat.Type.systemBars()); v.setPadding(20,b.top,20,b.bottom); ins
        }
        tv=findViewById(R.id.tv_posting_status); button=findViewById(R.id.btn_next_chunk); progress=findViewById(R.id.progress_posting)
        ChunkRepository.restore(this)
        chunks=ChunkRepository.getSelectedChunks().map { it.copy() }
        index=(state?.getInt("index") ?: 0).coerceIn(0,chunks.size)
        pending=state?.getBoolean("pending") ?: false
        blocked=state?.getBoolean("blocked") ?: (ChunkRepository.selectedMode==ChunkRepository.PostingMode.FULL_AUTO)
        render()
    }
    private fun render() {
        progress.max=chunks.size; progress.progress=index
        when {
            chunks.isEmpty() -> { tv.text="No saved clips available. Return and split a video."; button.text="Back"; button.setOnClickListener { finish() } }
            blocked -> {
                tv.text="Full Auto unavailable: posting cannot be verified. Use supervised sharing with explicit confirmation after checking My status."
                button.text="Use supervised sharing"
                button.setOnClickListener { blocked=false; render() }
            }
            index>=chunks.size -> {
                tv.text="You confirmed $index clips. StatusSplitter cannot verify upload or delivery."
                button.text="Finish"; button.setOnClickListener { finish() }
            }
            pending -> {
                tv.text="Clip ${index+1}/${chunks.size} shared for review. In WhatsApp select My status, check audience, and send. Return only after checking My status. Sharing or returning does not prove posting."
                button.text="Confirm or retry clip"
                button.setOnClickListener {
                    AlertDialog.Builder(this).setTitle("Did you verify this clip in My status?")
                        .setMessage("Confirm only after checking it posted. If cancelled, failed, or unsure, retry or leave it unconfirmed.")
                        .setPositiveButton("I verified it") { _,_ -> index++; pending=false; render() }
                        .setNeutralButton("Retry same clip") { _,_ -> shareCurrent() }
                        .setNegativeButton("Not confirmed",null).show()
                }
            }
            else -> {
                tv.text="Supervised sharing: ${index} of ${chunks.size} user-confirmed. You choose My status and tap Send yourself."
                button.text="Share clip ${index+1} to WhatsApp"
                button.setOnClickListener { shareCurrent() }
            }
        }
    }
    private fun shareCurrent() {
        try {
            val file=File(chunks[index].filePath)
            require(file.isFile && file.length()>0) { "Clip missing. Split the video again." }
            require(file.length()<=VideoSplitter.MAX_CHUNK_SIZE_BYTES) { "Clip exceeds 16 MiB. Re-export at a lower bitrate; compression is not available." }
            val uri=FileProvider.getUriForFile(this,"$packageName.fileprovider",file)
            val intent=Intent(Intent.ACTION_SEND).apply {
                type="video/mp4"; putExtra(Intent.EXTRA_STREAM,uri)
                clipData=ClipData.newRawUri("Video clip",uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); setPackage("com.whatsapp")
            }
            try { startActivity(intent) }
            catch(e:android.content.ActivityNotFoundException) {
                intent.setPackage(null); startActivity(Intent.createChooser(intent,"Choose WhatsApp or a video app"))
            }
            pending=true; render()
        } catch(e:Exception) {
            tv.text="Share failed: ${e.message}. No clip has been confirmed."
            button.text="Retry share"; button.setOnClickListener { shareCurrent() }
        }
    }
    override fun onSaveInstanceState(out:Bundle) {
        out.putInt("index",index); out.putBoolean("pending",pending); out.putBoolean("blocked",blocked)
        super.onSaveInstanceState(out)
    }
    override fun onDestroy() {
        AutoPostManager.get(this).setListener(null).stop()
        stopService(Intent(this,PostingOverlayService::class.java))
        super.onDestroy()
    }
}
