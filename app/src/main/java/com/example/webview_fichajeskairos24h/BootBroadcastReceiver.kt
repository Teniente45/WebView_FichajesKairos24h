package com.example.webview_fichajeskairos24h

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// Clase que escucha el evento de arranque del dispositivo
class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Verificar que el evento es el arranque del sistema
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Iniciar la actividad principal de la aplicación
            val launchIntent = Intent(context, KioskLauncherActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Iniciar en nueva tarea
            context.startActivity(launchIntent)
            Log.d("BootBroadcastReceiver", "La aplicación se ha iniciado tras el reinicio.")
        }
    }
}
