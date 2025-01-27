@file:Suppress("DEPRECATION")

package com.example.webview_fichajeskairos24h

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

internal class KioskLauncherActivity : AppCompatActivity() {

    // Variables para gestionar el modo kiosko
    private var isKioskModeEnabled = false
    private var longPressStartTime: Long = 0
    private var isLongPress = false
    private val longPressThreshold = 2000L // Tiempo en milisegundos para definir un largo presionado

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configuración de la orientación de la pantalla como horizontal
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Ocultar la barra de navegación y otros elementos del sistema para pantalla completa
        hideSystemUI()

        // Establecer la app como lanzador predeterminado
        setAsDefaultLauncher()

        // Habilitar el modo kiosko
        enableKioskMode()

        // Configuración del WebView
        val webView = findViewById<WebView>(R.id.webView)
        val webSettings = webView.settings

        // Habilitar JavaScript en el WebView
        webSettings.javaScriptEnabled = true
        // Habilitar almacenamiento local (DOM Storage)
        webSettings.domStorageEnabled = true
        // Deshabilitar caché para asegurar que la página siempre se cargue desde la red
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Limpiar caché, historial y cookies al iniciar
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Configuración del cliente WebViewClient para manejar eventos de página y errores
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // Verificar que la URL cargada es válida
                if (url.contains("kairos24h.es") && URLUtil.isValidUrl(url)) {
                    Log.d("KioskLauncherActivity", "URL final kairos y válida: $url")
                } else {
                    // Mostrar un mensaje si la URL no es válida
                    Toast.makeText(this@KioskLauncherActivity, "URL no válida", Toast.LENGTH_LONG)
                        .show()
                }
            }

            // Manejo de errores al cargar recursos en el WebView
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                val urlError = request.url.toString()
                val errorMessage = error.description.toString()
                Log.e("WebViewError", "Error cargando URL: $urlError | Error: $errorMessage")
            }
        }

        // Añadir la interfaz JavaScript para poder interactuar con la app desde el WebView
        webView.addJavascriptInterface(
            WebAppInterface(this), "Android"
        )

        // Cargar la URL de la página inicial
        val url = "https://setfichaje.kairos24h.es/index.php?r=citaRedWeb/cppIndex&xEntidad=1003&cKiosko=TABLET1"
        webView.loadUrl(url)
    }

    // Función para habilitar el modo kiosko
    @SuppressLint("ObsoleteSdkInt")
    private fun enableKioskMode() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, AdminReceiver::class.java)

        try {
            if (dpm.isDeviceOwnerApp(packageName)) {
                // Configura la app como la única anclada en modo kiosko
                dpm.setLockTaskPackages(adminName, arrayOf(packageName))

                // Dependiendo de la versión del sistema, habilitar restricciones de tarea
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    dpm.setLockTaskFeatures(adminName, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    dpm.setLockTaskFeatures(
                        adminName,
                        DevicePolicyManager.LOCK_TASK_FEATURE_HOME or DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
                    )
                }

                // Activar el modo kiosko
                startLockTask()
                isKioskModeEnabled = true
                Toast.makeText(this, "Modo kiosko activado. App anclada.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("KioskLauncherActivity", "La app NO es Device Owner.")
                Toast.makeText(this, "No somos Device Owner. No se puede habilitar el modo kiosko.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("KioskLauncherActivity", "Error al habilitar modo kiosko: " + e.message)
        }
    }

    // Función para deshabilitar el modo kiosko
    private fun disableKioskMode() {
        try {
            stopLockTask()
            isKioskModeEnabled = false
        } catch (e: Exception) {
            Log.e("KioskLauncherActivity", "Error al detener LockTask: " + e.message)
        }
    }

    // Establece la aplicación como el lanzador predeterminado
    private fun setAsDefaultLauncher() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            // Filtrar las actividades para establecer la app como lanzador predeterminado
            val filter = IntentFilter(Intent.ACTION_MAIN)
            filter.addCategory(Intent.CATEGORY_HOME)
            filter.addCategory(Intent.CATEGORY_DEFAULT)

            val component = ComponentName(packageName, KioskLauncherActivity::class.java.name)
            dpm.addPersistentPreferredActivity(admin, filter, component)
        }
    }

    // Función para ocultar las barras del sistema y lograr pantalla completa
    @SuppressLint("ObsoleteSdkInt")
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Para versiones anteriores, usar flags del sistema
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // Función para alternar entre activar y desactivar el modo kiosko
    private fun toggleKioskMode() {
        if (isKioskModeEnabled) {
            disableKioskMode()
        } else {
            enableKioskMode()
        }
        isKioskModeEnabled = !isKioskModeEnabled
    }

    // Detecta largos presionados en la parte superior izquierda para activar el menú PIN
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)

        // Obtener el ancho de la pantalla
        val screenWidth = resources.displayMetrics.widthPixels

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // Detectar si el toque está en los dos cuartos derechos de la pantalla
                if (ev.x > screenWidth / 2) {
                    longPressStartTime = System.currentTimeMillis()
                    isLongPress = true
                } else {
                    isLongPress = false
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isLongPress) {
                    val pressDuration = System.currentTimeMillis() - longPressStartTime
                    if (pressDuration >= longPressThreshold) {
                        showPinDialog()
                        toggleKioskMode()
                    }
                }
                isLongPress = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // Muestra un diálogo para salir del modo kiosko ingresando un PIN
    private fun showPinDialog() {
        val editText = EditText(this)
        editText.hint = "Introduce PIN"

        AlertDialog.Builder(this)
            .setTitle("Salir de Kiosko")
            .setView(editText)
            .setPositiveButton("Aceptar") { dialogInterface: DialogInterface, i: Int ->
                val pinIngresado = editText.text.toString().trim { it <= ' ' }
                // Verificar si el PIN es correcto
                if (checkPin(pinIngresado)) {
                    disableKioskMode()
                    clearDefaultLauncher()
                    setQuickstepAsDefaultLauncher()
                    finishAffinity()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton(
                "Cancelar"
            ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
            .show()
    }

    // Establece Quickstep como el lanzador predeterminado después de salir del modo kiosko
    private fun setQuickstepAsDefaultLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val componentName =
            ComponentName("com.android.quickstep", "com.android.quickstep/.MainActivity")
        intent.setComponent(componentName)

        startActivity(intent)
    }

    // Verifica si el PIN ingresado es correcto
    private fun checkPin(pin: String): Boolean {
        return pin == "3300"
    }

    // Limpia el lanzador predeterminado configurado previamente
    private fun clearDefaultLauncher() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            try {
                dpm.clearPackagePersistentPreferredActivities(admin, packageName)
                Toast.makeText(this, "Lanzador predeterminado eliminado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("KioskLauncherActivity", "Error al limpiar el lanzador predeterminado: " + e.message)
            }
        } else {
            Log.e("KioskLauncherActivity", "No somos Device Owner, no se puede eliminar el lanzador.")
        }
    }

    // Oculta la barra de navegación nuevamente si pierde el foco
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            hideSystemUI()
        }
    }

    // Asegura que el modo kiosko esté activado al reanudar la actividad
    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
}

class WebAppInterface(private val context: Context) {

    // Método accesible desde JavaScript
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}


