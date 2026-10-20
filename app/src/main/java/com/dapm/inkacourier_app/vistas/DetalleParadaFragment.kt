package com.dapm.inkacourier_app.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dapm.inkacourier_app.MainActivity
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.controlador.VisitaFallidaViewModel
import com.dapm.inkacourier_app.databinding.FragmentDetalleParadaBinding
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.Recepcion
import com.dapm.inkacourier_app.modelo.VisitaFallida

/**
 * Pantalla de detalle de parada (HU-01/CA-01 a CA-03, CA-05). Tras un registro
 * muestra el resultado guardado: recepción y cobro (HU-03, HU-04) o visita fallida (HU-05).
 */
class DetalleParadaFragment : Fragment() {

    private var _binding: FragmentDetalleParadaBinding? = null
    private val binding get() = _binding!!

    private val paradasViewModel: ParadasViewModel by activityViewModels()
    private val recepcionViewModel: RecepcionViewModel by activityViewModels()
    private val visitaFallidaViewModel: VisitaFallidaViewModel by activityViewModels()

    private val idParada: String get() = requireArguments().getString(ARG_ID_PARADA)!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleParadaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val actividad = requireActivity() as MainActivity
        configurarBarra(binding.barra, getString(R.string.titulo_detalle)) { actividad.volver() }

        binding.btnRegistrarRecepcion.setOnClickListener {
            actividad.navegarA(RegistroRecepcionFragment.nuevaInstancia(idParada))
        }
        binding.btnRegistrarVisitaFallida.setOnClickListener {
            actividad.navegarA(RegistroVisitaFallidaFragment.nuevaInstancia(idParada))
        }
        binding.btnVerConstancia.setOnClickListener {
            actividad.navegarA(ConstanciaRecepcionFragment.nuevaInstancia(idParada))
        }

