package com.example.webview_fichajeskairos24h

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bloquear en orientación horizontal
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        hideSystemUI() // Activa la inmersión total al iniciar

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

    // Método para ocultar la barra de navegación
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    // Detectar cuando el usuario intenta interactuar con los botones de la barra de navegación
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            // Si se presionan los botones de inicio o multitarea, mostrar el PIN
            showPinDialog()
            return true  // Evitar que se ejecute la acción predeterminada (salir o cambiar de app)
        }
        return super.onKeyDown(keyCode, event)
    }

    // Método para bloquear la app y pedir el PIN solo cuando se intenta salir
    override fun onBackPressed() {
        // Evitar que el back botón cierre la app directamente
        showPinDialog()
    }

    // Método para mostrar el diálogo de PIN
    private fun showPinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ingrese el PIN")
            .setView(dialogView)
            .setCancelable(false)  // Deshabilitar cancelación
            .setPositiveButton("Aceptar") { dialog, _ ->
                val enteredPin = pinInput.text.toString()
                if (enteredPin == PIN) {
                    // Si el PIN es correcto, puedes salir
                    super.onBackPressed()
                } else {
                    // Si el PIN es incorrecto, muestra un mensaje y no hacer nada
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Evitar que cierre si el PIN es incorrecto
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }
}
