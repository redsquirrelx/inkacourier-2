package com.dapm.inkacourier_app.modelo

import android.graphics.Bitmap
import java.math.BigDecimal

/**
 * Recepción de un pedido (HU-03) y, si corresponde, su cobro contraentrega (HU-04).
 * Mientras se edita es un borrador; al guardarse respalda la constancia (RF-14).
 */
data class Recepcion(
    val idParada: String,
    var idPedido: String? = null,
    var idJornada: String? = null,

    // Identidad del receptor (RF-07)
    var tipoReceptor: String = TITULAR,
    var nombreReceptor: String = "",
    var tipoDocumento: String = DNI,
    var numeroDocumento: String = "",
    var identidadVerificada: Boolean = false,

    // Tercero autorizado (RF-11)
    var autorizacionRemitente: Boolean = false,
    var recepcionMismoPredio: Boolean = false,

    // Evidencias (RF-08, RF-09)
    var firma: Bitmap? = null,
    var fotografiaFachada: Bitmap? = null,

    // Excepción de seguridad (RF-15)
    var sectorRiesgo: String? = null,
    var motivoExoneracion: String? = null,
    var notaSeguridad: String = "",

    // Fecha y ubicación de la visita (RF-10)
    var fechaHora: Long? = null,
    var latitud: Double? = null,
    var longitud: Double? = null,

    var resultadoEntrega: String? = null,

    // Cobro contraentrega (RF-23 a RF-26)
    var medioPago: String? = null,
    var importeCobradoSoles: BigDecimal? = null,
    var importeExactoVerificado: Boolean = false,
    var operacionObservadaEnVivo: Boolean = false,
    var codigoOperacion: String = "",
    var confirmacionDespacho: Boolean = false,
    var aporteEfectivoSoles: BigDecimal? = null
) {
    fun esTercero(): Boolean = tipoReceptor == TERCERO_AUTORIZADO

    fun tieneCobro(): Boolean = medioPago != null

    companion object {
        const val TITULAR = "Titular"
        const val TERCERO_AUTORIZADO = "Tercero autorizado"

        const val DNI = "DNI"
        const val CARNE_EXTRANJERIA = "Carné de Extranjería"
        val TIPOS_DOCUMENTO = listOf(DNI, CARNE_EXTRANJERIA)

        const val EFECTIVO = "Efectivo"
        const val YAPE = "Yape"
        const val PLIN = "Plin"

        const val ENTREGADO_TITULAR = "Entregado al titular"
        const val ENTREGADO_TERCERO = "Entregado a tercero autorizado"

        /** Única muestra de motivo tipificado disponible en el diseño; el catálogo de Operaciones está pendiente. */
        val MOTIVOS_EXONERACION = listOf("Exoneración autorizada por seguridad")
    }
}
