package com.statussplitter

data class VideoChunk(
    var index: Int,
    val filePath: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    var selected: Boolean = true
) {
    val durationSeconds: Float get() = durationMs / 1000f
    val sizeMB: Float get() = sizeBytes / (1024f * 1024f)
    val label: String get() = "Part ${index + 1} (${String.format("%.1f", durationSeconds)}s)"
}