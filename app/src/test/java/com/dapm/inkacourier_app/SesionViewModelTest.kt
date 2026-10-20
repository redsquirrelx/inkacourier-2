package com.dapm.inkacourier_app

import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.controlador.SesionViewModel
import com.dapm.inkacourier_app.controlador.VisitaFallidaViewModel
import com.dapm.inkacourier_app.modelo.EstadoParada
import com.dapm.inkacourier_app.modelo.VisitaFallida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/** Inicio de sesión simulado y acceso exclusivo a las asignaciones propias. */
class SesionViewModelTest {

    @get:Rule
    val ejecucionInmediata = EjecucionInmediataRule()

    private lateinit var sesion: SesionViewModel
    private lateinit var paradas: ParadasViewModel
    private lateinit var recepcion: RecepcionViewModel
    private lateinit var visita: VisitaFallidaViewModel

    /** Paradas de REP-01 tal como podría enviarlas una vista: la fuente las debe revalidar. */
    private val paradasDeLucho by lazy {
        ParadasViewModel(SesionViewModel().apply { iniciarSesion("lucho", "ruta01") }).consultarParadas()
    }

    @Before
    fun preparar() {
        sesion = SesionViewModel()
        paradas = ParadasViewModel(sesion)
        recepcion = RecepcionViewModel(paradas)
        visita = VisitaFallidaViewModel(paradas)
    }

    private fun idsDe(usuario: String, contrasena: String): Set<String> {
        val otraSesion = SesionViewModel()
        assertTrue(otraSesion.iniciarSesion(usuario, contrasena))
        return ParadasViewModel(otraSesion).consultarParadas().map { it.idParada }.toSet()
    }

    private fun assertRechaza(bloque: () -> Unit) {
        try {
            bloque()
            fail("Se esperaba IllegalStateException")
        } catch (esperado: IllegalStateException) {
        }
    }

    @Test
    fun accesoCorrecto_estableceAlRepartidorYSusParadasSonDistintas() {
        assertNull("Proceso nuevo sin sesión", sesion.repartidorActual)
        assertTrue(sesion.iniciarSesion("lucho", "ruta01"))
        assertEquals(SesionViewModel.ID_REPARTIDOR_LUCHO, sesion.repartidorActual!!.idRepartidor)

        val lucho = idsDe("lucho", "ruta01")
        val rosa = idsDe("rosa", "ruta02")
        assertEquals(setOf("P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08"), lucho)
        assertEquals(setOf("P09", "P10"), rosa)
        assertTrue("Sin IDs compartidos", lucho.intersect(rosa).isEmpty())
    }

    @Test
    fun credencialesIncorrectas_noAutentican() {
        listOf(
            "lucho" to "ruta02", "rosa" to "ruta01", "pedro" to "ruta01", "lucho" to "incorrecta",
            // Sin normalización: mayúsculas o espacios no coinciden.
            "Lucho" to "ruta01", " lucho" to "ruta01", "lucho" to "ruta01 "
        ).forEach { (usuario, contrasena) ->
            assertFalse("$usuario/$contrasena", sesion.iniciarSesion(usuario, contrasena))
            assertNull(sesion.repartidorActual)
        }
    }

    @Test
    fun camposVacios_noAutentican() {
        assertFalse(sesion.iniciarSesion("", "ruta01"))
        assertFalse(sesion.iniciarSesion("lucho", ""))
        assertFalse(sesion.iniciarSesion("", ""))
        assertNull(sesion.repartidorActual)
    }

    @Test
    fun sinSesion_noSeExponenParadasNiBorradores() {
        assertTrue(paradas.consultarParadas().isEmpty())
        assertTrue(paradas.paradas.value!!.isEmpty())
        assertNull(paradas.buscarParadaPropia("P01"))
        assertRechaza { paradas.consultarParada("P01") }
        assertRechaza { recepcion.obtenerBorrador(paradasDeLucho.first()) }
        assertRechaza { visita.obtenerBorrador(paradasDeLucho.first()) }
        assertNull(recepcion.borrador.value)
        assertNull(visita.borrador.value)
        assertEquals("", paradas.ultimaActualizacion)
    }

    @Test
    fun paradaAjena_seRechazaSinExponerDatosNiModificarEstado() {
        assertTrue(sesion.iniciarSesion("rosa", "ruta02"))
        val p01 = paradasDeLucho.first { it.idParada == "P01" }
        val p02 = paradasDeLucho.first { it.idParada == "P02" }

        assertRechaza { paradas.consultarParada("P01") }
        assertRechaza { paradas.consultarParada("NO-EXISTE") }
        assertRechaza { recepcion.obtenerBorrador(p02) }
        assertRechaza { visita.obtenerBorrador(p01) }
        assertNull(recepcion.borrador.value)
        assertNull(recepcion.guardarRecepcion(p02))
        assertNull(visita.guardarVisitaFallida(p01, System.currentTimeMillis()))
        assertNull(recepcion.obtenerRecepcionGuardada("P01"))
        assertNull(visita.obtenerVisitaGuardada("P01"))
        assertEquals(BigDecimal("0.00"), recepcion.recaudacionFisicaSoles)

        val antes = paradas.consultarParadas()
        paradas.aplicarResultadoPersistido("P01", EstadoParada.ENTREGADA)
        assertSame("No se publica ningún cambio", antes, paradas.paradas.value)
        assertTrue(paradas.consultarParadas().none { it.idParada == "P01" })
    }

    @Test
    fun paradasPropias_permitenLosFlujosExistentes() {
        assertTrue(sesion.iniciarSesion("rosa", "ruta02"))
        assertEquals("16 oct., 08:45", paradas.ultimaActualizacion)

        val p10 = paradas.consultarParada("P10")
        assertEquals(BigDecimal("45.00"), recepcion.obtenerBorrador(p10).importeCobradoSoles)
        recepcion.confirmarImporteExacto(true)
        assertTrue(recepcion.verificarCobro(p10))
        recepcion.descartarBorrador()

        val p09 = paradas.consultarParada("P09")
        visita.obtenerBorrador(p09)
        visita.seleccionarCausa(VisitaFallida.DIRECCION_NO_LOCALIZADA)
        visita.actualizarObservaciones("Edificio sin numeración visible.")
        val ahora = System.currentTimeMillis()
        visita.completarFechaUbicacion(ahora)
        val guardada = visita.guardarVisitaFallida(p09, ahora)!!
        paradas.aplicarResultadoPersistido("P09", EstadoParada.FALLIDA)

        assertSame(guardada, visita.obtenerVisitaGuardada("P09"))
        assertEquals(EstadoParada.FALLIDA, paradas.consultarParada("P09").estado)
    }

    @Test
    fun guardado_revalidaConLaFuenteCentralYNoConLaParadaDeLaVista() {
        assertTrue(sesion.iniciarSesion("lucho", "ruta01"))
        val p04 = paradas.consultarParada("P04")
        visita.obtenerBorrador(p04)
        visita.seleccionarCausa(VisitaFallida.DIRECCION_NO_LOCALIZADA)
        visita.actualizarObservaciones("Observación")
        visita.completarFechaUbicacion(System.currentTimeMillis())

        // La vista envía la parada fallida como si siguiera pendiente: la fuente central lo impide.
        val alterada = p04.copy(estado = EstadoParada.PENDIENTE)
        assertNull(visita.guardarVisitaFallida(alterada, System.currentTimeMillis()))
        assertNull(visita.obtenerVisitaGuardada("P04"))
    }
}
