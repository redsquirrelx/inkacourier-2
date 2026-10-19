package com.dapm.inkacourier_app.modelo

/**
 * Intento de entrega no concretado (HU-05, RF-16 a RF-22).
 * Es distinto de una Recepcion: el pedido permanece sin entregar.
 */
data class VisitaFallida(
    val idParada: String,
    var causa: String? = null,
    var observaciones: String = "",

    // Contexto de contacto para destinatario ausente (RF-18)
    var llegadaDomicilio: Long? = null,
    var contactoDuranteVentana: Long? = null,
    var sinRespuesta: Boolean? = null,
    var extensionAutorizada: Boolean? = null,

    // Recepción rechazada (RF-20)
    var motivoRechazo: String? = null,
    var constanciasRechazo: String = "",

    // Excepción de seguridad (RF-22)
    var sectorRiesgo: String? = null,
    var motivoExoneracion: String? = null,
    var notaSeguridad: String = "",

    // Fecha y ubicación reales del intento (RF-17)
    var fechaHora: Long? = null,
    var latitud: Double? = null,
    var longitud: Double? = null
) {
    companion object {
        const val DESTINATARIO_AUSENTE = "Destinatario ausente"
        const val DIRECCION_NO_LOCALIZADA = "Dirección no localizada"
        const val RECEPCION_RECHAZADA = "Recepción rechazada"
        const val FALTA_DE_PAGO = "Falta de pago / transacción no verificada"

        /** Ejemplos de motivo documentados en HU-05/CA-03; no constituyen un catálogo exhaustivo. */
        val MOTIVOS_RECHAZO = listOf("Pedido duplicado", "Producto no solicitado")
    }
}
