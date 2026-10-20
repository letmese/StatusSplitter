package com.statussplitter

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.Build
import java.io.File
import java.nio.ByteBuffer

/** Lossless remuxing only. Unsupported inputs fail explicitly; no pretend compression. */
object VideoSplitter {
    const val MAX_CHUNK_SIZE_BYTES = 16 * 1024 * 1024
    const val DEFAULT_MAX_DURATION_MS = 90_000L
    private const val MAX_SAMPLE_BYTES = 32 * 1024 * 1024

    fun planRanges(syncFrames: LongArray, durationUs: Long, maxUs: Long): List<LongRange> {
        require(durationUs > 0 && maxUs > 0)
        val sync = syncFrames.distinct().sorted()
        require(sync.firstOrNull() == 0L) { "Video must start with a sync frame at zero; export as H.264 MP4 first." }
        val ranges = mutableListOf<LongRange>()
        var start = 0L
        while (start < durationUs) {
            val end = if (durationUs - start <= maxUs) durationUs else
                sync.lastOrNull { it > start && it - start <= maxUs }
                    ?: throw IllegalArgumentException("Keyframe gap exceeds clip limit; re-export with more frequent keyframes.")
            ranges.add(start until end)
            start = end
        }
        return ranges
    }

    fun splitVideo(inputPath: String, outputDir: File, maxDurationMs: Long = DEFAULT_MAX_DURATION_MS,
                   onProgress: (Int, Int) -> Unit = { _, _ -> }): Result<List<VideoChunk>> = runCatching {
        require(File(inputPath).isFile) { "Input video is missing." }
        require(maxDurationMs in 1..DEFAULT_MAX_DURATION_MS) { "Clip duration must be 1–90000 ms." }
        require(outputDir.isDirectory || outputDir.mkdirs()) { "Cannot create clip directory." }
        val created = mutableListOf<File>()
        try {
            val source = inspect(inputPath)
            val ranges = planRanges(source.sync.toLongArray(), source.duration, maxDurationMs * 1000)
            val batch = java.util.UUID.randomUUID().toString()
            ranges.mapIndexed { index, range ->
                check(!Thread.currentThread().isInterrupted) { "Splitting cancelled." }
                onProgress(index+1, ranges.size)
                val file = File(outputDir,"${batch}_chunk_%03d.mp4".format(index))
                created.add(file)
                remux(inputPath, file, range.first, range.last+1, source)
                validate(file, source.audio != null, maxDurationMs)
                VideoChunk(index,file.absolutePath,range.first/1000,(range.last+1)/1000,
                    (range.last+1-range.first)/1000,file.length())
            }.also { require(it.isNotEmpty()) { "No playable video clips produced." } }
        } catch(e: Exception) {
            created.forEach { it.delete() }
            throw e
        }
    }

    private data class Source(val video: Int, val audio: Int?, val duration: Long,
                              val sync: List<Long>, val rotation: Int)
    private fun inspect(path: String): Source {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(path)
            val videos = (0 until extractor.trackCount).filter {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }
            val audios = (0 until extractor.trackCount).filter {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            require(videos.size == 1 && audios.size <= 1) { "Use a video with one video track and at most one audio track." }
            val v=videos.single(); val a=audios.firstOrNull()
            val format=extractor.getTrackFormat(v)
            require(format.getString(MediaFormat.KEY_MIME)=="video/avc") { "Only H.264/AVC video is supported. Re-export as H.264 MP4." }
            require(a==null || extractor.getTrackFormat(a).getString(MediaFormat.KEY_MIME)=="audio/mp4a-latm") {
                "Only AAC audio is supported. Re-export as H.264/AAC MP4."
            }
            val duration=format.getLong(MediaFormat.KEY_DURATION)
            require(duration>0) { "No readable video duration." }
            extractor.selectTrack(v)
            val sync=mutableListOf<Long>()
            var previous=-1L
            while(extractor.sampleTrackIndex>=0) {
                val time=extractor.sampleTime
                require(time>previous && time<duration) { "Unsupported reordered or invalid timestamps. Re-export without B-frames." }
                require(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED==0) { "Encrypted video is unsupported." }
                if(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC!=0) sync.add(time)
                previous=time
                extractor.advance()
            }
            require(previous>=0 && sync.isNotEmpty()) { "No readable video samples." }
            val metadata=MediaMetadataRetriever()
            val rotation=try {
                metadata.setDataSource(path)
                (metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0)
            } finally { metadata.release() }
            require(rotation in listOf(0,90,180,270)) { "Unsupported rotation metadata." }
            return Source(v,a,duration,sync,rotation)
        } finally { extractor.release() }
    }

