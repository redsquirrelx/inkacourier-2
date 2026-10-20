package com.dapm.inkacourier_app.vistas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dapm.inkacourier_app.MainActivity
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.controlador.SesionViewModel
import com.dapm.inkacourier_app.databinding.FragmentInicioSesionBinding

/**
 * Pantalla de inicio de sesión. Los errores se muestran en el propio formulario;
 * la validación de credenciales la realiza SesionViewModel, nunca esta vista.
 */
class InicioSesionFragment : Fragment() {

    private var _binding: FragmentInicioSesionBinding? = null
    private val binding get() = _binding!!

    private val sesionViewModel: SesionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioSesionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBarra(binding.barra, getString(R.string.iniciar_sesion), alVolver = null)
        binding.btnIniciarSesion.setOnClickListener { iniciarSesion() }
    }

    private fun iniciarSesion() {
        // Sin normalizar: las credenciales se envían tal como se escribieron.
        val usuario = binding.usuario.text.toString()
        val contrasena = binding.contrasena.text.toString()
        limpiarMensajes()
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarCamposObligatorios()
            return
        }
        if (sesionViewModel.iniciarSesion(usuario, contrasena)) abrirParadas() else mostrarErrorCredenciales()
    }

    /** Indica cada campo obligatorio vacío y permanece sin autenticar. */
    private fun mostrarCamposObligatorios() {
        if (binding.usuario.text.isNullOrEmpty()) binding.tilUsuario.helperText = getString(R.string.ingresa_usuario)
        if (binding.contrasena.text.isNullOrEmpty()) binding.tilContrasena.helperText = getString(R.string.ingresa_contrasena)
        mostrarAviso(binding.avisoInicioSesion, Tono.ERROR,
            getString(R.string.campos_obligatorios), getString(R.string.campos_obligatorios_texto))
    }

    /** Usuario inexistente o contraseña incorrecta: permanece en el formulario. */
    private fun mostrarErrorCredenciales() {
        mostrarAviso(binding.avisoInicioSesion, Tono.ERROR,
            getString(R.string.credenciales_incorrectas), getString(R.string.credenciales_incorrectas_texto))
    }

    private fun limpiarMensajes() {
        binding.tilUsuario.helperText = null
        binding.tilContrasena.helperText = null
        ocultarAviso(binding.avisoInicioSesion)
    }

    /** Abre las paradas del repartidor y retira el formulario de la pila. */
    private fun abrirParadas() {
        WindowCompat.getInsetsController(requireActivity().window, binding.root).hide(WindowInsetsCompat.Type.ime())
        (requireActivity() as MainActivity).abrirDestinoRaiz(ParadasFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
