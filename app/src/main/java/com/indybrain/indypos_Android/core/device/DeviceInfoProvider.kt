package com.indybrain.indypos_Android.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides device metadata required by the login API.
 */
@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getDeviceInfo(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()

        return DeviceInfo(
            deviceUuid = getDeviceUuid(),
            deviceName = listOf(manufacturer, model)
                .filter { it.isNotBlank() }
                .joinToString(separator = " ")
                .ifBlank { "Android Device" },
            deviceType = "mobile",
            platform = "Android",
            model = model.ifBlank { "Android" },
            version = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
            appVersion = getAppVersion()
        )
    }

    private fun getDeviceUuid(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return if (androidId.isNullOrBlank()) {
            // Fallback when androidId is unavailable
            java.util.UUID.randomUUID().toString()
        } else {
            androidId
        }
    }

    private fun getAppVersion(): String {
        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }
}

data class DeviceInfo(
    val deviceUuid: String,
    val deviceName: String,
    val deviceType: String,
    val platform: String,
    val model: String,
    val version: String,
    val appVersion: String
)

