package com.cleaningos.core.utils

import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIDevice

actual val platformName: String = UIDevice.currentDevice.systemName() + " " +
    UIDevice.currentDevice.systemVersion

actual fun isPermissionGranted(permission: String): Boolean = true // iOS uses Info.plist

actual fun getCacheDir(): String = NSTemporaryDirectory()
