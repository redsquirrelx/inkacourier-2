package com.dapm.inkacourier_app.vistas

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dapm.inkacourier_app.MainActivity
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.databinding.FragmentRegistroRecepcionBinding
import com.dapm.inkacourier_app.modelo.EstadoParada
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.Recepcion
import com.google.android.material.snackbar.Snackbar

/**
 * Destino "registro_recepcion" (HU-03 y HU-04; SEQ-03, SEQ-04, SEQ-05, SEQ-09 a SEQ-11).
 * Los datos se conservan en el borrador de RecepcionViewModel mientras se captura la firma o la foto.
 */
class RegistroRecepcionFragment : Fragment() {

    private var _binding: FragmentRegistroRecepcionBinding? = null
    private val binding get() = _binding!!

    private val paradasViewModel: ParadasViewModel by activityViewModels()
    private val recepcionViewModel: RecepcionViewModel by activityViewModels()

    private val idParada: String get() = requireArguments().getString(ARG_ID_PARADA)!!
    private lateinit var parada: Parada

    /** Evita reenviar al controlador los cambios que la propia vista aplica al renderizar. */
    private var actualizandoVista = false

    /** Cámara externa (destino "camara"); null indica captura cancelada. */
    private val camara = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { resultadoCaptura ->
        recepcionViewModel.asociarFotografia(resultadoCaptura)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegistroRecepcionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parada = paradasViewModel.consultarParada(idParada)
        val borrador = recepcionViewModel.obtenerBorrador(parada)

        configurarBarra(binding.barra, getString(R.string.registrar_recepcion)) { cancelarRecepcion() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { cancelarRecepcion() }
        binding.txtPedido.text = getString(R.string.pedido, parada.idPedido)

        configurarCobro(borrador)
        configurarIdentidad(borrador)
        configurarAutorizacion(borrador)
        configurarEvidencias()
        configurarExcepcionFormulario(binding.excepcion, parada.sectorRiesgo, borrador.motivoExoneracion, borrador.notaSeguridad) { motivo, nota ->
            recepcionViewModel.actualizarExcepcion(motivo, nota)
        }

        binding.btnGuardarRecepcion.setOnClickListener { guardarRecepcion() }
        binding.btnCancelar.setOnClickListener { cancelarRecepcion() }

        recepcionViewModel.borrador.observe(viewLifecycleOwner) { actual ->
            if (actual != null) renderizar(actual)
        }
    }

    // ---- Configuración inicial de cada sección ----

    /** HU-04: sección condicional de cobro; solo se ofrecen los medios autorizados. */
    private fun configurarCobro(borrador: Recepcion) {
        val esContraentrega = parada.esContraentrega()
        binding.cardCobro.visibility = visibleSi(esContraentrega)
        binding.txtTipoPedido.text = getString(if (esContraentrega) R.string.pedido_contraentrega else R.string.pedido_sin_cobro)
        if (!esContraentrega) return

        binding.txtImporte.text = formatearSoles(parada.importeContraentregaSoles)
        val botonesPorMedio = mapOf(
            Recepcion.EFECTIVO to binding.btnEfectivo,
            Recepcion.YAPE to binding.btnYape,
            Recepcion.PLIN to binding.btnPlin
        )
        botonesPorMedio.forEach { (medio, boton) ->
            boton.visibility = visibleSi(medio in parada.mediosPagoAutorizados)
        }
        botonesPorMedio[borrador.medioPago]?.let { binding.grupoMedioPago.check(it.id) }
        binding.edtCodigoOperacion.setText(borrador.codigoOperacion)

        binding.grupoMedioPago.addOnButtonCheckedListener { _, idBoton, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val medio = botonesPorMedio.entries.first { it.value.id == idBoton }.key
            recepcionViewModel.seleccionarMedioPago(medio)
        }
        binding.chkImporteExacto.setOnCheckedChangeListener { _, marcado ->
            if (!actualizandoVista) recepcionViewModel.confirmarImporteExacto(marcado)
        }
        binding.edtCodigoOperacion.doAfterTextChanged { texto ->
            if (!actualizandoVista) recepcionViewModel.actualizarCodigoOperacion(texto.toString().trim())
        }
        binding.chkOperacionObservada.setOnCheckedChangeListener { _, _ -> enviarVerificacionDigital() }
        binding.chkConfirmacionDespacho.setOnCheckedChangeListener { _, _ -> enviarVerificacionDigital() }
    }

    /** RF-07: titular o tercero, con DNI o Carné de Extranjería verificado por el repartidor. */
    private fun configurarIdentidad(borrador: Recepcion) {
        binding.grupoTipoReceptor.check(if (borrador.esTercero()) R.id.btnTercero else R.id.btnTitular)
        binding.btnTercero.isEnabled = parada.admiteTercero
        binding.txtTerceroNoAdmitido.visibility = visibleSi(!parada.admiteTercero)
        binding.edtNombreReceptor.setText(borrador.nombreReceptor)
        binding.edtNumeroDocumento.setText(borrador.numeroDocumento)
        binding.chkIdentidadVerificada.isChecked = borrador.identidadVerificada

        binding.grupoTipoReceptor.addOnButtonCheckedListener { _, idBoton, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val tipo = if (idBoton == R.id.btnTercero) Recepcion.TERCERO_AUTORIZADO else Recepcion.TITULAR
            recepcionViewModel.seleccionarTipoReceptor(tipo)
        }
        configurarDesplegable(binding.selTipoDocumento, Recepcion.TIPOS_DOCUMENTO, borrador.tipoDocumento) { enviarIdentidad() }
        binding.edtNombreReceptor.doAfterTextChanged { enviarIdentidad() }
        binding.edtNumeroDocumento.doAfterTextChanged { enviarIdentidad() }
        binding.chkIdentidadVerificada.setOnCheckedChangeListener { _, _ -> enviarIdentidad() }
    }

    /** RF-11: autorización del remitente comprobada para la recepción por un tercero. */
    private fun configurarAutorizacion(borrador: Recepcion) {
        binding.chkAutorizacionComprobada.isChecked = borrador.autorizacionRemitente
        binding.chkRecepcionPermitida.isChecked = borrador.recepcionMismoPredio
        binding.chkAutorizacionComprobada.setOnCheckedChangeListener { _, _ -> enviarAutorizacion() }
        binding.chkRecepcionPermitida.setOnCheckedChangeListener { _, _ -> enviarAutorizacion() }
    }

    private fun configurarEvidencias() {
        binding.btnCapturarFirma.setOnClickListener { abrirFirma() }
        binding.btnTomarFotografia.setOnClickListener { abrirCamara() }
    }

    // ---- Envío de datos al controlador ----

    private fun enviarIdentidad() {
        recepcionViewModel.actualizarIdentidad(
            nombre = binding.edtNombreReceptor.text.toString().trim(),
            tipoDocumento = binding.selTipoDocumento.text.toString(),
            numeroDocumento = binding.edtNumeroDocumento.text.toString().trim(),
            verificada = binding.chkIdentidadVerificada.isChecked
        )
    }

    private fun enviarAutorizacion() {
        recepcionViewModel.actualizarAutorizacion(
            binding.chkAutorizacionComprobada.isChecked,
            binding.chkRecepcionPermitida.isChecked
        )
    }

    private fun enviarVerificacionDigital() {
        if (actualizandoVista) return
        recepcionViewModel.actualizarVerificacionDigital(
            binding.chkOperacionObservada.isChecked,
            binding.chkConfirmacionDespacho.isChecked
        )
    }

    // ---- Presentación del estado del borrador ----

    private fun renderizar(borrador: Recepcion) {
        actualizandoVista = true
        binding.cardAutorizacion.visibility = visibleSi(borrador.esTercero())
        renderizarCobro(borrador)
        renderizarEvidencias(borrador)
        mostrarFechaUbicacion(binding.fechaUbicacion, borrador.fechaHora, borrador.latitud, borrador.longitud)
        binding.btnGuardarRecepcion.isEnabled = recepcionViewModel.puedeGuardar(parada)
        actualizandoVista = false

        // SEQ-04: con las evidencias y demás datos completos se asocian fecha, hora y GPS.
        if (borrador.fechaHora == null && recepcionViewModel.datosCompletos(parada)) {
            recepcionViewModel.completarFechaUbicacion()
        }
    }

    private fun renderizarCobro(borrador: Recepcion) {
        if (!parada.esContraentrega()) return
        val esEfectivo = borrador.medioPago == Recepcion.EFECTIVO
        marcarMedioSeleccionado()
        binding.seccionEfectivo.visibility = visibleSi(esEfectivo)
        binding.seccionDigital.visibility = visibleSi(!esEfectivo)
        // El controlador reinicia las verificaciones al cambiar el medio o el código.
        sincronizar(binding.chkImporteExacto, borrador.importeExactoVerificado)
        sincronizar(binding.chkOperacionObservada, borrador.operacionObservadaEnVivo)
        sincronizar(binding.chkConfirmacionDespacho, borrador.confirmacionDespacho)
        if (binding.edtCodigoOperacion.text.toString().trim() != borrador.codigoOperacion) {
            binding.edtCodigoOperacion.setText(borrador.codigoOperacion)
        }

        if (borrador.confirmacionDespacho) {
            mostrarAviso(binding.avisoConfirmacion, Tono.EXITO,
                getString(R.string.abono_confirmado), getString(R.string.abono_no_suma))
        } else {
            mostrarAviso(binding.avisoConfirmacion, Tono.ADVERTENCIA,
                getString(R.string.confirmacion_pendiente), getString(R.string.confirmacion_pendiente_texto))
        }
    }

    private fun renderizarEvidencias(borrador: Recepcion) {
        val firma = borrador.firma
        binding.imgFirma.visibility = visibleSi(firma != null)
        binding.imgFirma.setImageBitmap(firma)
        mostrarEstadoEvidencia(binding.txtEstadoFirma, firma != null, R.string.capturada)

        // RF-15: con exoneración (titular en sector de alto riesgo) la fotografía no se exige.
        val exonerada = recepcionViewModel.aplicaExoneracion(parada)
        binding.seccionFotografia.visibility = visibleSi(!exonerada)
        binding.excepcion.root.visibility = visibleSi(exonerada)
        val foto = borrador.fotografiaFachada
        binding.imgFotografia.visibility = visibleSi(foto != null)
        binding.imgFotografia.setImageBitmap(foto)
        mostrarEstadoEvidencia(binding.txtEstadoFotografia, foto != null, R.string.registrada)
    }

    private fun mostrarEstadoEvidencia(insignia: TextView, completa: Boolean, textoCompleta: Int) {
        if (completa) {
            mostrarInsignia(insignia, getString(textoCompleta), Tono.EXITO)
        } else {
            mostrarInsignia(insignia, getString(R.string.pendiente), Tono.ADVERTENCIA)
        }
    }

    /** El medio de pago elegido muestra la marca ✓ del diseño (.segment[aria-pressed=true]). */
    private fun marcarMedioSeleccionado() {
        listOf(binding.btnEfectivo, binding.btnYape, binding.btnPlin).forEach { boton ->
            boton.icon = if (boton.isChecked) ContextCompat.getDrawable(requireContext(), R.drawable.ic_check) else null
        }
    }

    private fun sincronizar(casilla: CheckBox, valor: Boolean) {
        if (casilla.isChecked != valor) casilla.isChecked = valor
    }

    // ---- Navegación ----

    private fun abrirFirma() {
        FirmaReceptorDialog().show(childFragmentManager, FirmaReceptorDialog.ETIQUETA)
    }

    private fun abrirCamara() {
        try {
            camara.launch(null)
        } catch (error: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.sin_camara, Snackbar.LENGTH_LONG).show()
        }
    }

    /** Solo el guardado exitoso vuelve al detalle (evento recepcionGuardada; DEC-NAV-01/D3). */
    private fun guardarRecepcion() {
        val paradaVigente = paradasViewModel.consultarParada(idParada)
        val guardada = recepcionViewModel.guardarRecepcion(paradaVigente)
        if (guardada == null) {
            Snackbar.make(binding.root, R.string.registro_no_valido, Snackbar.LENGTH_LONG).show()
            return
        }
        paradasViewModel.aplicarResultadoPersistido(idParada, EstadoParada.ENTREGADA)
        (requireActivity() as MainActivity).volver()
    }

    /** Cancelar o Atrás descarta el borrador y vuelve al detalle sin confirmación (D18). */
    private fun cancelarRecepcion() {
        recepcionViewModel.descartarBorrador()
        (requireActivity() as MainActivity).volver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID_PARADA = "idParada"

        fun nuevaInstancia(idParada: String) = RegistroRecepcionFragment().apply {
            arguments = bundleOf(ARG_ID_PARADA to idParada)
        }
    }
}
