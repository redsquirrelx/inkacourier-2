package com.dapm.inkacourier_app.modelo

/**
 * Repartidor del catálogo simulado de SesionViewModel.
 * Sus paradas se vinculan mediante Parada.idRepartidor, no por el nombre visible.
 */
data class Repartidor(
    val idRepartidor: String,
    val nombre: String,
    val usuario: String,
    val contrasena: String
)
