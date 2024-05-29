package ru.mishenko.maksim.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context

class AdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun getComponentName(context: Context) =
            ComponentName(context.applicationContext, AdminReceiver::class.java)
    }
}