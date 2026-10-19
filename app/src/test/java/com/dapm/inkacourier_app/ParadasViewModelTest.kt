package com.dapm.inkacourier_app

import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.modelo.EstadoParada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/** HU-01: consulta de paradas. */
class ParadasViewModelTest {

    @get:Rule
    val ejecucionInmediata = EjecucionInmediataRule()

    private lateinit var viewModel: ParadasViewModel

    @Before
    fun preparar() {
        viewModel = ParadasViewModel()
    }

    @Test
    fun ca01_paradaConDomicilioContactoYVentanaHoraria() {
        val parada = viewModel.consultarParada("P01")
        assertEquals("Av. Los Olivos 245", parada.domicilio)
        assertEquals("900 000 245", parada.contacto)
        assertEquals("09:00 – 12:00", parada.ventanaHoraria)
    }

    @Test
    fun ca02_referenciasDeAccesoSeparadasDelDomicilio() {
        val parada = viewModel.consultarParada("P01")
        assertEquals(3, parada.referenciasAcceso.size)
        assertFalse(parada.referenciasAcceso.contains(parada.domicilio))
    }

    @Test
    fun ca03_contraentregaConImporteExactoYMediosAutorizados() {
        val parada = viewModel.consultarParada("P02")
        assertTrue(parada.esContraentrega())
        assertEquals(BigDecimal("128.50"), parada.importeContraentregaSoles)
        assertEquals(listOf("Efectivo", "Yape", "Plin"), parada.mediosPagoAutorizados)

        val sinCobro = viewModel.consultarParada("P01")
        assertNull(sinCobro.importeContraentregaSoles)
    }

    @Test
    fun ca04_laRutaContieneLosCuatroEstadosOperativos() {
        val estados = viewModel.consultarParadas().map { it.estado }.toSet()
        assertEquals(EstadoParada.values().toSet(), estados)
        assertTrue(EstadoParada.PENDIENTE.estaPorAtender())
        assertTrue(EstadoParada.EN_CAMINO.estaPorAtender())
        assertFalse(EstadoParada.ENTREGADA.estaPorAtender())
        assertFalse(EstadoParada.FALLIDA.estaPorAtender())
    }

    @Test
    fun ca05_sinConexionConservaLosDatosYIndicaLaUltimaActualizacion() {
        val antes = viewModel.consultarParadas()
        viewModel.actualizarConexion(hayConexion = false)

        assertTrue(viewModel.estaSinConexion())
        assertEquals(antes, viewModel.consultarParadas())
        assertEquals("16 oct., 08:30", viewModel.ultimaActualizacion)

        viewModel.actualizarConexion(hayConexion = true)
        assertFalse(viewModel.estaSinConexion())
    }

    @Test
    fun aplicarResultadoPersistidoPublicaUnaListaNuevaConElEstado() {
        val antes = viewModel.consultarParadas()
        viewModel.aplicarResultadoPersistido("P01", EstadoParada.ENTREGADA)
        val despues = viewModel.consultarParadas()

        assertNotSame(antes, despues)
        assertEquals(EstadoParada.ENTREGADA, viewModel.consultarParada("P01").estado)
        assertEquals(EstadoParada.PENDIENTE, antes.first { it.idParada == "P01" }.estado)

        viewModel.aplicarResultadoPersistido("P01", EstadoParada.ENTREGADA)
        assertEquals(EstadoParada.ENTREGADA, viewModel.consultarParada("P01").estado)
    }
}
