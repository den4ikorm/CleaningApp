package com.cleaningos.core.utils

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cleaningos.core.platform.AppContextHolder

actual val platformName: String = "Android ${android.os.Build.VERSION.SDK_INT}"

actual fun isPermissionGranted(permission: String): Boolean {
    val ctx = AppContextHolder.appContext ?: return false
    return ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
}

actual fun getCacheDir(): String =
    AppContextHolder.appContext?.cacheDir?.absolutePath ?: "/tmp"
