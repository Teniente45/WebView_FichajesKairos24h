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
import android.webkit.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class KioskLauncherActivity : AppCompatActivity() {

    // Variables para la detección de long press
    private var isLongPress = false
    private var longPressStartTime = 0L
    private val longPressThreshold = 7000L // 7 segundos

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bloquea la pantalla en horizontal
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Oculta las barras del sistema
        hideSystemUI()

        // Configura este Activity como el launcher preferente (solo si es Device Owner)
        setAsDefaultLauncher()

        // Habilita el modo Kiosko (LockTask) si somos Device Owner
        enableKioskMode()

        // Setup del WebView
        val webView: WebView = findViewById(R.id.webView)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        // Limpieza de caché/cookies
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies {}
        CookieManager.getInstance().flush()

        // Configura el WebViewClient (corrige la falta de llaves aquí)
        webView.webViewClient = object : WebViewClient() {

            // Cuando se finaliza la carga de una página
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Ejemplo: si la URL contiene "kairos24h.es", comprobamos si es válida
                if (url?.contains("kairos24h.es") == true) {
                    if (URLUtil.isValidUrl(url)) {
                        // OJO: llamar de nuevo a loadUrl(url) dentro de onPageFinished
                        // podría causar recargas infinitas si no se controla
                        Log.d("KioskLauncherActivity", "URL final kairos y válida: $url")
                    } else {
                        Toast.makeText(
                            this@KioskLauncherActivity,
                            "URL no válida",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // Manejo de errores al cargar la URL
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val urlError = request?.url.toString()
                val errorMessage = error?.description.toString()
                Log.e("WebViewError", "Error cargando URL: $urlError | Error: $errorMessage")

                Toast.makeText(
                    this@KioskLauncherActivity,
                    "Error al cargar la página: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Inyectar JavaScript para “LIMPIAR” u otros eventos
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let { Log.d("WebViewConsole", it) }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Interfaz JS
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Carga tu URL
        val url = "https://setfichaje.kairos24h.es/index.php?r=citaRedWeb/cppIndex&xEntidad=1003&cKiosko=TABLET1"
        webView.loadUrl(url)
    }

    private fun enableKioskMode() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, AdminReceiver::class.java)

        try {
            if (dpm.isDeviceOwnerApp(packageName)) {
                dpm.setLockTaskPackages(adminName, arrayOf(packageName))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    dpm.setLockTaskFeatures(
                        adminName,
                        DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                                DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
                    )
                }
                startLockTask()
            } else {
                Log.e("KioskLauncherActivity", "La app NO es Device Owner.")
                Toast.makeText(this, "No somos Device Owner.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("KioskLauncherActivity", "Error al habilitar modo kiosko: ${e.message}")
        }
    }

    private fun setAsDefaultLauncher() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.clearPackagePersistentPreferredActivities(admin, packageName)

            val filter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

            val component = ComponentName(packageName, this::class.java.name)
            dpm.addPersistentPreferredActivity(admin, filter, component)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    private fun disableKioskMode() {
        try {
            stopLockTask()
            Toast.makeText(this, "Modo kiosko desactivado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("KioskLauncherActivity", "Error al detener LockTask: ${e.message}")
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val screenWidth = resources.displayMetrics.widthPixels
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
                    }
                }
                isLongPress = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun showPinDialog() {
        val editText = EditText(this)
        editText.hint = "Introduce PIN"

        AlertDialog.Builder(this)
            .setTitle("Salir de Kiosko")
            .setView(editText)
            .setPositiveButton("Aceptar") { dialogInterface: DialogInterface, _: Int ->
                val pinIngresado = editText.text.toString().trim()
                if (checkPin(pinIngresado)) {
                    disableKioskMode()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancelar") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun checkPin(pin: String): Boolean {
        return pin == "3300"
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onButtonClick() {
            runOnUiThread {
                Toast.makeText(
                    this@KioskLauncherActivity,
                    "Botón LIMPIAR pulsado (desde JS)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            hideSystemUI()
        }
    }
}
