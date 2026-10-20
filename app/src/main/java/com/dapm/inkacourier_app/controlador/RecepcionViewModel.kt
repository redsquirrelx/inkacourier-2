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
 * las recepciones guardadas para el detalle y la constancia. Solo opera sobre paradas
 * propias del repartidor actual, resueltas en la fuente central de ParadasViewModel.
 */
class RecepcionViewModel(private val paradasViewModel: ParadasViewModel) : ViewModel() {

    /** Recepciones guardadas por idPedido: impide registrar dos veces el mismo pedido (RF-27). */
    private val recepcionesGuardadas = mutableMapOf<String, Recepcion>()

    /** Recaudación física de la jornada: solo cobros en efectivo (RF-26). */
    var recaudacionFisicaSoles: BigDecimal = BigDecimal("0.00")
        private set

    private val _borrador = MutableLiveData<Recepcion?>()
    val borrador: LiveData<Recepcion?> = _borrador

    /**
     * Crea el borrador de la parada o recupera el existente al volver de la firma o la cámara.
     * Rechaza con IllegalStateException una parada ajena o sin sesión, sin leer el borrador.
     */
    fun obtenerBorrador(parada: Parada): Recepcion {
        val vigente = paradasViewModel.consultarParada(parada.idParada)
        val actual = borradorActual()
        if (actual != null && actual.idParada == vigente.idParada) return actual

        return crearBorrador(vigente)
    }

