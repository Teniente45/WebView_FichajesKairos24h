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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class KioskLauncherActivity : AppCompatActivity() {

    private var isKioskModeEnabled = false
    private var longPressStartTime: Long = 0
    private var isLongPress = false
    private val longPressThreshold = 2000L // 2 segundos de duración para el largo presionado

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        hideSystemUI()

        setAsDefaultLauncher()

        enableKioskMode()

        val webView: WebView = findViewById(R.id.webView)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies {}
        CookieManager.getInstance().flush()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("kairos24h.es") == true) {
                    if (URLUtil.isValidUrl(url)) {
                        Log.d("KioskLauncherActivity", "URL final kairos y válida: $url")
                    } else {
                        Toast.makeText(this@KioskLauncherActivity, "URL no válida", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                val urlError = request?.url.toString()
                val errorMessage = error?.description.toString()
                Log.e("WebViewError", "Error cargando URL: $urlError | Error: $errorMessage")
                Toast.makeText(this@KioskLauncherActivity, "Error al cargar la página: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

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
                        DevicePolicyManager.LOCK_TASK_FEATURE_HOME or DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD
                    )
                }
                startLockTask()
                Toast.makeText(this, "Modo kiosko activado. App anclada.", Toast.LENGTH_SHORT).show()
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
            val filter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

            val component = ComponentName(packageName, KioskLauncherActivity::class.java.name)
            dpm.addPersistentPreferredActivity(admin, filter, component)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
            Toast.makeText(this, "Modo kiosko desactivado. Navegación habilitada.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("KioskLauncherActivity", "Error al detener LockTask: ${e.message}")
        }
    }

    private fun toggleKioskMode() {
        if (isKioskModeEnabled) {
            disableKioskMode()
        } else {
            enableKioskMode()
        }
        isKioskModeEnabled = !isKioskModeEnabled
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val screenHeight = resources.displayMetrics.heightPixels
                if (ev.y < screenHeight / 4) {  // Parte superior izquierda (primer cuarto de la pantalla)
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
                    clearDefaultLauncher() // Quita el lanzador predeterminado
                    setQuickstepAsDefaultLauncher() // Establece Quickstep como el lanzador predeterminado
                    finishAffinity() // Cierra todas las actividades de la app
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

    private fun setQuickstepAsDefaultLauncher() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val componentName = ComponentName("com.android.quickstep", "com.android.quickstep/.MainActivity") // Quickstep es el launcher predeterminado
        intent.component = componentName

        // Establece Quickstep como el lanzador predeterminado
        startActivity(intent)
    }

    private fun checkPin(pin: String): Boolean {
        return pin == "3300"  // El PIN que se debe ingresar
    }

    private fun clearDefaultLauncher() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.clearPackagePersistentPreferredActivities(admin, packageName)
            Toast.makeText(this, "Lanzador predeterminado eliminado", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("KioskLauncherActivity", "No se pudo eliminar la configuración de lanzador.")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            hideSystemUI()
        }
    }

    override fun onResume() {
        super.onResume()
        enableKioskMode()
    }

    // La interfaz WebAppInterface que interactúa con Javascript
    class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
