package com.dapm.inkacourier_app

import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.modelo.Recepcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * HU-03 y HU-04: validaciones del borrador que no requieren imágenes.
 * El guardado completo con firma y fotografía se prueba en androidTest.
 */
class RecepcionViewModelTest {

    @get:Rule
    val ejecucionInmediata = EjecucionInmediataRule()

    private lateinit var paradas: ParadasViewModel
    private lateinit var viewModel: RecepcionViewModel

    @Before
    fun preparar() {
        paradas = ParadasViewModel()
        viewModel = RecepcionViewModel()
    }

    @Test
    fun hu03ca01_identidadExigeDocumentoValidoYVerificacion() {
        viewModel.obtenerBorrador(paradas.consultarParada("P01"))
        viewModel.actualizarIdentidad("María Rojas", Recepcion.DNI, "00000000", verificada = false)
        assertFalse(viewModel.identidadCompleta())

        viewModel.actualizarIdentidad("María Rojas", Recepcion.CARNE_EXTRANJERIA, "00000000", verificada = true)
        assertTrue(viewModel.identidadCompleta())

        viewModel.actualizarIdentidad("María Rojas", "Pasaporte", "00000000", verificada = true)
        assertFalse(viewModel.identidadCompleta())
    }

    @Test
    fun hu03ca02_terceroExigeAutorizacionYQueLaParadaLoAdmita() {
        val admite = paradas.consultarParada("P01")
        viewModel.obtenerBorrador(admite)
        viewModel.seleccionarTipoReceptor(Recepcion.TERCERO_AUTORIZADO)
        assertFalse(viewModel.autorizacionCompleta(admite))
        viewModel.actualizarAutorizacion(autorizacionComprobada = true, mismoPredio = true)
        assertTrue(viewModel.autorizacionCompleta(admite))

        viewModel.descartarBorrador()
        val noAdmite = paradas.consultarParada("P05")
        viewModel.obtenerBorrador(noAdmite)
        viewModel.seleccionarTipoReceptor(Recepcion.TERCERO_AUTORIZADO)
        viewModel.actualizarAutorizacion(autorizacionComprobada = true, mismoPredio = true)
        assertFalse(viewModel.autorizacionCompleta(noAdmite))
    }

    @Test
    fun hu03ca03_altoRiesgoSustituyeLaFotoPorElMotivoDeExoneracion() {
        val parada = paradas.consultarParada("P05")
        viewModel.obtenerBorrador(parada)
        assertFalse(viewModel.fotografiaCompleta(parada))
        viewModel.actualizarExcepcion("Exoneración autorizada por seguridad", "")
        assertTrue(viewModel.fotografiaCompleta(parada))
    }

    @Test
    fun hu03ca03_laExoneracionSoloAplicaAlTitular() {
        val parada = paradas.consultarParada("P05").copy(admiteTercero = true)
        viewModel.obtenerBorrador(parada)
        assertTrue(viewModel.aplicaExoneracion(parada))

        viewModel.seleccionarTipoReceptor(Recepcion.TERCERO_AUTORIZADO)
        viewModel.actualizarExcepcion("Exoneración autorizada por seguridad", "")
        assertFalse(viewModel.aplicaExoneracion(parada))
        assertFalse("Un tercero no está exonerado de la foto", viewModel.fotografiaCompleta(parada))
    }

    @Test
    fun hu04_elBorradorConservaElImporteExactoDelPedido() {
        val parada = paradas.consultarParada("P02")
        val borrador = viewModel.obtenerBorrador(parada)
        assertEquals(parada.importeContraentregaSoles, borrador.importeCobradoSoles)

        viewModel.confirmarImporteExacto(true)
        borrador.importeCobradoSoles = java.math.BigDecimal("100.00")
        assertFalse("Un importe distinto no es pago íntegro", viewModel.verificarCobro(parada))
    }

    @Test
    fun hu04ca01_efectivoExigeConfirmarElImporteExacto() {
        val parada = paradas.consultarParada("P02")
        val borrador = viewModel.obtenerBorrador(parada)
        assertEquals(Recepcion.EFECTIVO, borrador.medioPago)
        assertFalse(viewModel.verificarCobro(parada))

        viewModel.confirmarImporteExacto(true)
        assertTrue(viewModel.verificarCobro(parada))
    }

    @Test
    fun hu04ca02_pagoDigitalExigeCodigoObservacionYConfirmacionDeDespacho() {
        val parada = paradas.consultarParada("P02")
        viewModel.obtenerBorrador(parada)
        viewModel.seleccionarMedioPago(Recepcion.YAPE)
        viewModel.actualizarCodigoOperacion("OP-00128450")
        viewModel.actualizarVerificacionDigital(observadaEnVivo = true, confirmacionDespacho = false)
        assertFalse("Sin confirmación bancaria no se verifica el pago", viewModel.verificarCobro(parada))

        viewModel.actualizarVerificacionDigital(observadaEnVivo = true, confirmacionDespacho = true)
        assertTrue(viewModel.verificarCobro(parada))
    }

    @Test
    fun hu04ca02_cambiarCodigoOMedioInvalidaLaVerificacionAnterior() {
        val parada = paradas.consultarParada("P02")
        viewModel.obtenerBorrador(parada)
        viewModel.seleccionarMedioPago(Recepcion.PLIN)
        viewModel.actualizarCodigoOperacion("OP-1")
        viewModel.actualizarVerificacionDigital(observadaEnVivo = true, confirmacionDespacho = true)

        viewModel.actualizarCodigoOperacion("OP-2")
        assertFalse(viewModel.borrador.value!!.confirmacionDespacho)

        viewModel.actualizarVerificacionDigital(observadaEnVivo = true, confirmacionDespacho = true)
        viewModel.seleccionarMedioPago(Recepcion.EFECTIVO)
        assertFalse(viewModel.borrador.value!!.confirmacionDespacho)
        assertFalse(viewModel.verificarCobro(parada))
    }

    @Test
    fun hu04_medioNoAutorizadoNoVerificaElCobro() {
        val soloDigital = paradas.consultarParada("P08")
        viewModel.obtenerBorrador(soloDigital)
        viewModel.seleccionarMedioPago(Recepcion.EFECTIVO)
        viewModel.confirmarImporteExacto(true)
        assertFalse(viewModel.verificarCobro(soloDigital))
    }

    @Test
    fun completarFechaUbicacionAsociaFechaYGps() {
        viewModel.obtenerBorrador(paradas.consultarParada("P01"))
        assertNull(viewModel.borrador.value!!.fechaHora)
        viewModel.completarFechaUbicacion()
        assertTrue(viewModel.borrador.value!!.fechaHora != null)
        assertTrue(viewModel.borrador.value!!.latitud != null)
    }

    @Test
    fun sinFirmaNoSeGuardaYElBorradorSeConserva() {
        val parada = paradas.consultarParada("P01")
        viewModel.obtenerBorrador(parada)
        viewModel.actualizarIdentidad("María Rojas", Recepcion.DNI, "00000000", verificada = true)

        assertNull(viewModel.guardarRecepcion(parada))
        assertEquals("María Rojas", viewModel.borrador.value!!.nombreReceptor)
        assertNull(viewModel.obtenerRecepcionGuardada("P01"))
    }
}
