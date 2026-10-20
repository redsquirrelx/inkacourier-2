package com.dapm.inkacourier_app.controlador

import android.util.Log
import androidx.lifecycle.ViewModel
import com.dapm.inkacourier_app.modelo.Repartidor

/**
 * Contexto autenticado compartido (alcance de Activity). Contiene el catálogo
 * simulado de repartidores y el repartidor actual, que solo existe en memoria: un
 * proceso nuevo comienza sin sesión y exige iniciarla otra vez.
 */
class SesionViewModel : ViewModel() {

    /** Credenciales ilustrativas de la simulación; no son cuentas reales. */
    private val repartidores: List<Repartidor> = listOf(
        Repartidor(idRepartidor = ID_REPARTIDOR_LUCHO, nombre = "Lucho", usuario = "lucho", contrasena = "ruta01"),
        Repartidor(idRepartidor = ID_REPARTIDOR_ROSA, nombre = "Rosa", usuario = "rosa", contrasena = "ruta02")
    )

    var repartidorActual: Repartidor? = null
        private set

    /**
     * Operación local y síncrona. Usuario y contraseña deben coincidir exactamente,
     * sin normalizar, con los del mismo repartidor. Si fallan, no se establece sesión.
     */
    fun iniciarSesion(usuario: String, contrasena: String): Boolean {
        if (usuario.isEmpty() || contrasena.isEmpty()) return false
        val repartidor = repartidores.firstOrNull { it.usuario == usuario }
        if (repartidor == null || repartidor.contrasena != contrasena) {
            Log.w(TAG, "Inicio de sesión rechazado")
            return false
        }
        repartidorActual = repartidor
        Log.d(TAG, "Sesión iniciada: ${repartidor.idRepartidor}")
        return true
    }

    companion object {
        private const val TAG = "SesionViewModel"
        const val ID_REPARTIDOR_LUCHO = "REP-01"
        const val ID_REPARTIDOR_ROSA = "REP-02"
    }
}
