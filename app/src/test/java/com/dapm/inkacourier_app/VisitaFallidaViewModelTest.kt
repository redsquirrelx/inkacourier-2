package com.dapm.inkacourier_app

import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.UbicacionSimulada
import com.dapm.inkacourier_app.controlador.VisitaFallidaViewModel
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.VisitaFallida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** HU-05: registrar el resultado de un intento de entrega. */
class VisitaFallidaViewModelTest {

    @get:Rule
    val ejecucionInmediata = EjecucionInmediataRule()

    private val minuto = VisitaFallidaViewModel.UN_MINUTO
    private lateinit var paradas: ParadasViewModel
    private lateinit var viewModel: VisitaFallidaViewModel

    /** Llegada al domicilio 30 minutos antes de la prueba. */
    private val llegada = System.currentTimeMillis() - 30 * VisitaFallidaViewModel.UN_MINUTO

    @Before
    fun preparar() {
        paradas = ParadasViewModel()
        viewModel = VisitaFallidaViewModel()
    }

    private fun iniciar(idParada: String): Parada {
        val parada = paradas.consultarParada(idParada)
        viewModel.obtenerBorrador(parada)
        return parada
    }

    private fun documentarAusencia(minutoContacto: Int, sinRespuesta: Boolean? = true, extension: Boolean? = false) {
        viewModel.seleccionarCausa(VisitaFallida.DESTINATARIO_AUSENTE)
        viewModel.actualizarObservaciones("Se intentó contactar al destinatario sin respuesta.")
        viewModel.actualizarContacto(llegada, llegada + minutoContacto * minuto, sinRespuesta, extension)
    }

    @Test
    fun ca01_contactoEnCursoNoPermiteRegistrarAntesDeDiezMinutos() {
        val parada = iniciar("P01")
        documentarAusencia(minutoContacto = 6)
        val ahora = llegada + 6 * minuto

        assertFalse(viewModel.puedeRegistrarAusencia(ahora))
        assertEquals(6L, viewModel.minutosTranscurridos(ahora))
        assertNull(viewModel.guardarVisitaFallida(parada, ahora))
        assertNotNull("El borrador se conserva intacto", viewModel.borrador.value)
    }

    @Test
    fun ca01_ausenciaDocumentadaSeRegistraALosDiezMinutos() {
        val parada = iniciar("P01")
        documentarAusencia(minutoContacto = 6)
        val ahora = llegada + 10 * minuto
        viewModel.completarFechaUbicacion(ahora)

        assertTrue(viewModel.puedeRegistrarAusencia(ahora))
        val visita = viewModel.guardarVisitaFallida(parada, ahora)!!
        assertEquals(VisitaFallida.DESTINATARIO_AUSENTE, visita.causa)
        assertNotNull("Fecha y hora asociadas al completar los datos", visita.fechaHora)
        assertEquals(UbicacionSimulada.LATITUD, visita.latitud!!, 0.0)
        assertEquals(visita, viewModel.obtenerVisitaGuardada("P01"))
        assertNull(viewModel.borrador.value)
    }

    @Test
    fun ca01_contactoFueraDeLaVentanaOExtensionImpidenRegistrar() {
        iniciar("P01")
        val ahora = llegada + 15 * minuto

        documentarAusencia(minutoContacto = 4)
        assertFalse(viewModel.puedeRegistrarAusencia(ahora))

        documentarAusencia(minutoContacto = 8)
        assertFalse(viewModel.puedeRegistrarAusencia(ahora))

        documentarAusencia(minutoContacto = 7, extension = true)
        assertFalse(viewModel.puedeRegistrarAusencia(ahora))

        documentarAusencia(minutoContacto = 5, sinRespuesta = null)
        assertFalse(viewModel.puedeRegistrarAusencia(ahora))

        documentarAusencia(minutoContacto = 5)
        assertTrue(viewModel.puedeRegistrarAusencia(ahora))
    }