        paradasViewModel.paradas.observe(viewLifecycleOwner) {
            // Una parada ajena o sin sesión se rechaza antes de mostrar datos.
            val parada = actividad.conAccesoPropio { paradasViewModel.consultarParada(idParada) } ?: return@observe
            mostrarDetalle(parada)
        }
        paradasViewModel.sinConexion.observe(viewLifecycleOwner) { sinConexion ->
            if (sinConexion) mostrarAvisoUltimaActualizacion() else ocultarAviso(binding.avisoSinConexion)
        }
    }

    private fun mostrarDetalle(parada: Parada) {
        binding.txtPedido.text = getString(R.string.pedido, parada.idPedido)
        mostrarEstado(binding.txtEstado, parada.estado)

        mostrarDatoIcono(binding.datoDestinatario, R.drawable.ic_persona, parada.destinatario, getString(R.string.destinataria_titular))
        mostrarDatoIcono(binding.datoDomicilio, R.drawable.ic_ubicacion, parada.domicilio, parada.distrito)
        mostrarDatoIcono(binding.datoContacto, R.drawable.ic_telefono, parada.contacto, getString(R.string.contacto_registrado))
        mostrarDatoIcono(binding.datoVentana, R.drawable.ic_reloj, parada.ventanaHoraria, getString(R.string.ventana_atencion))

        // CA-02: las referencias se muestran aparte del domicilio registrado.
        binding.cardReferencias.visibility = visibleSi(parada.referenciasAcceso.isNotEmpty())
        binding.txtReferencias.text = parada.referenciasAcceso.joinToString(" ")

        // CA-03: importe exacto en soles y medios autorizados.
        binding.cardContraentrega.visibility = visibleSi(parada.esContraentrega())
        binding.txtImporte.text = formatearSoles(parada.importeContraentregaSoles)
        binding.filaMedios.txtEtiqueta.text = getString(R.string.medios_autorizados)
        binding.filaMedios.txtValor.text = parada.mediosPagoAutorizados.joinToString(" · ")

        val recepcion = recepcionViewModel.obtenerRecepcionGuardada(parada.idParada)
        val visita = visitaFallidaViewModel.obtenerVisitaGuardada(parada.idParada)
        mostrarResultado(recepcion, visita)

        val porAtender = parada.estado.estaPorAtender()
        binding.btnRegistrarRecepcion.visibility = visibleSi(porAtender)
        binding.btnRegistrarVisitaFallida.visibility = visibleSi(porAtender)
        // La constancia solo se abre si existe una recepción guardada.
        binding.btnVerConstancia.visibility = visibleSi(recepcion != null)
        binding.acciones.visibility = visibleSi(porAtender || recepcion != null)
    }

    private fun mostrarResultado(recepcion: Recepcion?, visita: VisitaFallida?) {
        ocultarAviso(binding.avisoResultado)
        ocultarAviso(binding.avisoVisitaRealizada)
        binding.cardRecepcion.visibility = View.GONE
        binding.cardCobro.visibility = View.GONE
        binding.cardVisita.visibility = View.GONE
        binding.excepcion.root.visibility = View.GONE

        if (recepcion != null) mostrarRecepcion(recepcion)
        if (visita != null) mostrarResultadoVisita(visita)
    }

    private fun mostrarRecepcion(recepcion: Recepcion) {
        val resultado = recepcion.resultadoEntrega.orEmpty()
        mostrarAviso(binding.avisoResultado, Tono.EXITO, getString(R.string.recepcion_registrada), resultado)

        binding.cardRecepcion.visibility = View.VISIBLE
        limpiarFilas(binding.contenedorRecepcion)
        agregarFilaDato(binding.contenedorRecepcion, getString(R.string.receptor), recepcion.nombreReceptor)
        agregarFilaDato(binding.contenedorRecepcion, getString(R.string.fecha_y_hora), formatearFechaHora(recepcion.fechaHora!!))
        agregarFilaDato(binding.contenedorRecepcion, getString(R.string.resultado), resultado)

        if (recepcion.tieneCobro()) {
            binding.cardCobro.visibility = View.VISIBLE
            limpiarFilas(binding.contenedorCobro)
            agregarFilasCobro(binding.contenedorCobro, recepcion)
        }
        mostrarExcepcion(binding.excepcion, recepcion.sectorRiesgo, recepcion.motivoExoneracion, recepcion.notaSeguridad)
    }

    /** Muestra el motivo de la visita fallida y el pedido como no entregado (HU-05). */
    private fun mostrarResultadoVisita(visita: VisitaFallida) {
        val causa = visita.causa.orEmpty()
        mostrarAviso(binding.avisoResultado, Tono.ADVERTENCIA, getString(R.string.visita_registrada),
            getString(R.string.visita_fallida_causa, causa.replaceFirstChar { it.lowercase() }))

        binding.cardVisita.visibility = View.VISIBLE
        val contenedor = binding.contenedorVisita
        limpiarFilas(contenedor)
        agregarFilaDato(contenedor, getString(R.string.causa), causa)
        visita.motivoRechazo?.let { agregarFilaDato(contenedor, getString(R.string.motivo), it) }
        agregarFilaDato(contenedor, getString(R.string.estado_pedido), getString(R.string.no_entregado))
        agregarFilaDato(contenedor, getString(R.string.fecha_y_hora), formatearFechaHora(visita.fechaHora!!))
        agregarFilaDato(contenedor, getString(R.string.gps), formatearGps(visita.latitud, visita.longitud))
        val observaciones = layoutInflater.inflate(R.layout.view_texto_soporte, contenedor, false) as TextView
        observaciones.text = visita.observaciones
        contenedor.addView(observaciones)

        mostrarAviso(binding.avisoVisitaRealizada, Tono.INFO,
            getString(R.string.visita_realizada), getString(R.string.visita_realizada_texto))
        mostrarExcepcion(binding.excepcion, visita.sectorRiesgo, visita.motivoExoneracion, visita.notaSeguridad)
    }

    private fun mostrarAvisoUltimaActualizacion() {
        mostrarAviso(
            binding.avisoSinConexion, Tono.ADVERTENCIA, getString(R.string.sin_conexion),
            getString(R.string.datos_ultima_actualizacion, paradasViewModel.ultimaActualizacion)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID_PARADA = "idParada"

        fun nuevaInstancia(idParada: String) = DetalleParadaFragment().apply {
            arguments = bundleOf(ARG_ID_PARADA to idParada)
        }
    }
}
