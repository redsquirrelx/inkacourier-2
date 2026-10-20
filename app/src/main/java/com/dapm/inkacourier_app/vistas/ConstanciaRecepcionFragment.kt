package com.dapm.inkacourier_app.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dapm.inkacourier_app.MainActivity
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.databinding.FragmentConstanciaRecepcionBinding
import com.dapm.inkacourier_app.modelo.Recepcion

/**
 * Pantalla de constancia de recepción (RF-14). Consulta de solo lectura:
 * abrirla no genera otra recepción ni otro aporte a la recaudación.
 */
class ConstanciaRecepcionFragment : Fragment() {

    private var _binding: FragmentConstanciaRecepcionBinding? = null
    private val binding get() = _binding!!

    private val recepcionViewModel: RecepcionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConstanciaRecepcionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val actividad = requireActivity() as MainActivity
        configurarBarra(binding.barra, getString(R.string.titulo_constancia)) { actividad.volver() }

        val idParada = requireArguments().getString(ARG_ID_PARADA)!!
        val recepcion = recepcionViewModel.obtenerRecepcionGuardada(idParada)
        if (recepcion == null) {
            actividad.volver()
            return
        }
        mostrarConstancia(recepcion)
    }

    private fun mostrarConstancia(recepcion: Recepcion) {
        binding.txtPedido.text = getString(R.string.pedido, recepcion.idPedido)
        binding.txtResultado.text = recepcion.resultadoEntrega

        val identidad = binding.contenedorIdentidad
        agregarFilaDato(identidad, getString(R.string.receptor), recepcion.nombreReceptor)
        agregarFilaDato(identidad, getString(R.string.documento), "${recepcion.tipoDocumento} · ${recepcion.numeroDocumento}")
        agregarFilaDato(identidad, getString(R.string.recepcion), recepcion.tipoReceptor)

        binding.cardAutorizacion.visibility = visibleSi(recepcion.esTercero())
        if (recepcion.esTercero()) {
            val autorizacion = binding.contenedorAutorizacion
            agregarFilaDato(autorizacion, getString(R.string.autorizacion), getString(R.string.comprobada))
            agregarFilaDato(autorizacion, getString(R.string.recepcion_alternativa), getString(R.string.permitida))
            agregarFilaDato(autorizacion, getString(R.string.lugar), getString(R.string.mismo_domicilio))
        }

        binding.cardCobro.visibility = visibleSi(recepcion.tieneCobro())
        if (recepcion.tieneCobro()) agregarFilasCobro(binding.contenedorCobro, recepcion)

        binding.imgFirma.setImageBitmap(recepcion.firma)

        // Con exoneración, la foto se sustituye por el sector y el motivo (RF-14).
        val exonerada = recepcion.sectorRiesgo != null
        binding.cardFotografia.visibility = visibleSi(!exonerada)
        binding.imgFotografia.setImageBitmap(recepcion.fotografiaFachada)
        mostrarExcepcion(binding.excepcion, recepcion.sectorRiesgo, recepcion.motivoExoneracion, recepcion.notaSeguridad)

        mostrarFechaUbicacion(binding.fechaUbicacion, recepcion.fechaHora, recepcion.latitud, recepcion.longitud)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID_PARADA = "idParada"

        fun nuevaInstancia(idParada: String) = ConstanciaRecepcionFragment().apply {
            arguments = bundleOf(ARG_ID_PARADA to idParada)
        }
    }
}
