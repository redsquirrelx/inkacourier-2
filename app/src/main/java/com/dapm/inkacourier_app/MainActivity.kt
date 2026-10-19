package com.dapm.inkacourier_app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.dapm.inkacourier_app.controlador.ParadasViewModel
import com.dapm.inkacourier_app.databinding.ActivityMainBinding
import com.dapm.inkacourier_app.vistas.ParadasFragment

/**
 * Única Activity de la aplicación. Aloja los Fragments de NAV-01 y comunica
 * la conectividad del dispositivo a ParadasViewModel (HU-01/CA-05).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val paradasViewModel: ParadasViewModel by viewModels()
    private lateinit var conectividad: ConnectivityManager

    /** Observa la red predeterminada: sus capacidades indican si hay acceso a internet. */
    private val observadorRed = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            informarConexion(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))

        override fun onLost(network: Network) = informarConexion(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarMargenesDelSistema()

        conectividad = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Destino inicial de NAV-01: paradas.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor, ParadasFragment())
                .commit()
        }
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

    /** Retorno implícito de NAV-01 (retroceso: true). */
    fun volver() {
        supportFragmentManager.popBackStack()
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
}
