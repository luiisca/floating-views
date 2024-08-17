package io.github.luiisca.floating.views.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {
  /**
   * Display over other apps permission is not needed for versions older than M
   */
  private fun canDrawOverlays(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true
    }
    return Settings.canDrawOverlays(context)
  }

  /**
   * Starts the floating service if the overlay permission is granted, or requests the permission otherwise.
   *
   * @param context The service context
   * @param serviceClass The class of the service to be started.
   */
  fun startFloatServiceIfPermitted(context: Context, serviceClass: Class<*>) {
    if (canDrawOverlays(context)) {
      ContextCompat.startForegroundService(context, Intent(context, serviceClass))
    } else {
      // only >=23 apis will reach this block b/c of canDrawOverlays()
      @SuppressLint("InlinedApi")
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
      )
      context.startActivity(intent)
    }
  }
}
