package com.dapm.inkacourier_app.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.databinding.DialogFirmaReceptorBinding

/**
 * Diálogo de captura de firma del receptor (RF-08). Confirmar asocia la firma al borrador;
 * cancelar vuelve sin modificar la firma anterior.
 */
class FirmaReceptorDialog : DialogFragment() {

    private var _binding: DialogFirmaReceptorBinding? = null
    private val binding get() = _binding!!

    private val recepcionViewModel: RecepcionViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialogo_Firma)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFirmaReceptorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val borrador = recepcionViewModel.borrador.value
        // Identifica al receptor solo cuando ya se registró su identidad.
        val nombre = borrador?.nombreReceptor.orEmpty()
        binding.txtReceptor.visibility = visibleSi(nombre.isNotBlank())
        binding.txtReceptor.text = "$nombre · ${borrador?.tipoDocumento} ${borrador?.numeroDocumento}".trim()

        binding.firmaView.alCambiarTrazos = { hayTrazos ->
            binding.btnConfirmarFirma.isEnabled = hayTrazos
            binding.txtIndicacion.visibility = visibleSi(!hayTrazos)
        }
        binding.btnBorrarFirma.setOnClickListener { binding.firmaView.borrar() }
        binding.btnCancelarFirma.setOnClickListener { dismiss() }
        binding.btnConfirmarFirma.setOnClickListener { confirmarFirma() }
    }

    private fun confirmarFirma() {
        if (!binding.firmaView.tieneTrazos) return
        recepcionViewModel.asociarFirma(binding.firmaView.obtenerFirma())
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ETIQUETA = "FirmaReceptorDialog"
    }
}
