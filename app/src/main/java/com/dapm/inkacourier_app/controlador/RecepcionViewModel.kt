package com.dapm.inkacourier_app.controlador

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.Recepcion
import java.math.BigDecimal

/**
 * Controlador de HU-03 y HU-04. Con alcance de Activity, comparte el borrador entre
 * el formulario, el diálogo de firma y el retorno de la cámara, y conserva en memoria
 * las recepciones guardadas para el detalle y la constancia.
 */
class RecepcionViewModel : ViewModel() {

    /** Recepciones guardadas por idPedido: impide registrar dos veces el mismo pedido (RF-27). */
    private val recepcionesGuardadas = mutableMapOf<String, Recepcion>()

    /** Recaudación física de la jornada: solo cobros en efectivo (RF-26). */
    var recaudacionFisicaSoles: BigDecimal = BigDecimal("0.00")
        private set

    private val _borrador = MutableLiveData<Recepcion?>()
    val borrador: LiveData<Recepcion?> = _borrador

    /** Crea el borrador de la parada o recupera el existente al volver de la firma o la cámara. */
    fun obtenerBorrador(parada: Parada): Recepcion {
        val actual = _borrador.value
        if (actual != null && actual.idParada == parada.idParada) return actual

        val nuevo = Recepcion(idParada = parada.idParada, sectorRiesgo = parada.sectorRiesgo)
        if (parada.esContraentrega()) {
            nuevo.medioPago = parada.mediosPagoAutorizados.first()
            nuevo.importeCobradoSoles = parada.importeContraentregaSoles
        }
        _borrador.value = nuevo
        return nuevo
    }

    fun actualizarIdentidad(nombre: String, tipoDocumento: String, numeroDocumento: String, verificada: Boolean) {
        val b = borradorActual() ?: return
        b.nombreReceptor = nombre
        b.tipoDocumento = tipoDocumento
        b.numeroDocumento = numeroDocumento
        b.identidadVerificada = verificada
        publicar(b)
    }

    fun seleccionarTipoReceptor(tipoReceptor: String) {
        val b = borradorActual() ?: return
        if (b.tipoReceptor == tipoReceptor) return
        b.tipoReceptor = tipoReceptor
        if (!b.esTercero()) {
            b.autorizacionRemitente = false
            b.recepcionMismoPredio = false
        }
        publicar(b)
    }

    fun actualizarAutorizacion(autorizacionComprobada: Boolean, mismoPredio: Boolean) {
        val b = borradorActual() ?: return
        b.autorizacionRemitente = autorizacionComprobada
        b.recepcionMismoPredio = mismoPredio
        publicar(b)
    }

    /** Cambiar el medio invalida las verificaciones anteriores (DEC-SEQ-01/7). */
    fun seleccionarMedioPago(medio: String) {
        val b = borradorActual() ?: return
        if (b.medioPago == medio) return
        b.medioPago = medio
        b.importeExactoVerificado = false
        b.operacionObservadaEnVivo = false
        b.codigoOperacion = ""
        b.confirmacionDespacho = false
        publicar(b)
    }

    fun confirmarImporteExacto(verificado: Boolean) {
        val b = borradorActual() ?: return
        b.importeExactoVerificado = verificado
        publicar(b)
    }

    /** Un código distinto corresponde a otra operación: la confirmación anterior deja de valer. */
    fun actualizarCodigoOperacion(codigo: String) {
        val b = borradorActual() ?: return
        if (b.codigoOperacion == codigo) return
        b.codigoOperacion = codigo
        b.confirmacionDespacho = false
        publicar(b)
    }

    fun actualizarVerificacionDigital(observadaEnVivo: Boolean, confirmacionDespacho: Boolean) {
        val b = borradorActual() ?: return
        b.operacionObservadaEnVivo = observadaEnVivo
        b.confirmacionDespacho = confirmacionDespacho
        publicar(b)
    }

    fun asociarFirma(firma: Bitmap) {
        val b = borradorActual() ?: return
        b.firma = firma
        publicar(b)
    }

    /** Un resultado null (captura cancelada) conserva la fotografía anterior. */
    fun asociarFotografia(resultadoCaptura: Bitmap?) {
        val b = borradorActual() ?: return
        if (resultadoCaptura == null) return
        b.fotografiaFachada = resultadoCaptura
        publicar(b)
    }

    fun actualizarExcepcion(motivo: String?, nota: String) {
        val b = borradorActual() ?: return
        b.motivoExoneracion = motivo
        b.notaSeguridad = nota
        publicar(b)
    }

    /**
     * Evalúa el cobro contra el importe exacto y los medios autorizados de la parada
     * (RF-23 a RF-25; SEQ-09). Datos ausentes no verifican el pago.
     */
    fun verificarCobro(parada: Parada): Boolean {
        if (!parada.esContraentrega()) return true
        val b = _borrador.value ?: return false
        val medio = b.medioPago ?: return false
        if (medio !in parada.mediosPagoAutorizados) return false
        val importe = b.importeCobradoSoles ?: return false
        if (importe.compareTo(parada.importeContraentregaSoles) != 0) return false
        return if (medio == Recepcion.EFECTIVO) {
            b.importeExactoVerificado
        } else {
            b.codigoOperacion.isNotBlank() && b.operacionObservadaEnVivo && b.confirmacionDespacho
        }
    }

