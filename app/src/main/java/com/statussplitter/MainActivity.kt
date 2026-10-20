package com.statussplitter

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.launch
import java.io.File

import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private lateinit var root: View
    private lateinit var tvStatus: TextView
    private lateinit var btnPickVideo: Button
    private lateinit var btnEnableAuto: Button
    private lateinit var btnSetupGuide: Button
    private lateinit var btnFullAuto: Button
    private lateinit var tvAutoStatusText: TextView
    
    private var selectedUri: Uri? = null
    private var videoName: String? = null
    @Volatile var hasPermission = false

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onVideoSelected(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setupWindowInsets()
        checkPermissions()
        updateAutomationUI()
    }

    private fun initViews() {
        root = findViewById(R.id.root_layout)
        tvStatus = findViewById(R.id.tv_status)
        btnPickVideo = findViewById(R.id.btn_pick_video)
        btnEnableAuto = findViewById(R.id.btn_enable_automation)
        btnSetupGuide = findViewById(R.id.btn_setup_guide)
        btnFullAuto = findViewById(R.id.btn_full_auto)
        btnFullAuto.setOnClickListener { onFullAutoClicked() }
        tvAutoStatusText = findViewById(R.id.tv_automation_status)

        btnPickVideo.isEnabled = false
        btnPickVideo.setOnClickListener {
            if (hasPermission) pickVideoLauncher.launch("video/*") else checkPermissions()
        }
        btnEnableAuto.setOnClickListener { openAccessibilitySettings() }
        btnSetupGuide.setOnClickListener { startActivity(Intent(this, SetupActivity::class.java)) }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, bars.top, view.paddingRight, bars.bottom)
            insets
        }
    }

    // ==================== PERMISSIONS ====================
    private fun checkPermissions() {
        // ACTION_GET_CONTENT grants access to the selected URI only.
        onPermissionGranted()
    }

    private fun onPermissionGranted() {
        hasPermission = true
        resetUI()
        tvStatus.text = "Choose a video. Only the selected file is accessed."
    }

    // ==================== AUTOMATION UI ====================
    private fun onFullAutoClicked() { pickVideoLauncher.launch("video/*") }

    private fun updateAutomationUI() {
        tvAutoStatusText.text = "Supervised sharing only — you confirm each clip"
        btnFullAuto.text = "Choose video for supervised sharing"
        btnEnableAuto.text = "Sharing instructions"
        btnEnableAuto.isEnabled = true
        btnEnableAuto.setOnClickListener { startActivity(Intent(this, SetupActivity::class.java)) }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Enable StatusSplitter, then return", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val serviceId = "$packageName/${StatusPostAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(contentResolver, "enabled_accessibility_services") ?: ""
        return serviceId in enabledServices
    }

    // ==================== VIDEO HANDLING ====================
    private fun onVideoSelected(uri: Uri) {
        selectedUri = uri
        videoName = try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else "video.mp4"
                } else "video.mp4"
            } ?: "video.mp4"
        } catch (e: Exception) { "video.mp4" }

        tvStatus.text = "Selected: $videoName\nTap below to split."
        btnPickVideo.text = "Split & Review Chunks"
        btnPickVideo.setOnClickListener { splitVideo(uri) }
    }

    private fun splitVideo(uri: Uri) {
        btnPickVideo.isEnabled = false
        tvStatus.text = "Splitting..."
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val outputDir = File(filesDir, "chunks").also { it.mkdirs() }
                outputDir.listFiles()?.forEach { it.delete() }
                
                val inputFile = File(cacheDir, "input_video.mp4")
                (contentResolver.openInputStream(uri) ?: error("Cannot read selected video")).use { input ->
                    inputFile.outputStream().use { output -> input.copyTo(output) }
                }
                
                VideoSplitter.splitVideo(
                    inputFile.absolutePath,
                    outputDir,
                    90000L
                ) { current, total ->
                    this@MainActivity.runOnUiThread {
                        tvStatus.text = "Processing chunk $current of $total..."
                    }
                }.fold(
                    onSuccess = { chunks ->
                        if (chunks.isEmpty()) {
                            runOnUiThread { resetUI() }
                        } else {
                            ChunkRepository.chunks = chunks.toMutableList()
                            ChunkRepository.save(this@MainActivity)
                            runOnUiThread {
                                startActivity(Intent(this@MainActivity, ChunkReviewActivity::class.java).apply {
                                    putExtra("video_name", videoName)
                                })
                                resetUI()
                            }
                        }
                    },
                    onFailure = { exception ->
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: ${exception.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                            resetUI()
                        }
                    }
                )
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                    resetUI()
                }
            }
        }
    }

    private fun resetUI() {
        btnPickVideo.isEnabled = true
        btnPickVideo.text = "Select Video"
        btnPickVideo.setOnClickListener {
            if (hasPermission) pickVideoLauncher.launch("video/*") else checkPermissions()
        }
    }

    override fun onResume() {
        super.onResume()

        updateAutomationUI()
    }
}
