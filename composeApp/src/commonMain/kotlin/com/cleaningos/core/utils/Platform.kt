package com.cleaningos.core.utils

/**
 * Platform.kt — Expect/Actual bridge for platform-specific implementations.
 * commonMain declares expect; androidMain/iosMain provide actuals.
 */

/** Platform name for debug/logging */
expect val platformName: String

/** Check if a runtime permission is granted (Android) / available (iOS) */
expect fun isPermissionGranted(permission: String): Boolean

/** Get platform-specific temp directory for cached files */
expect fun getCacheDir(): String
