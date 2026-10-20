package com.dapm.inkacourier_app.vistas

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.databinding.ItemParadaBinding
import com.dapm.inkacourier_app.modelo.Parada

/** Fila de la lista: muestra una parada con su estado operativo (HU-01/CA-04). */
class ParadaHolder(private val binding: ItemParadaBinding) : RecyclerView.ViewHolder(binding.root) {

    fun enlazar(parada: Parada, numero: Int, alSeleccionar: (Parada) -> Unit) {
        val contexto = binding.root.context
        binding.txtNumero.text = String.format("%02d", numero)
        binding.txtPedido.text = contexto.getString(R.string.pedido, parada.idPedido)
        binding.txtDestinatario.text = parada.destinatario
        binding.txtDomicilio.text = parada.domicilio
        binding.txtVentana.text = parada.ventanaHoraria
        mostrarEstado(binding.txtEstado, parada.estado)
        binding.root.setOnClickListener { alSeleccionar(parada) }
    }
}

/** Presenta las paradas de la jornada (HU-01). Seleccionar una fila avisa al Fragment. */
class ParadasAdapter(
    private var paradas: List<Parada>,
    private val alSeleccionar: (Parada) -> Unit
) : RecyclerView.Adapter<ParadaHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParadaHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemParadaBinding.inflate(inflater, parent, false)
        return ParadaHolder(binding)
    }

    override fun onBindViewHolder(holder: ParadaHolder, position: Int) {
        holder.enlazar(paradas[position], position + 1, alSeleccionar)
    }

    override fun getItemCount(): Int = paradas.size

    /**
     * Reemplaza la lista. Si son las mismas paradas, solo notifica las filas que cambiaron
     * (por ejemplo, el estado tras un registro); si cambió el conjunto, redibuja la lista.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun actualizarParadas(nuevas: List<Parada>) {
        val anteriores = paradas
        paradas = nuevas
        if (anteriores.map { it.idParada } != nuevas.map { it.idParada }) {
            notifyDataSetChanged()
            return
        }
        for (posicion in nuevas.indices) {
            if (nuevas[posicion] != anteriores[posicion]) notifyItemChanged(posicion)
        }
    }
}
