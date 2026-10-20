package com.statussplitter

import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionInvariantsTest {
    @Test fun pickerDoesNotRequireBroadStoragePermissions() {
        val app = RuntimeEnvironment.getApplication()
        val declared = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(declared.any { it.contains("READ_MEDIA") || it.contains("READ_EXTERNAL_STORAGE") })
    }
}
