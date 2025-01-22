package com.example.webview_fichajeskairos24h

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var clickCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var resetTask: Runnable? = null
    private val resetDelay = 10000L // 10 segundos

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) // Bloquea en horizontal

        // Activa la inmersión total al iniciar
        hideSystemUI()

        val webView: WebView = findViewById(R.id.webView)

        // Configuración de WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Limpieza de caché y cookies antes de cargar la URL
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Inyectar JavaScript para detectar clics en el botón "LIMPIAR"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("""
                    (function() {
                        const button = document.querySelector('button[onclick*="LIMPIAR"]');
                        if (button) {
                            button.addEventListener('click', function() {
                                Android.onButtonClick();
                            });
                        }
                    })();
                """.trimIndent(), null)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                val url = request?.url.toString()
                val errorMessage = error?.description.toString()
                Log.e("WebViewError", "Error cargando URL: $url | Error: $errorMessage")
                Toast.makeText(this@MainActivity, "Error al cargar la página: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }

        // Configura WebChromeClient para depuración de la consola
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let { Log.d("WebViewConsole", it) }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Añadir la interfaz JavaScript
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Cargar la URL
        val url = "https://setfichaje.kairos24h.es/index.php?r=citaRedWeb/cppIndex&xEntidad=1003&cKiosko=TABLET1"
        webView.loadUrl(url)

        // Permitir que el usuario salga del modo inmersivo con un toque
        webView.setOnClickListener {
            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        // Android 11 (API 30) y superior
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                // Ocultar barras de sistema (barra de estado y navegación)
                controller.hide(WindowInsets.Type.systemBars())
                // Deshabilitar gestos de navegación para que no aparezca la barra
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Para versiones anteriores a Android 11
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

    private fun showSystemUI() {
        // Mostrar las barras del sistema si se necesitan
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            // Para versiones anteriores a Android 11
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    // Interfaz para recibir eventos desde JavaScript
    inner class WebAppInterface {
        @JavascriptInterface
        fun onButtonClick() {
            runOnUiThread {
                clickCount++

                if (resetTask != null) {
                    handler.removeCallbacks(resetTask!!)
                }

                if (clickCount >= 6) {
                    showSystemUI()
                    resetTask = Runnable {
                        hideSystemUI()
                        clickCount = 0
                    }
                    handler.postDelayed(resetTask!!, resetDelay)
                } else {
                    resetTask = Runnable {
                        clickCount = 0
                    }
                    handler.postDelayed(resetTask!!, resetDelay)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            hideSystemUI()  // Asegurarse de que las barras sigan ocultas si la aplicación pierde foco
        }
    }
}
