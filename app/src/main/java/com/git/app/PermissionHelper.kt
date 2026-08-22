package com.git.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Android 11+ (API 30+) requires the "All files access" permission to read/write
     *  arbitrary directories outside the app's private sandbox. */
    fun hasAllFilesAccess(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    fun getAllFilesAccessIntent(context: Context): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        else
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        return Intent(action).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /** Fallback: open the app's settings details page where the user can find
     *  "All files access" under Special app access. Used when the MANAGE intent
     *  is not resolvable (some OEM ROMs) so we never crash. */
    fun getAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /** Returns true if the system can actually handle the given intent. */
    fun canResolve(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    /**
     * Safely launch the "All files access" settings page. Some devices / ROMs do not
     * expose the MANAGE_ALL_FILES_ACCESS_PERMISSION activity, so we fall back to the
     * app details page. Never throws ActivityNotFoundException.
     */
    fun launchAllFilesAccess(context: Context, launcher: ActivityResultLauncher<Intent>) {
        val intent = getAllFilesAccessIntent(context)
        val fallback = getAppDetailsIntent(context)
        val target = if (canResolve(context, intent)) intent else fallback
        try {
            launcher.launch(target)
        } catch (e: ActivityNotFoundException) {
            try {
                launcher.launch(fallback)
            } catch (_: Exception) {
            }
        }
    }
}
