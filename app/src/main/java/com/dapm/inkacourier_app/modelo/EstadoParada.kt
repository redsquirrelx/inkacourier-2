package com.dapm.inkacourier_app.modelo

/** Estados operativos de una parada (HU-01/CA-04, RF-04). */
enum class EstadoParada(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    EN_CAMINO("En camino"),
    ENTREGADA("Entregada"),
    FALLIDA("Fallida");

    /** Pendiente y en camino son visitas por atender; las demás son resultados ya registrados. */
    fun estaPorAtender(): Boolean = this == PENDIENTE || this == EN_CAMINO
}