    private fun remux(path: String, file: File, start: Long, end: Long, source: Source) {
        val extractor=MediaExtractor()
        var muxer: MediaMuxer?=null
        var started=false
        try {
            extractor.setDataSource(path)
            muxer=MediaMuxer(file.path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(source.rotation)
            val tracks=listOfNotNull(source.video,source.audio)
            val mapped=tracks.associateWith { muxer.addTrack(extractor.getTrackFormat(it)) }
            muxer.start(); started=true
            // Copy tracks separately: a common origin preserves A/V offsets, no fabricated timestamps.
            for(track in tracks) {
                extractor.selectTrack(track)
                extractor.seekTo(start,MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                var previous=-1L; var count=0
                var buffer: ByteBuffer?=null
                val info=MediaCodec.BufferInfo()
                while(extractor.sampleTrackIndex>=0) {
                    check(!Thread.currentThread().isInterrupted) { "Splitting cancelled." }
                    val pts=extractor.sampleTime
                    if(pts>=end) break
                    if(pts<start) { extractor.advance(); continue }
                    require(pts>previous) { "Unsupported reordered timestamps; re-export without B-frames." }
                    if(track==source.video && count==0) require(pts==start && extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC!=0) {
                        "Clip does not start on a sync frame."
                    }
                    val required=if(Build.VERSION.SDK_INT>=28) extractor.sampleSize else MAX_SAMPLE_BYTES.toLong()
                    require(required in 1..MAX_SAMPLE_BYTES.toLong()) { "Video sample exceeds safe buffer limit; re-export at a lower bitrate." }
                    if(buffer==null || buffer.capacity()<required) buffer=ByteBuffer.allocateDirect(required.toInt())
                    buffer.clear()
                    val size=extractor.readSampleData(buffer,0)
                    require(size>0 && size<=buffer.capacity()) { "Invalid or truncated media sample." }
                    require(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED==0) { "Encrypted sample unsupported." }
                    val flags=if(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC!=0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    info.set(0,size,pts-start,flags)
                    buffer.position(0); buffer.limit(size)
                    muxer.writeSampleData(mapped.getValue(track),buffer,info)
                    previous=pts; count++; extractor.advance()
                }
                require(count>0) { "A clip contains an empty video or audio track; re-export source." }
                extractor.unselectTrack(track)
            }
            muxer.stop(); started=false
        } finally {
            if(started) runCatching { muxer?.stop() }
            muxer?.release(); extractor.release()
        }
    }

    private fun validate(file: File, audioExpected: Boolean, maxDurationMs: Long) {
        require(file.length() in 1..MAX_CHUNK_SIZE_BYTES.toLong()) {
            "Clip exceeds 16 MiB. No clips were kept. Re-export at a lower bitrate; compression is not implemented."
        }
        val ex=MediaExtractor()
        try {
            ex.setDataSource(file.path)
            var videoFound=false; var audioFound=false
            for(i in 0 until ex.trackCount) {
                val fmt=ex.getTrackFormat(i); val mime=fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if(mime.startsWith("video/") || mime.startsWith("audio/")) {
                    ex.selectTrack(i); ex.seekTo(0,MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    require(ex.sampleTime>=0) { "Output has no media samples." }
                    if(mime.startsWith("video/")) {
                        videoFound=true
                        require(ex.sampleTime==0L && ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC!=0) { "Output is not sync-aligned." }
                        require(fmt.getLong(MediaFormat.KEY_DURATION)<=maxDurationMs*1000) { "Output exceeds duration limit; re-export source." }
                    } else audioFound=true
                    ex.unselectTrack(i)
                }
            }
            require(videoFound && (!audioExpected || audioFound)) { "Output lost required tracks." }
        } finally { ex.release() }
        val metadata=MediaMetadataRetriever()
        try {
            metadata.setDataSource(file.path)
            val frame=metadata.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            require(frame!=null) { "Output failed decode validation." }
            frame.recycle()
        } finally { metadata.release() }
    }
    fun needsReEncode(chunk: VideoChunk) = chunk.sizeBytes>MAX_CHUNK_SIZE_BYTES
}