    private fun crearBorrador(parada: Parada): Recepcion {
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

    /** Al cambiar el medio de pago hay que verificarlo de nuevo. */
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

    /** Una firma nueva invalida la fecha y el GPS anteriores; la vista los vuelve a asociar. */
    fun asociarFirma(firma: Bitmap) {
        val b = borradorActual() ?: return
        b.firma = firma
        b.fechaHora = null
        b.latitud = null
        b.longitud = null
        publicar(b)
    }

    /**
     * Un resultado null (captura cancelada) conserva la fotografía, la fecha y el GPS anteriores.
     * Una foto nueva invalida la fecha y el GPS; la vista los vuelve a asociar.
     */
    fun asociarFotografia(resultadoCaptura: Bitmap?) {
        val b = borradorActual() ?: return
        if (resultadoCaptura == null) return
        b.fotografiaFachada = resultadoCaptura
        b.fechaHora = null
        b.latitud = null
        b.longitud = null
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
     * (RF-23 a RF-25). Datos ausentes no verifican el pago.
     */
    fun verificarCobro(parada: Parada): Boolean {
        if (!parada.esContraentrega()) return true
        val b = borradorActual() ?: return false
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
        val b = borradorActual() ?: return false
        return b.nombreReceptor.isNotBlank() &&
            b.numeroDocumento.isNotBlank() &&
            b.tipoDocumento in Recepcion.TIPOS_DOCUMENTO &&
            b.identidadVerificada
    }

    /** El tercero solo es válido si la parada lo admite y el repartidor comprobó la autorización (RF-11). */
    fun autorizacionCompleta(parada: Parada): Boolean {
        val b = borradorActual() ?: return false
        if (!b.esTercero()) return true
        return parada.admiteTercero && b.autorizacionRemitente && b.recepcionMismoPredio
    }

    /** RF-15: la exoneración de foto aplica a la recepción por el titular en un sector de alto riesgo. */
    fun aplicaExoneracion(parada: Parada): Boolean {
        val b = borradorActual() ?: return false
        return parada.esZonaAltoRiesgo() && !b.esTercero()
    }

    /** Con exoneración, la foto se sustituye por el motivo tipificado; sin ella, la foto es obligatoria. */
    fun fotografiaCompleta(parada: Parada): Boolean {
        val b = borradorActual() ?: return false
        return if (aplicaExoneracion(parada)) b.motivoExoneracion != null else b.fotografiaFachada != null
    }

    /** Datos obligatorios del formulario, sin contar fecha y ubicación. */
    fun datosCompletos(parada: Parada): Boolean {
        val b = borradorActual() ?: return false
        return identidadCompleta() &&
            autorizacionCompleta(parada) &&
            b.firma != null &&
            fotografiaCompleta(parada) &&
            verificarCobro(parada)
    }

    fun puedeGuardar(parada: Parada): Boolean =
        datosCompletos(parada) && borradorActual()?.fechaHora != null

    /**
     * Guarda la recepción con su cobro y genera la constancia (RF-12 a RF-14, RF-24, RF-26 a RF-28).
     * Devuelve null si los datos no son válidos o contradicen la recepción ya registrada;
     * el borrador se conserva intacto.
     */
    fun guardarRecepcion(parada: Parada): Recepcion? {
        // El propietario se revalida en la fuente central, no con la Parada enviada por la vista.
        val vigente = paradasViewModel.buscarParadaPropia(parada.idParada)
        if (vigente == null) {
            Log.w(TAG, "Parada ${parada.idParada} no asignada al repartidor actual; no se guarda")
            return null
        }
        val b = borradorActual()
        val existente = recepcionesGuardadas[vigente.idPedido]
        if (existente != null) {
            // Un reenvío con los mismos datos devuelve el registro existente;
            // uno con datos incompatibles falla sin sobrescribirlo.
            val compatible = b == null || (
                b.idParada == existente.idParada &&
                    b.tipoReceptor == existente.tipoReceptor &&
                    b.nombreReceptor == existente.nombreReceptor &&
                    b.tipoDocumento == existente.tipoDocumento &&
                    b.numeroDocumento == existente.numeroDocumento &&
                    (!vigente.esContraentrega() ||
                        (b.medioPago == existente.medioPago && b.codigoOperacion == existente.codigoOperacion))
                )
            if (!compatible) {
                Log.w(TAG, "Conflicto: el pedido ${vigente.idPedido} ya tiene otra recepción registrada; no se sobrescribe")
                return null
            }
            Log.w(TAG, "El pedido ${vigente.idPedido} ya tiene recepción registrada; no se duplica")
            return existente
        }
        if (b == null || b.idParada != vigente.idParada || !vigente.estado.estaPorAtender() || !puedeGuardar(vigente)) {
            Log.w(TAG, "Recepción de ${vigente.idPedido} no válida; se conserva el borrador")
            return null
        }
        val guardada = b.copy(
            idPedido = vigente.idPedido,
            idJornada = vigente.idJornada,
            resultadoEntrega = if (b.esTercero()) Recepcion.ENTREGADO_TERCERO else Recepcion.ENTREGADO_TITULAR
        )
        // La constancia conserva la foto o, con exoneración, el sector y el motivo (RF-14).
        if (aplicaExoneracion(vigente)) {
            guardada.fotografiaFachada = null
        } else {
            guardada.sectorRiesgo = null
            guardada.motivoExoneracion = null
            guardada.notaSeguridad = ""
        }
        if (vigente.esContraentrega()) {
            guardada.aporteEfectivoSoles =
                if (guardada.medioPago == Recepcion.EFECTIVO) vigente.importeContraentregaSoles else BigDecimal("0.00")
            recaudacionFisicaSoles = recaudacionFisicaSoles.add(guardada.aporteEfectivoSoles)
        } else {
            guardada.medioPago = null
            guardada.importeCobradoSoles = null
        }
        recepcionesGuardadas[vigente.idPedido] = guardada
        Log.d(TAG, "Recepción guardada: ${guardada.resultadoEntrega} (${vigente.idPedido}). Recaudación física: $recaudacionFisicaSoles")
        descartarBorrador()
        return guardada
    }

    /** Solo devuelve la constancia de una asignación propia del repartidor actual. */
    fun obtenerRecepcionGuardada(idParada: String): Recepcion? {
        if (paradasViewModel.buscarParadaPropia(idParada) == null) return null
        return recepcionesGuardadas.values.firstOrNull { it.idParada == idParada }
    }

    /** Cancelar o Atrás: elimina solo los datos no guardados del borrador propio. */
    fun descartarBorrador() {
        if (borradorActual() != null) _borrador.value = null
    }

    /**
     * Asocia fecha, hora y GPS de la visita al borrador (RF-10).
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

    /** Devuelve null si el borrador no pertenece al repartidor actual. */
    private fun borradorActual(): Recepcion? =
        _borrador.value?.takeIf { paradasViewModel.buscarParadaPropia(it.idParada) != null }

    private fun publicar(borrador: Recepcion) {
        _borrador.value = borrador
    }

    companion object {
        private const val TAG = "RecepcionViewModel"
    }
}
