package com.statussplitter

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.statussplitter.BreadcrumbLogger

class ChunkReviewActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var rv: RecyclerView
    private lateinit var tvSum: TextView
    private lateinit var rg: RadioGroup
    private lateinit var rbManual: RadioButton
    private lateinit var rbGuided: RadioButton
    private lateinit var rbFullAuto: RadioButton
    private lateinit var btn: Button
    private lateinit var adapter: ChunkAdapter
    private var vname = "video"

    override fun onCreate(s: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(s); setContentView(R.layout.activity_chunk_review)
        root = findViewById(R.id.root_layout_review)
        rv = findViewById(R.id.rv_chunks)
        tvSum = findViewById(R.id.tv_summary)
        rg = findViewById(R.id.rg_posting_mode)
        rbManual = findViewById(R.id.rb_manual_assist)
        rbGuided = findViewById(R.id.rb_guided_auto)
        rbFullAuto = findViewById(R.id.rb_full_auto)
        btn = findViewById(R.id.btn_proceed_to_post)
        
        vname = intent.getStringExtra("video_name") ?: "video"
        BreadcrumbLogger.log(this, "ChunkReview.onCreate", "video=$vname")
        setupInsets()
        ChunkRepository.restore(this)
        setupRV()
        updateSum()
        setupMode()
        setupBtn()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, b.top, v.paddingRight, b.bottom); ins
        }
    }

    private fun setupMode() {
        rbManual.isChecked = true
        ChunkRepository.selectedMode = ChunkRepository.PostingMode.MANUAL_ASSIST
        rg.setOnCheckedChangeListener { _, id ->
            ChunkRepository.selectedMode = when(id) {
                R.id.rb_full_auto -> ChunkRepository.PostingMode.FULL_AUTO
                R.id.rb_guided_auto -> ChunkRepository.PostingMode.GUIDED_AUTO
                else -> ChunkRepository.PostingMode.MANUAL_ASSIST
            }
        }
    }

    private fun setupBtn() {
        btn.setOnClickListener {
            BreadcrumbLogger.log(this, "PostButton.clicked", "Starting validation")
            val sel = ChunkRepository.getSelectedChunks()
            BreadcrumbLogger.log(this, "PostButton.chunks", "Selected: ${sel.size} chunks")
            if (sel.isEmpty()) { 
                BreadcrumbLogger.log(this, "PostButton", "No chunks selected, showing toast")
                Toast.makeText(this,"Select at least one",Toast.LENGTH_SHORT).show()
                return@setOnClickListener 
            }
            if (sel.any { !java.io.File(it.filePath).isFile || VideoSplitter.needsReEncode(it) }) {
                Toast.makeText(this,"Missing or oversized clip. Re-split or re-export at a lower bitrate.",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            ChunkRepository.save(this)
            BreadcrumbLogger.log(this, "PostButton", "Starting PostingActivity with video=$vname mode=${ChunkRepository.selectedMode}")
            startActivity(Intent(this, PostingActivity::class.java).apply { putExtra("video_name",vname) })
            BreadcrumbLogger.log(this, "PostButton", "startActivity called - returning from click handler")
        }
    }

    private fun setupRV() {
        adapter = ChunkAdapter(ChunkRepository.chunks,
            { c ->
                try {
                    val uri=androidx.core.content.FileProvider.getUriForFile(this,"$packageName.fileprovider",java.io.File(c.filePath))
                    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                } catch(e:Exception) { Toast.makeText(this,"No video player available: ${e.message}",Toast.LENGTH_LONG).show() }
            },
            { f,t -> adapter.moveItem(f,t) })
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun updateSum() {
        val sz = ChunkRepository.chunks.sumOf{it.sizeBytes}/(1024.0*1024.0)
        val dur = ChunkRepository.chunks.sumOf{it.durationMs}/1000.0
        val big = ChunkRepository.chunks.count{VideoSplitter.needsReEncode(it)}
        val sb = StringBuilder()
        sb.appendLine("Video: $vname"); sb.appendLine("Chunks: ${ChunkRepository.chunks.size}")
        sb.appendLine("Duration: %.1fs".format(dur)); sb.appendLine("Size: %.2fMB".format(sz))
        if (big>0) sb.appendLine("Warning: $big chunk(s) >16MB")
        tvSum.text = sb.toString()
    }

    override fun onPause() {
        runCatching { ChunkRepository.save(this) }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Update a11y status display if needed
    }
}
