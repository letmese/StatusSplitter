package com.statussplitter

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChunkRepository {
    var chunks: MutableList<VideoChunk> = mutableListOf()
    enum class PostingMode { MANUAL_ASSIST, GUIDED_AUTO, FULL_AUTO }
    var selectedMode = PostingMode.MANUAL_ASSIST
    fun getSelectedChunks() = chunks.filter { it.selected }
    fun clear() { chunks.clear(); selectedMode = PostingMode.MANUAL_ASSIST }
    fun save(context: Context) {
        val data = JSONArray()
        chunks.forEach { c -> data.put(JSONObject().apply {
            put("path", c.filePath); put("start", c.startTimeMs); put("end", c.endTimeMs)
            put("duration", c.durationMs); put("size", c.sizeBytes); put("selected", c.selected)
        }) }
        val file = AtomicFile(File(context.filesDir,"chunks.json"))
        val stream = file.startWrite()
        try { stream.write(data.toString().toByteArray()); file.finishWrite(stream) }
        catch(e: Exception) { file.failWrite(stream); throw e }
    }
    fun restore(context: Context) {
        if (chunks.isNotEmpty()) return
        runCatching {
            val data = JSONArray(AtomicFile(File(context.filesDir,"chunks.json")).readFully().toString(Charsets.UTF_8))
            val restored = (0 until data.length()).map { i ->
                val c=data.getJSONObject(i)
                val file=File(c.getString("path"))
                require(file.isFile && file.length()==c.getLong("size")) { "Saved clip missing or changed" }
                require(file.canonicalPath.startsWith(File(context.filesDir,"chunks").canonicalPath+File.separator))
                VideoChunk(i,file.path,c.getLong("start"),c.getLong("end"),c.getLong("duration"),c.getLong("size"),c.getBoolean("selected"))
            }
            chunks=restored.toMutableList()
        }.onFailure { clear() }
        // Never resume external posting automatically after process death.
        selectedMode=PostingMode.MANUAL_ASSIST
    }
}
