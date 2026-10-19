package com.dapm.inkacourier_app.vistas

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dapm.inkacourier_app.MainActivity
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.VisitaFallidaViewModel
import com.dapm.inkacourier_app.databinding.FragmentRegistroVisitaFallidaBinding
import com.dapm.inkacourier_app.modelo.EstadoParada
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.VisitaFallida
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

/**
 * Destino "registro_visita_fallida" (HU-05; SEQ-06, SEQ-07, SEQ-08).
 * Registrar vuelve al detalle solo si el intento se guardó correctamente.
 */
class RegistroVisitaFallidaFragment : Fragment() {

    private var _binding: FragmentRegistroVisitaFallidaBinding? = null
    private val binding get() = _binding!!

    private val paradasViewModel: ParadasViewModel by activityViewModels()
    private val visitaFallidaViewModel: VisitaFallidaViewModel by activityViewModels()

    private val idParada: String get() = requireArguments().getString(ARG_ID_PARADA)!!
    private lateinit var parada: Parada

    // Contexto de contacto documentado en el formulario (HU-05/CA-01).
    private var llegada: Long? = null
    private var contacto: Long? = null
    private var sinRespuesta: Boolean? = null
    private var extensionAutorizada: Boolean? = null

    /** El plazo de 10 minutos avanza con el reloj: se reevalúa periódicamente. */
    private val reevaluacionPeriodica = object : Runnable {
        override fun run() {
            visitaFallidaViewModel.reevaluar()
            binding.root.postDelayed(this, INTERVALO_REEVALUACION_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistroVisitaFallidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parada = paradasViewModel.consultarParada(idParada)
        val borrador = visitaFallidaViewModel.obtenerBorrador(parada)

        configurarBarra(binding.barra, getString(R.string.titulo_visita_fallida)) { cancelarVisitaFallida() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { cancelarVisitaFallida() }
        binding.txtPedido.text = getString(R.string.pedido, parada.idPedido)

        configurarResultado(borrador)
        configurarContacto(borrador)
        configurarRechazo(borrador)
        configurarExcepcionFormulario(binding.excepcion, parada.sectorRiesgo, borrador.motivoExoneracion, borrador.notaSeguridad) { motivo, nota ->
            visitaFallidaViewModel.actualizarExcepcion(motivo, nota)
        }

        binding.btnRegistrar.setOnClickListener { registrarVisitaFallida() }
        binding.btnCancelar.setOnClickListener { cancelarVisitaFallida() }

        visitaFallidaViewModel.borrador.observe(viewLifecycleOwner) { actual ->
            if (actual != null) renderizar(actual)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.post(reevaluacionPeriodica)
    }

    override fun onPause() {
        super.onPause()
        binding.root.removeCallbacks(reevaluacionPeriodica)
    }

    // ---- Configuración inicial ----

    /** RF-16: causa específica; la falta de pago solo se ofrece en pedidos contraentrega. */
    private fun configurarResultado(borrador: VisitaFallida) {
        configurarDesplegable(binding.selCausa, visitaFallidaViewModel.causasDisponibles(parada), borrador.causa) { causa ->
            visitaFallidaViewModel.seleccionarCausa(causa)
        }
        binding.edtObservaciones.setText(borrador.observaciones)
        binding.edtObservaciones.doAfterTextChanged { texto ->
            visitaFallidaViewModel.actualizarObservaciones(texto.toString().trim())
        }
        mostrarAviso(binding.avisoPagoNoVerificado, Tono.ADVERTENCIA,
            getString(R.string.pago_no_verificado), getString(R.string.pago_no_verificado_texto))
    }

    /** RF-18: llegada, gestión de contacto, falta de respuesta y extensión de Despacho. */
    private fun configurarContacto(borrador: VisitaFallida) {
        llegada = borrador.llegadaDomicilio
        contacto = borrador.contactoDuranteVentana
        sinRespuesta = borrador.sinRespuesta
        extensionAutorizada = borrador.extensionAutorizada
        llegada?.let { binding.edtLlegada.setText(formatearHora(it)) }
        contacto?.let { binding.edtContacto.setText(formatearHora(it)) }

        binding.edtLlegada.setOnClickListener {
            elegirHora(llegada) { hora ->
                llegada = hora
                binding.edtLlegada.setText(formatearHora(hora))
                enviarContacto()
            }
        }
        binding.edtContacto.setOnClickListener {
            elegirHora(contacto) { hora ->
                contacto = hora
                binding.edtContacto.setText(formatearHora(hora))
                enviarContacto()
            }
        }

        val opciones = listOf(getString(R.string.si), getString(R.string.no))
        configurarDesplegable(binding.selSinRespuesta, opciones, textoSiNo(sinRespuesta)) { elegido ->
            sinRespuesta = elegido == getString(R.string.si)
            enviarContacto()
        }
        configurarDesplegable(binding.selExtension, opciones, textoSiNo(extensionAutorizada)) { elegido ->
            extensionAutorizada = elegido == getString(R.string.si)
            enviarContacto()
        }
    }

    /** RF-20: motivo comunicado por el receptor y constancias aplicables. */
    private fun configurarRechazo(borrador: VisitaFallida) {
        var motivo = borrador.motivoRechazo
        binding.edtConstancias.setText(borrador.constanciasRechazo)
        configurarDesplegable(binding.selMotivoRechazo, VisitaFallida.MOTIVOS_RECHAZO, motivo) { elegido ->
            motivo = elegido
            visitaFallidaViewModel.actualizarRechazo(motivo, binding.edtConstancias.text.toString().trim())
        }
        binding.edtConstancias.doAfterTextChanged { texto ->
            visitaFallidaViewModel.actualizarRechazo(motivo, texto.toString().trim())
        }
    }

    private fun enviarContacto() {
        visitaFallidaViewModel.actualizarContacto(llegada, contacto, sinRespuesta, extensionAutorizada)
    }

    /** Selector de hora del día actual; devuelve el instante en milisegundos. */
    private fun elegirHora(valorActual: Long?, alElegir: (Long) -> Unit) {
        val calendario = Calendar.getInstance()
        valorActual?.let { calendario.timeInMillis = it }
        TimePickerDialog(requireContext(), { _, hora, minuto ->
            val elegido = Calendar.getInstance()
            elegido.set(Calendar.HOUR_OF_DAY, hora)
            elegido.set(Calendar.MINUTE, minuto)
            elegido.set(Calendar.SECOND, 0)
            elegido.set(Calendar.MILLISECOND, 0)
            alElegir(elegido.timeInMillis)
        }, calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), true).show()
    }

    private fun textoSiNo(valor: Boolean?): String? = when (valor) {
        true -> getString(R.string.si)
        false -> getString(R.string.no)
        null -> null
    }

    // ---- Presentación del estado del borrador ----

    private fun renderizar(borrador: VisitaFallida) {
        val ahora = System.currentTimeMillis()
        val causa = borrador.causa
        binding.cardContacto.visibility = visibleSi(causa == VisitaFallida.DESTINATARIO_AUSENTE)
        binding.cardRechazo.visibility = visibleSi(causa == VisitaFallida.RECEPCION_RECHAZADA)
        binding.avisoPagoNoVerificado.root.visibility = visibleSi(causa == VisitaFallida.FALTA_DE_PAGO)
        binding.txtRespaldo.text = borrador.observaciones.ifBlank { getString(R.string.respaldo_vacio) }

        renderizarAvisosAusencia(borrador, ahora)
        mostrarFechaUbicacion(binding.fechaUbicacion, borrador.fechaHora, borrador.latitud, borrador.longitud)
        binding.btnRegistrar.isEnabled = visitaFallidaViewModel.puedeGuardar(parada, ahora)

        // SEQ-07: con la documentación exigida completa se asocian fecha, hora y GPS del intento.
        if (borrador.fechaHora == null && visitaFallidaViewModel.datosCompletos(parada, ahora)) {
            visitaFallidaViewModel.completarFechaUbicacion(ahora)
        }
    }

    /** HU-05/CA-01: mientras no se cumplan las condiciones no corresponde registrar ausencia. */
    private fun renderizarAvisosAusencia(borrador: VisitaFallida, ahora: Long) {
        val esAusencia = borrador.causa == VisitaFallida.DESTINATARIO_AUSENTE
        if (!esAusencia || visitaFallidaViewModel.puedeRegistrarAusencia(ahora)) {
            ocultarAviso(binding.avisoContactoEnCurso)
            ocultarAviso(binding.avisoAusenciaNoHabilitada)
            return
        }
        val minutos = visitaFallidaViewModel.minutosTranscurridos(ahora)
        if (minutos != null && minutos < VisitaFallidaViewModel.MINUTOS_ESPERA_AUSENCIA) {
            var texto = getString(R.string.minutos_transcurridos, minutos)
            if (borrador.sinRespuesta == true) texto += " " + getString(R.string.sin_respuesta_destinatario)
            mostrarAviso(binding.avisoContactoEnCurso, Tono.ADVERTENCIA, getString(R.string.contacto_en_curso), texto)
        } else {
            ocultarAviso(binding.avisoContactoEnCurso)
        }
        mostrarAviso(binding.avisoAusenciaNoHabilitada, Tono.INFO,
            getString(R.string.ausencia_no_habilitada), getString(R.string.ausencia_no_habilitada_texto))
    }

    // ---- Navegación ----

    private fun registrarVisitaFallida() {
        val paradaVigente = paradasViewModel.consultarParada(idParada)
        val guardada = visitaFallidaViewModel.guardarVisitaFallida(paradaVigente, System.currentTimeMillis())
        if (guardada == null) {
            Snackbar.make(binding.root, R.string.registro_no_valido, Snackbar.LENGTH_LONG).show()
            return
        }
        paradasViewModel.aplicarResultadoPersistido(idParada, EstadoParada.FALLIDA)
        (requireActivity() as MainActivity).volver()
    }

    /** Cancelar o Atrás descarta el borrador y vuelve al detalle sin confirmación (D18). */
    private fun cancelarVisitaFallida() {
        visitaFallidaViewModel.descartarBorrador()
        (requireActivity() as MainActivity).volver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID_PARADA = "idParada"
        private const val INTERVALO_REEVALUACION_MS = 30_000L

        fun nuevaInstancia(idParada: String) = RegistroVisitaFallidaFragment().apply {
            arguments = bundleOf(ARG_ID_PARADA to idParada)
        }
    }
}
