package com.dapm.inkacourier_app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.controlador.RecepcionViewModel
import com.dapm.inkacourier_app.controlador.SesionViewModel
import com.dapm.inkacourier_app.controlador.VisitaFallidaViewModel
import com.dapm.inkacourier_app.databinding.ActivityMainBinding
import com.dapm.inkacourier_app.vistas.InicioSesionFragment

/**
 * Única Activity de la aplicación. Aloja los Fragments de cada pantalla, comunica
 * la conectividad del dispositivo a ParadasViewModel (HU-01/CA-05) y exige
 * iniciar sesión antes de mostrar cualquier pantalla protegida.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sesionViewModel: SesionViewModel by viewModels()
    private val paradasViewModel: ParadasViewModel by viewModels()

    /**
     * Todos los controladores tienen alcance de Activity (activityViewModels en los Fragments)
     * y reciben el mismo contexto autenticado: una sola SesionViewModel y una sola fuente de paradas.
     */
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { SesionViewModel() }
        initializer { ParadasViewModel(sesionViewModel) }
        initializer { RecepcionViewModel(paradasViewModel) }
        initializer { VisitaFallidaViewModel(paradasViewModel) }
    }
    private lateinit var conectividad: ConnectivityManager

    /** Observa la red predeterminada: sus capacidades indican si hay acceso a internet. */
    private val observadorRed = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            informarConexion(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))

        override fun onLost(network: Network) = informarConexion(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // La sesión solo vive en memoria. En un proceso nuevo no hay sesión: el estado guardado
        // se descarta para no restaurar pantallas protegidas ni exponer sus datos.
        // En una rotación el ViewModel conserva la sesión y se restaura la pantalla actual.
        val haySesion = sesionViewModel.repartidorActual != null
        super.onCreate(if (haySesion) savedInstanceState else null)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargenesDelSistema()

        conectividad = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Pantalla inicial: inicio de sesión.
        if (!haySesion) abrirDestinoRaiz(InicioSesionFragment())
    }

    override fun onStart() {
        super.onStart()
        paradasViewModel.actualizarConexion(hayConexion())
        conectividad.registerDefaultNetworkCallback(observadorRed)
    }

    override fun onStop() {
        super.onStop()
        conectividad.unregisterNetworkCallback(observadorRed)
    }

    /** Abre un destino interno conservando el anterior en la pila para volver. */
    fun navegarA(destino: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, destino)
            .addToBackStack(null)
            .commit()
    }

    fun volver() {
        supportFragmentManager.popBackStack()
    }

    /** Reemplaza la pila completa por un destino: tras iniciar sesión, Atrás no vuelve al formulario. */
    fun abrirDestinoRaiz(destino: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, destino)
            .commit()
    }

    /**
     * Intercepta el rechazo (IllegalStateException) de una consulta de parada ajena o sin sesión
     * para no cerrar la app: sin sesión vuelve al inicio de sesión; con sesión, al destino anterior.
     */
    fun <T> conAccesoPropio(consulta: () -> T): T? = try {
        consulta()
    } catch (rechazo: IllegalStateException) {
        Log.w(TAG, "Acceso rechazado: ${rechazo.message}")
        if (sesionViewModel.repartidorActual == null) abrirDestinoRaiz(InicioSesionFragment()) else volver()
        null
    }

    private fun informarConexion(conectado: Boolean) {
        runOnUiThread { paradasViewModel.actualizarConexion(conectado) }
    }

    private fun hayConexion(): Boolean {
        val red = conectividad.activeNetwork ?: return false
        val capacidades = conectividad.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Barras del sistema y teclado (edge-to-edge con adjustResize). */
    private fun aplicarMargenesDelSistema() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contenedor) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, maxOf(barras.bottom, teclado.bottom))
            insets
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