    @Test
    fun ca02_direccionNoLocalizadaConservaObservacionesYGps() {
        val parada = iniciar("P06")
        viewModel.seleccionarCausa(VisitaFallida.DIRECCION_NO_LOCALIZADA)
        assertNull("Sin observaciones no hay datos completos", viewModel.borrador.value!!.fechaHora)

        viewModel.actualizarObservaciones("La numeración no es visible.")
        assertNull("Sin fecha y GPS no se registra", viewModel.guardarVisitaFallida(parada, System.currentTimeMillis()))
        viewModel.completarFechaUbicacion(System.currentTimeMillis())
        val visita = viewModel.guardarVisitaFallida(parada, System.currentTimeMillis())!!
        assertEquals("La numeración no es visible.", visita.observaciones)
        assertNotNull(visita.latitud)
        assertNotNull(visita.longitud)
    }

    @Test
    fun ca03_rechazoExigeYConservaElMotivo() {
        val parada = iniciar("P01")
        viewModel.seleccionarCausa(VisitaFallida.RECEPCION_RECHAZADA)
        viewModel.actualizarObservaciones("El receptor comunica que el pedido está duplicado.")
        viewModel.completarFechaUbicacion(System.currentTimeMillis())
        assertNull(viewModel.guardarVisitaFallida(parada, System.currentTimeMillis()))

        viewModel.actualizarRechazo("Pedido duplicado", "El receptor manifestó el motivo en el domicilio.")
        val visita = viewModel.guardarVisitaFallida(parada, System.currentTimeMillis())!!
        assertEquals("Pedido duplicado", visita.motivoRechazo)
        assertEquals(VisitaFallida.RECEPCION_RECHAZADA, visita.causa)
    }

    @Test
    fun ca04_faltaDePagoSoloSeOfreceEnPedidosContraentrega() {
        assertFalse(viewModel.causasDisponibles(paradas.consultarParada("P01")).contains(VisitaFallida.FALTA_DE_PAGO))
        assertTrue(viewModel.causasDisponibles(paradas.consultarParada("P02")).contains(VisitaFallida.FALTA_DE_PAGO))

        val parada = iniciar("P02")
        viewModel.seleccionarCausa(VisitaFallida.FALTA_DE_PAGO)
        viewModel.actualizarObservaciones("El cliente dispone de S/ 100.00; no se confirma el pago íntegro.")
        viewModel.completarFechaUbicacion(System.currentTimeMillis())
        val visita = viewModel.guardarVisitaFallida(parada, System.currentTimeMillis())!!
        assertEquals(VisitaFallida.FALTA_DE_PAGO, visita.causa)
    }

    @Test
    fun ca05_altoRiesgoExigeMotivoDeExoneracionYConservaLaCausaReal() {
        val parada = iniciar("P05")
        documentarAusencia(minutoContacto = 6)
        val ahora = llegada + 10 * minuto
        assertFalse(viewModel.datosCompletos(parada, ahora))

        viewModel.actualizarExcepcion("Exoneración autorizada por seguridad", "")
        viewModel.completarFechaUbicacion(ahora)
        val visita = viewModel.guardarVisitaFallida(parada, ahora)!!
        assertEquals(VisitaFallida.DESTINATARIO_AUSENTE, visita.causa)
        assertEquals("Sector de riesgo registrado", visita.sectorRiesgo)
        assertEquals("Exoneración autorizada por seguridad", visita.motivoExoneracion)
        assertNotNull(visita.fechaHora)
    }

    @Test
    fun descartarBorradorEliminaLosCambiosNoGuardados() {
        iniciar("P01")
        viewModel.actualizarObservaciones("Borrador")
        viewModel.descartarBorrador()

        val nuevo = viewModel.obtenerBorrador(paradas.consultarParada("P01"))
        assertEquals("", nuevo.observaciones)
        assertNull(viewModel.obtenerVisitaGuardada("P01"))
    }
}
