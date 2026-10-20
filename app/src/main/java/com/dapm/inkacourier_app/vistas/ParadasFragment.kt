package com.dapm.inkacourier_app.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dapm.inkacourier_app.MainActivity
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.databinding.FragmentParadasBinding

/** Pantalla de paradas: lista de la jornada (HU-01/CA-01, CA-04, CA-05). */
class ParadasFragment : Fragment() {

    private var _binding: FragmentParadasBinding? = null
    private val binding get() = _binding!!

    private val paradasViewModel: ParadasViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentParadasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBarra(binding.barra, getString(R.string.titulo_paradas), alVolver = null)
        binding.txtFechaJornada.text = paradasViewModel.fechaJornada
        binding.rvParadas.layoutManager = LinearLayoutManager(requireContext())

        // Consulta las asignaciones del repartidor actual; la lista sigue sus cambios.
        val adapter = ParadasAdapter(paradasViewModel.consultarParadas()) { parada -> abrirDetalle(parada.idParada) }
        binding.rvParadas.adapter = adapter
        paradasViewModel.paradas.observe(viewLifecycleOwner) { paradas ->
            adapter.actualizarParadas(paradas)
            binding.txtCantidad.text = getString(R.string.asignaciones, paradas.size)
        }
        paradasViewModel.sinConexion.observe(viewLifecycleOwner) { sinConexion ->
            if (sinConexion) mostrarAvisoUltimaActualizacion() else ocultarAviso(binding.avisoSinConexion)
        }
    }

    /** Indica que los datos son los guardados en la última actualización, no recién sincronizados. */
    private fun mostrarAvisoUltimaActualizacion() {
        mostrarAviso(
            binding.avisoSinConexion, Tono.ADVERTENCIA, getString(R.string.sin_conexion),
            getString(R.string.datos_ultima_actualizacion, paradasViewModel.ultimaActualizacion)
        )
    }

    private fun abrirDetalle(idParada: String) {
        (requireActivity() as MainActivity).navegarA(DetalleParadaFragment.nuevaInstancia(idParada))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
