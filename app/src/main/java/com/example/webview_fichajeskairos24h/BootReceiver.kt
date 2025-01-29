package com.example.webview_fichajeskairos24h

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado: iniciando KioskLauncherActivity...")

            val launchIntent = Intent(context, KioskLauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error iniciando la actividad: ${e.message}")
            }
        }
    }

    companion object {
        // Método para habilitar BootReceiver manualmente si está deshabilitado
        fun enableReceiver(context: Context) {
            val pm = context.packageManager
            val componentName = ComponentName(context, BootReceiver::class.java)
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("BootReceiver", "BootReceiver habilitado manualmente")
        }
    }
}
