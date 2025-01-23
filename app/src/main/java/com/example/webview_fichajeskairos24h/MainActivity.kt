package com.example.webview_fichajeskairos24h

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // ----- Variables para el conteo de taps -----
    private var tapCount = 0
    private val requiredTaps = 8
    private val tapWindowMs = 10_000L // 10 seg

    private var firstTapTime: Long = 0L
    // --------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bloquea la pantalla en horizontal
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Llama a ocultar las barras del sistema
        hideSystemUI()

        // --- Iniciar MODO KIOSKO si somos Device Owner ---
        enableKioskMode()

        // Referencia al WebView
        val webView: WebView = findViewById(R.id.webView)

        // Configuración WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Limpiar caché y cookies antes de cargar la URL
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Configurar WebViewClient para tu lógica
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inyección de JS para detectar "LIMPIAR" (según tu ejemplo)
                webView.evaluateJavascript(
                    """
                        (function() {
                            const button = document.querySelector('button[onclick*="LIMPIAR"]');
                            if (button) {
                                button.addEventListener('click', function() {
                                    Android.onButtonClick();
                                });
                            }
                        })();
                    """.trimIndent(), null
                )
            }

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
                    this@MainActivity,
                    "Error al cargar la página: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Configura WebChromeClient para ver logs de consola
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let {
                    Log.d("WebViewConsole", it)
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Añadir interfaz JavaScript
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Cargar la URL
        val url = "https://setfichaje.kairos24h.es/index.php?r=citaRedWeb/cppIndex&xEntidad=1003&cKiosko=TABLET1"
        webView.loadUrl(url)
    }

    /**
     * En modo Kiosko real (Device Owner), se fuerza la app con LockTask.
     */
    private fun enableKioskMode() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, AdminReceiver::class.java)

        // Comprueba si esta app es Device Owner
        if (dpm.isDeviceOwnerApp(packageName)) {
            // Asigna este paquete como el único "pinneable"
            dpm.setLockTaskPackages(adminName, arrayOf(packageName))

            // Opcionalmente, puedes permitir ciertas features (Home, notificaciones, etc.)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setLockTaskFeatures(
                    adminName,
                    DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                            DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
                )
            }

            // Inicia lockTask sin confirmación de usuario
            startLockTask()
        } else {
            Log.e("MainActivity", "La app NO es Device Owner. No se puede forzar el Kiosko.")
            Toast.makeText(
                this,
                "La app NO es Device Owner. El kiosko real no funcionará.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Oculta barras de estado y navegación para lograr modo inmersivo.
     */
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

    /**
     * Llamar a stopLockTask() para “desanclar” la app del modo kiosko.
     */
    private fun disableKioskMode() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error deteniendo LockTask: ${e.message}")
        }
    }

    /**
     * Intercepta cualquier toque en la Activity:
     * si llegamos a 8 toques en menos de 10s => desbloqueamos kiosk.
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()

            // Si es el primer toque o han pasado >10s desde el primer toque
            if (tapCount == 0 || (currentTime - firstTapTime) > tapWindowMs) {
                tapCount = 1
                firstTapTime = currentTime
            } else {
                tapCount++
                // Si llegamos a los taps requeridos antes de 10s
                if (tapCount >= requiredTaps) {
                    Toast.makeText(this, "Modo Kiosko desactivado", Toast.LENGTH_SHORT).show()
                    disableKioskMode()
                    tapCount = 0
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Interfaz para recibir eventos desde JavaScript
     * (p.e. cuando pulsas el botón "LIMPIAR" en la web).
     */
    inner class WebAppInterface {
        @JavascriptInterface
        fun onButtonClick() {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "Botón LIMPIAR pulsado (desde JS)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            hideSystemUI() // Asegura que, si se pierde foco, se re-oculten las barras
        }
    }
}
