package com.dapm.inkacourier_app

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.SesionViewModel
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.modelo.EstadoParada
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.Recepcion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** HU-03 y HU-04: guardado completo con firma y fotografía reales (Bitmap). */
@RunWith(AndroidJUnit4::class)
class RecepcionGuardadoTest {

    private lateinit var sesion: SesionViewModel
    private lateinit var paradas: ParadasViewModel
    private lateinit var viewModel: RecepcionViewModel
    private val imagen = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

    /** LiveData exige el hilo principal. */
    private fun enHiloPrincipal(bloque: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(bloque)

    @Before
    fun preparar() = enHiloPrincipal {
        sesion = SesionViewModel().apply { iniciarSesion("lucho", "ruta01") }
        paradas = ParadasViewModel(sesion)
        viewModel = RecepcionViewModel(paradas)
    }

    private fun completarRecepcion(parada: Parada, nombre: String, tercero: Boolean = false) {
        viewModel.obtenerBorrador(parada)
        if (tercero) {
            viewModel.seleccionarTipoReceptor(Recepcion.TERCERO_AUTORIZADO)
            viewModel.actualizarAutorizacion(autorizacionComprobada = true, mismoPredio = true)
        }
        viewModel.actualizarIdentidad(nombre, Recepcion.DNI, "00000000", verificada = true)
        viewModel.asociarFirma(imagen)
        if (parada.esZonaAltoRiesgo()) {
            viewModel.actualizarExcepcion(Recepcion.MOTIVOS_EXONERACION.first(), "Nota de seguridad")
        } else {
            viewModel.asociarFotografia(imagen)
        }
        viewModel.completarFechaUbicacion()
    }

    @Test
    fun hu03ca01_entregaAlTitularGeneraConstancia() = enHiloPrincipal {
        val parada = paradas.consultarParada("P01")
        completarRecepcion(parada, "María Rojas")

        val recepcion = viewModel.guardarRecepcion(parada)!!
        assertEquals(Recepcion.ENTREGADO_TITULAR, recepcion.resultadoEntrega)
        assertNotNull(recepcion.firma)
        assertNotNull(recepcion.fotografiaFachada)
        assertNotNull(recepcion.fechaHora)
        assertNotNull(recepcion.latitud)
        assertNull("Pedido sin cobro", recepcion.medioPago)
        assertNull(recepcion.importeCobradoSoles)
        assertNull("Sin exoneración no se conserva sector", recepcion.sectorRiesgo)
        assertSame(recepcion, viewModel.obtenerRecepcionGuardada("P01"))
    }

    @Test
    fun hu03ca02_entregaATerceroAutorizado() = enHiloPrincipal {
        val parada = paradas.consultarParada("P01")
        completarRecepcion(parada, "Carlos Rojas", tercero = true)

        val recepcion = viewModel.guardarRecepcion(parada)!!
        assertEquals(Recepcion.ENTREGADO_TERCERO, recepcion.resultadoEntrega)
        assertEquals(true, recepcion.autorizacionRemitente)
        assertEquals(true, recepcion.recepcionMismoPredio)
    }

    @Test
    fun hu03ca03_altoRiesgoSinFotoConservaSectorYMotivo() = enHiloPrincipal {
        val parada = paradas.consultarParada("P05")
        completarRecepcion(parada, "Rosa Quispe")

        val recepcion = viewModel.guardarRecepcion(parada)!!
        assertEquals(Recepcion.ENTREGADO_TITULAR, recepcion.resultadoEntrega)
        assertNull(recepcion.fotografiaFachada)
        assertEquals("Sector de riesgo registrado", recepcion.sectorRiesgo)
        assertEquals(Recepcion.MOTIVOS_EXONERACION.first(), recepcion.motivoExoneracion)
        assertNotNull(recepcion.fechaHora)
    }

    @Test
    fun hu04ca01_efectivoSeSumaUnaSolaVezALaRecaudacionFisica() = enHiloPrincipal {
        val parada = paradas.consultarParada("P02")
        completarRecepcion(parada, "Diego Torres")
        viewModel.confirmarImporteExacto(true)

        val recepcion = viewModel.guardarRecepcion(parada)!!
        assertEquals(Recepcion.EFECTIVO, recepcion.medioPago)
        assertEquals(BigDecimal("128.50"), recepcion.importeCobradoSoles)
        assertEquals(BigDecimal("128.50"), recepcion.aporteEfectivoSoles)
        assertEquals(BigDecimal("128.50"), viewModel.recaudacionFisicaSoles)

        // RF-27: reenviar el mismo pedido no duplica el cobro ni la recaudación.
        completarRecepcion(parada, "Diego Torres")
        viewModel.confirmarImporteExacto(true)
        assertSame(recepcion, viewModel.guardarRecepcion(parada))
        assertEquals(BigDecimal("128.50"), viewModel.recaudacionFisicaSoles)

        // Un reenvío con datos incompatibles falla sin sobrescribir el registro.
        viewModel.actualizarIdentidad("Otra Persona", Recepcion.DNI, "11111111", verificada = true)
        assertNull(viewModel.guardarRecepcion(parada))
        assertSame(recepcion, viewModel.obtenerRecepcionGuardada("P02"))
        assertEquals("Diego Torres", recepcion.nombreReceptor)
        assertEquals("El borrador se conserva", "Otra Persona", viewModel.borrador.value!!.nombreReceptor)
        assertEquals(BigDecimal("128.50"), viewModel.recaudacionFisicaSoles)
    }

    @Test
    fun hu03_cambiarUnaEvidenciaInvalidaFechaYGps() = enHiloPrincipal {
        val parada = paradas.consultarParada("P01")
        completarRecepcion(parada, "María Rojas")
        assertNotNull(viewModel.borrador.value!!.fechaHora)

        viewModel.asociarFotografia(null)
        assertNotNull("Cancelar la cámara conserva fecha y GPS", viewModel.borrador.value!!.fechaHora)

        viewModel.asociarFotografia(imagen)
        assertNull(viewModel.borrador.value!!.fechaHora)
        assertNull(viewModel.borrador.value!!.latitud)
        assertNull("Sin fecha ni GPS no se guarda", viewModel.guardarRecepcion(parada))

        viewModel.completarFechaUbicacion()
        viewModel.asociarFirma(imagen)
        assertNull(viewModel.borrador.value!!.fechaHora)
        assertNull(viewModel.borrador.value!!.longitud)

        viewModel.completarFechaUbicacion()
        assertNotNull(viewModel.guardarRecepcion(parada))
    }

    @Test
    fun hu04ca02_pagoDigitalConfirmadoNoSeSumaAlEfectivo() = enHiloPrincipal {
        val parada = paradas.consultarParada("P02")
        completarRecepcion(parada, "Diego Torres")
        viewModel.seleccionarMedioPago(Recepcion.YAPE)
        viewModel.actualizarCodigoOperacion("OP-00128450")
        viewModel.actualizarVerificacionDigital(observadaEnVivo = true, confirmacionDespacho = false)
        assertNull("Sin confirmación de Despacho no se registra", viewModel.guardarRecepcion(parada))

        viewModel.actualizarVerificacionDigital(observadaEnVivo = true, confirmacionDespacho = true)
        val recepcion = viewModel.guardarRecepcion(parada)!!
        assertEquals(Recepcion.YAPE, recepcion.medioPago)
        assertEquals("OP-00128450", recepcion.codigoOperacion)
        assertEquals(true, recepcion.confirmacionDespacho)
        assertEquals(BigDecimal("0.00"), recepcion.aporteEfectivoSoles)
        assertEquals(BigDecimal("0.00"), viewModel.recaudacionFisicaSoles)
    }

    @Test
    fun hu04ca03_contraentregaATerceroVinculaAutorizacionYCobro() = enHiloPrincipal {
        val parada = paradas.consultarParada("P07")
        completarRecepcion(parada, "Carlos Flores", tercero = true)
        viewModel.confirmarImporteExacto(true)

        val recepcion = viewModel.guardarRecepcion(parada)!!
        assertEquals(Recepcion.ENTREGADO_TERCERO, recepcion.resultadoEntrega)
        assertEquals(true, recepcion.autorizacionRemitente)
        assertEquals(BigDecimal("64.90"), recepcion.importeCobradoSoles)
        assertEquals(Recepcion.EFECTIVO, recepcion.medioPago)
    }

    @Test
    fun aislamiento_borradorAjenoNoSeGuardaNiModificaLaRecaudacion() = enHiloPrincipal {
        val parada = paradas.consultarParada("P02")
        completarRecepcion(parada, "Diego Torres")
        viewModel.confirmarImporteExacto(true)

        // Con otro repartidor en el contexto compartido, el borrador de REP-01 es ajeno.
        assertEquals(true, sesion.iniciarSesion("rosa", "ruta02"))
        assertNull(viewModel.guardarRecepcion(parada))
        assertEquals(false, viewModel.puedeGuardar(parada))
        viewModel.asociarFirma(imagen)
        viewModel.descartarBorrador()
        assertNotNull("El descarte no afecta el borrador ajeno", viewModel.borrador.value)
        assertNull(viewModel.obtenerRecepcionGuardada("P02"))
        assertEquals(BigDecimal("0.00"), viewModel.recaudacionFisicaSoles)
        paradas.aplicarResultadoPersistido("P02", EstadoParada.ENTREGADA)

        assertEquals(true, sesion.iniciarSesion("lucho", "ruta01"))
        assertEquals("El estado ajeno no cambió", EstadoParada.EN_CAMINO, paradas.consultarParada("P02").estado)
        assertNotNull("El borrador propio sigue disponible para su dueño", viewModel.guardarRecepcion(parada))
    }

    @Test
    fun aislamiento_elGuardadoUsaLaParadaCentralYNoLaEnviadaPorLaVista() = enHiloPrincipal {
        val entregada = paradas.consultarParada("P03")
        completarRecepcion(entregada, "Ana Mendoza")

        val alterada = entregada.copy(estado = EstadoParada.PENDIENTE)
        assertNull(viewModel.guardarRecepcion(alterada))
        assertNull(viewModel.obtenerRecepcionGuardada("P03"))
    }
}
