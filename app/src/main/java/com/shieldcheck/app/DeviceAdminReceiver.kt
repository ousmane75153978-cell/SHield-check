package com.shieldcheck.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ShieldCheckDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("ShieldCheck", "Device Admin activated")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("ShieldCheck", "Device Admin deactivated")
    }

    override fun onBusy(context: Context, intent: Intent) {
        super.onBusy(context, intent)
        Log.d("ShieldCheck", "Device Admin is busy")
    }

    override fun onUserRemoved(context: Context, intent: Intent) {
        super.onUserRemoved(context, intent)
        Log.d("ShieldCheck", "User removed")
    }
}
