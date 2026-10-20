package com.dapm.inkacourier_app.modelo

import java.math.BigDecimal

/**
 * Parada asignada al repartidor (HU-01, RF-01 a RF-04).
 * Los datos provienen de la ruta simulada definida en ParadasViewModel.
 */
data class Parada(
    val idParada: String,
    val idPedido: String,
    val idJornada: String,
    /** Propietario de la asignación (Repartidor.idRepartidor). */
    val idRepartidor: String,
    val destinatario: String,
    val domicilio: String,
    val distrito: String,
    val contacto: String,
    val ventanaHoraria: String,
    val referenciasAcceso: List<String> = emptyList(),
    /** Importe exacto en soles; null si la parada no es contraentrega. */
    val importeContraentregaSoles: BigDecimal? = null,
    val mediosPagoAutorizados: List<String> = emptyList(),
    /** El remitente autorizó la recepción por un tercero en el mismo predio (HU-03/CA-02, HU-04/CA-03). */
    val admiteTercero: Boolean = false,
    /** Sector catalogado por Operaciones como de alto riesgo, con exoneración de foto autorizada; null si no aplica. */
    val sectorRiesgo: String? = null,
    val estado: EstadoParada = EstadoParada.PENDIENTE
) {
    fun esContraentrega(): Boolean = importeContraentregaSoles != null

    fun esZonaAltoRiesgo(): Boolean = sectorRiesgo != null
}
