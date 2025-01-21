package com.example.webview_fichajeskairos24h

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webView)

        // Configuración de WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Establece el modo de caché
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE // Opción para evitar caché

        // Limpieza de caché y cookies antes de cargar la URL
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // Configura WebViewClient para manejar errores
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val url = request?.url.toString()
                val errorMessage = error?.description.toString()

                // Registra el error en Logcat
                Log.e("WebViewError", "Error cargando URL: $url | Error: $errorMessage")

                // Muestra el error con la URL en un Toast
                Toast.makeText(this@MainActivity, "Error al cargar la página: $errorMessage", Toast.LENGTH_LONG).show()
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val url = request?.url.toString()
                val statusCode = errorResponse?.statusCode
                val statusText = errorResponse?.reasonPhrase
            }
        }

        // Configura WebChromeClient para depuración de la consola
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let { Log.d("WebViewConsole", it) }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Cargar la URL
        val url = "https://www.youtube.com/results?search_query=world+of+warcraft"
        webView.loadUrl(url)
    }
}