    fun identidadCompleta(): Boolean {
        val b = _borrador.value ?: return false
        return b.nombreReceptor.isNotBlank() &&
            b.numeroDocumento.isNotBlank() &&
            b.tipoDocumento in Recepcion.TIPOS_DOCUMENTO &&
            b.identidadVerificada
    }

    /** El tercero solo es válido si la parada lo admite y el repartidor comprobó la autorización (RF-11). */
    fun autorizacionCompleta(parada: Parada): Boolean {
        val b = _borrador.value ?: return false
        if (!b.esTercero()) return true
        return parada.admiteTercero && b.autorizacionRemitente && b.recepcionMismoPredio
    }

    /** RF-15 y SEQ-05: la exoneración de foto aplica a la recepción por el titular en un sector de alto riesgo. */
    fun aplicaExoneracion(parada: Parada): Boolean {
        val b = _borrador.value ?: return false
        return parada.esZonaAltoRiesgo() && !b.esTercero()
    }

    /** Con exoneración, la foto se sustituye por el motivo tipificado; sin ella, la foto es obligatoria. */
    fun fotografiaCompleta(parada: Parada): Boolean {
        val b = _borrador.value ?: return false
        return if (aplicaExoneracion(parada)) b.motivoExoneracion != null else b.fotografiaFachada != null
    }

    /** Datos obligatorios del formulario, sin contar fecha y ubicación. */
    fun datosCompletos(parada: Parada): Boolean {
        val b = _borrador.value ?: return false
        return identidadCompleta() &&
            autorizacionCompleta(parada) &&
            b.firma != null &&
            fotografiaCompleta(parada) &&
            verificarCobro(parada)
    }

    fun puedeGuardar(parada: Parada): Boolean =
        datosCompletos(parada) && _borrador.value?.fechaHora != null

    /**
     * Guarda la recepción con su cobro y genera la constancia (RF-12 a RF-14, RF-24, RF-26 a RF-28).
     * Devuelve null si los datos no son válidos; el borrador se conserva intacto.
     */
    fun guardarRecepcion(parada: Parada): Recepcion? {
        val existente = recepcionesGuardadas[parada.idPedido]
        if (existente != null) {
            Log.w(TAG, "El pedido ${parada.idPedido} ya tiene recepción registrada; no se duplica")
            return existente
        }
        if (!parada.estado.estaPorAtender() || !puedeGuardar(parada)) {
            Log.w(TAG, "Recepción de ${parada.idPedido} no válida; se conserva el borrador")
            return null
        }
        val guardada = _borrador.value!!.copy(
            idPedido = parada.idPedido,
            idJornada = parada.idJornada,
            resultadoEntrega = if (_borrador.value!!.esTercero()) Recepcion.ENTREGADO_TERCERO else Recepcion.ENTREGADO_TITULAR
        )
        // La constancia conserva la foto o, con exoneración, el sector y el motivo (RF-14).
        if (aplicaExoneracion(parada)) {
            guardada.fotografiaFachada = null
        } else {
            guardada.sectorRiesgo = null
            guardada.motivoExoneracion = null
            guardada.notaSeguridad = ""
        }
        if (parada.esContraentrega()) {
            guardada.aporteEfectivoSoles =
                if (guardada.medioPago == Recepcion.EFECTIVO) parada.importeContraentregaSoles else BigDecimal("0.00")
            recaudacionFisicaSoles = recaudacionFisicaSoles.add(guardada.aporteEfectivoSoles)
        } else {
            guardada.medioPago = null
            guardada.importeCobradoSoles = null
        }
        recepcionesGuardadas[parada.idPedido] = guardada
        Log.d(TAG, "Recepción guardada: ${guardada.resultadoEntrega} (${parada.idPedido}). Recaudación física: $recaudacionFisicaSoles")
        descartarBorrador()
        return guardada
    }

    fun obtenerRecepcionGuardada(idParada: String): Recepcion? =
        recepcionesGuardadas.values.firstOrNull { it.idParada == idParada }

    /** Cancelar o Atrás: elimina solo los datos no guardados (DEC-SEQ-01/4). */
    fun descartarBorrador() {
        _borrador.value = null
    }

    /**
     * SEQ-04: asocia fecha, hora y GPS de la visita al borrador (RF-10).
     * El Fragment la invoca cuando las evidencias y demás datos están completos.
     */
    fun completarFechaUbicacion() {
        val b = borradorActual() ?: return
        b.fechaHora = System.currentTimeMillis()
        b.latitud = UbicacionSimulada.LATITUD
        b.longitud = UbicacionSimulada.LONGITUD
        Log.d(TAG, "Fecha y ubicación asociadas a la recepción de ${b.idParada}")
        publicar(b)
    }

    private fun borradorActual(): Recepcion? = _borrador.value

    /** Notifica el cambio del borrador a la vista. */
    private fun publicar(borrador: Recepcion) {
        _borrador.value = borrador
    }

    companion object {
        private const val TAG = "RecepcionViewModel"
    }
}
