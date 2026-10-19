package com.dapm.inkacourier_app.controlador

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.VisitaFallida

/**
 * Controlador de HU-05. Con alcance de Activity, conserva el borrador del intento
 * y las visitas fallidas guardadas que muestra el detalle de la parada.
 */
class VisitaFallidaViewModel : ViewModel() {

    private val visitasGuardadas = mutableMapOf<String, VisitaFallida>()

    private val _borrador = MutableLiveData<VisitaFallida?>()
    val borrador: LiveData<VisitaFallida?> = _borrador

    fun obtenerBorrador(parada: Parada): VisitaFallida {
        val actual = _borrador.value
        if (actual != null && actual.idParada == parada.idParada) return actual

        val nuevo = VisitaFallida(idParada = parada.idParada, sectorRiesgo = parada.sectorRiesgo)
        _borrador.value = nuevo
        return nuevo
    }

    /** Causas disponibles: la falta de pago solo aplica a pedidos contraentrega (HU-05/CA-04). */
    fun causasDisponibles(parada: Parada): List<String> {
        val causas = mutableListOf(
            VisitaFallida.DESTINATARIO_AUSENTE,
            VisitaFallida.DIRECCION_NO_LOCALIZADA,
            VisitaFallida.RECEPCION_RECHAZADA
        )
        if (parada.esContraentrega()) causas.add(VisitaFallida.FALTA_DE_PAGO)
        return causas
    }

    fun seleccionarCausa(causa: String) {
        val b = _borrador.value ?: return
        b.causa = causa
        publicar(b)
    }

    fun actualizarObservaciones(observaciones: String) {
        val b = _borrador.value ?: return
        b.observaciones = observaciones
        publicar(b)
    }

    /** Datos de la gestión de contacto documentados por el repartidor (DEC-SEQ-01/5). */
    fun actualizarContacto(llegada: Long?, contacto: Long?, sinRespuesta: Boolean?, extensionAutorizada: Boolean?) {
        val b = _borrador.value ?: return
        b.llegadaDomicilio = llegada
        b.contactoDuranteVentana = contacto
        b.sinRespuesta = sinRespuesta
        b.extensionAutorizada = extensionAutorizada
        publicar(b)
    }

    fun actualizarRechazo(motivo: String?, constancias: String) {
        val b = _borrador.value ?: return
        b.motivoRechazo = motivo
        b.constanciasRechazo = constancias
        publicar(b)
    }

    fun actualizarExcepcion(motivo: String?, nota: String) {
        val b = _borrador.value ?: return
        b.motivoExoneracion = motivo
        b.notaSeguridad = nota
        publicar(b)
    }

    /** Vuelve a notificar el borrador para evaluarlo con la hora actual (el plazo de ausencia avanza con el reloj). */
    fun reevaluar() {
        val b = _borrador.value ?: return
        publicar(b)
    }

    /**
     * SEQ-07: asocia fecha, hora y GPS reales del intento (RF-17).
     * El Fragment la invoca cuando la documentación exigida está completa.
     */
    fun completarFechaUbicacion(ahora: Long) {
        val b = _borrador.value ?: return
        b.fechaHora = ahora
        b.latitud = UbicacionSimulada.LATITUD
        b.longitud = UbicacionSimulada.LONGITUD
        Log.d(TAG, "Fecha y ubicación asociadas al intento de ${b.idParada}")
        publicar(b)
    }

    /**
     * RF-18: contacto entre los minutos 5 y 7 desde la llegada, al menos 10 minutos
     * transcurridos, sin respuesta y sin extensión autorizada por Despacho.
     * Cualquier dato desconocido o incoherente impide registrar.
     */
    fun puedeRegistrarAusencia(ahora: Long): Boolean {
        val b = _borrador.value ?: return false
        val llegada = b.llegadaDomicilio ?: return false
        val contacto = b.contactoDuranteVentana ?: return false
        if (b.sinRespuesta != true || b.extensionAutorizada != false) return false
        if (contacto > ahora) return false
        val contactoEnVentana = contacto >= llegada + MINUTOS_INICIO_CONTACTO * UN_MINUTO &&
            contacto <= llegada + MINUTOS_FIN_CONTACTO * UN_MINUTO
        return contactoEnVentana && ahora >= llegada + MINUTOS_ESPERA_AUSENCIA * UN_MINUTO
    }

    /** Minutos desde la llegada al domicilio; null si no está informada o es futura. */
    fun minutosTranscurridos(ahora: Long): Long? {
        val llegada = _borrador.value?.llegadaDomicilio ?: return null
        if (llegada > ahora) return null
        return (ahora - llegada) / UN_MINUTO
    }

    /** Datos obligatorios según la causa, sin contar fecha y ubicación. */
    fun datosCompletos(parada: Parada, ahora: Long): Boolean {
        val b = _borrador.value ?: return false
        val causa = b.causa ?: return false
        if (causa !in causasDisponibles(parada) || b.observaciones.isBlank()) return false
        if (parada.esZonaAltoRiesgo() && b.motivoExoneracion == null) return false
        return when (causa) {
            VisitaFallida.DESTINATARIO_AUSENTE -> puedeRegistrarAusencia(ahora)
            VisitaFallida.RECEPCION_RECHAZADA -> b.motivoRechazo != null
            else -> true
        }
    }

    fun puedeGuardar(parada: Parada, ahora: Long): Boolean =
        datosCompletos(parada, ahora) && _borrador.value?.fechaHora != null

    /**
     * Guarda el intento (RF-16, RF-17). Revalida con la hora actual del dispositivo.
     * No registra cobro, recepción ni constancia: el pedido sigue sin entregar.
     */
    fun guardarVisitaFallida(parada: Parada, ahora: Long): VisitaFallida? {
        if (!parada.estado.estaPorAtender() || !puedeGuardar(parada, ahora)) {
            Log.w(TAG, "Visita fallida de ${parada.idParada} no válida; se conserva el borrador")
            return null
        }
        val guardada = _borrador.value!!.copy()
        // Solo se conservan los datos de la causa seleccionada.
        if (guardada.causa != VisitaFallida.DESTINATARIO_AUSENTE) {
            guardada.llegadaDomicilio = null
            guardada.contactoDuranteVentana = null
            guardada.sinRespuesta = null
            guardada.extensionAutorizada = null
        }
        if (guardada.causa != VisitaFallida.RECEPCION_RECHAZADA) {
            guardada.motivoRechazo = null
            guardada.constanciasRechazo = ""
        }
        visitasGuardadas[parada.idParada] = guardada
        Log.d(TAG, "Visita fallida guardada: ${guardada.causa} (${parada.idPedido})")
        descartarBorrador()
        return guardada
    }

    fun obtenerVisitaGuardada(idParada: String): VisitaFallida? = visitasGuardadas[idParada]

    fun descartarBorrador() {
        _borrador.value = null
    }

    /** Notifica el cambio del borrador a la vista. */
    private fun publicar(borrador: VisitaFallida) {
        _borrador.value = borrador
    }

    companion object {
        private const val TAG = "VisitaFallidaViewModel"
        const val UN_MINUTO = 60_000L
        const val MINUTOS_INICIO_CONTACTO = 5
        const val MINUTOS_FIN_CONTACTO = 7
        const val MINUTOS_ESPERA_AUSENCIA = 10
    }
}
