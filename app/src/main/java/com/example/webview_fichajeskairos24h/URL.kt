package com.example.webview_fichajeskairos24h

object WebViewURL {
    const val DEMO = "https://setfichaje.kairos24h.es/index.php"
    const val PRODUCCION = ""

    // Partes ENTIDAD
    const val RUTA = "citaRedWeb/cppIndex"
    const val PRUEBA = "1000"
    const val COMPLIANCE = "1002"
    const val BERLIN = "1003"
    const val RAFA = "1004"
    var ENTIDAD = PRUEBA // Puedes cambiarlo dinámicamente si lo necesitas

    // Partes KIOSKO
    const val N_TABLET = "1"
    var KIOSKO = "TABLET$N_TABLET" // Puedes cambiarlo dinámicamente si lo necesitas

    // Función para construir la URL
    fun buildUrl(): String {
        return "$DEMO?r=$RUTA&xEntidad=$ENTIDAD&cKiosko=$KIOSKO"
    }

    // La URL de la página inicial
    val LOGIN_URL = buildUrl() // Llamamos a la función para obtener la URL completa
}
