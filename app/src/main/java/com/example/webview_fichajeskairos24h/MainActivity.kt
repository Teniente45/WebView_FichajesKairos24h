package com.example.webview_fichajeskairos24h

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val PIN = "1234" // Establece el PIN de seguridad
    private var backPressCount = 0 // Contador de veces que se presiona la tecla "Back"
    private val backPressInterval: Long = 6000 // Intervalo de 2 segundos para contar los "Back presses"
    private var lastBackPressTime: Long = 0 // Hora de la última vez que se presionó "Back"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bloquear en orientación horizontal
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val webView: WebView = findViewById(R.id.webView)

        // Configuración de WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Limpiar la caché y evitar que se almacene
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Configura WebChromeClient para depuración de la consola
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let { Log.d("WebViewConsole", it) }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Cargar la URL
        val url = "https://setfichaje.kairos24h.es/index.php?r=citaRedWeb/cppIndex&xEntidad=1003&cKiosko=TABLET1"
        webView.loadUrl(url)

        enableDeviceAdmin() // Activar el Administrador de Dispositivo
    }

    // Método para activar el Administrador de Dispositivo
    private fun enableDeviceAdmin() {
        val deviceAdminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        // Verificar si la aplicación tiene permisos de administrador
        if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Para habilitar el modo kiosco")
            startActivityForResult(intent, 1) // Solicita los permisos
        } else {
            // Ya es administrador, activar el modo kiosco
            Log.d("DeviceAdmin", "La aplicación ya tiene privilegios de administrador.")
            devicePolicyManager.setLockTaskPackages(deviceAdminComponent, arrayOf(packageName))
            startLockTask() // Iniciar el modo kiosco
        }
    }

    // Detectar cuando el usuario presiona la tecla de "atrás"
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val currentTime = System.currentTimeMillis()

            // Verificar si el tiempo entre el último "back press" y este es menor que el intervalo permitido
            if (currentTime - lastBackPressTime <= backPressInterval) {
                backPressCount++
            } else {
                backPressCount = 1 // Reiniciar el contador si el intervalo es mayor al permitido
            }

            lastBackPressTime = currentTime // Actualizar el tiempo de la última vez que se presionó "back"

            // Si se presionan 5 veces seguidas, mostrar el cuadro de diálogo para el PIN
            if (backPressCount >= 5) {
                showPinDialog()
                backPressCount = 0 // Resetear el contador después de mostrar el diálogo
            }

            return true // Evitar que se ejecute la acción predeterminada (salir o cambiar de app)
        }
        return super.onKeyDown(keyCode, event)
    }

    // Método para mostrar el diálogo de PIN
    private fun showPinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Para salir introduzca PIN")
            .setView(dialogView)
            .setCancelable(false)  // Deshabilitar cancelación
            .setPositiveButton("Aceptar") { dialog, _ ->
                val enteredPin = pinInput.text.toString()
                if (enteredPin == PIN) {
                    // Si el PIN es correcto, desactivar el modo kiosco y salir de la app
                    stopLockTask()  // Desactivar el modo kiosco
                    finishAffinity() // Cerrar todas las actividades de la app y salir
                } else {
                    // Si el PIN es incorrecto, muestra un mensaje y no hace nada
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss() // Cerrar el diálogo sin hacer nada
            }
            .create()

        dialog.show()
    }

    // Asegurarse de que el modo kiosco se reinicie cada vez que la app entre en primer plano
    override fun onResume() {
        super.onResume()
        startLockTask()  // Activar el modo kiosco automáticamente
    }
}
