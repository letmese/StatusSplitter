package com.statussplitter

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File

// FileProvider assumes POSIX separators; Windows Robolectric does not emulate them.
// Only this platform boundary is substituted; Activity/share state is real.
@Implements(FileProvider::class)
class WindowsFileProviderShadow {
    companion object {
        @JvmStatic @Implementation
        fun getUriForFile(context: Context, authority: String, file: File): Uri =
            Uri.parse("content://$authority/chunks/${file.name}")
    }
}
