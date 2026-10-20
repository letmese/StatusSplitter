package com.statussplitter

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaRegressionTest {
    @Test fun invalidMediaIsFailureNotSuccessfulEmptyOutput() {
        val dir = java.nio.file.Files.createTempDirectory("bad-media").toFile()
        val result = VideoSplitter.splitVideo(File(dir, "missing.mp4").path, File(dir,"out"))
        assertTrue("Missing/corrupt media must fail explicitly", result.isFailure)
        dir.deleteRecursively()
    }
}
