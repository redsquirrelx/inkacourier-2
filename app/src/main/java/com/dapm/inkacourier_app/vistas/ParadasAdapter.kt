package com.dapm.inkacourier_app.vistas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.databinding.ItemParadaBinding
import com.dapm.inkacourier_app.modelo.Parada

/** Presenta las paradas de la jornada con su estado operativo (HU-01/CA-04). */
class ParadasAdapter(
    private val alSeleccionar: (Parada) -> Unit
) : ListAdapter<Parada, ParadasAdapter.ParadaViewHolder>(ComparadorParadas) {

    class ParadaViewHolder(val binding: ItemParadaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParadaViewHolder {
        val binding = ItemParadaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParadaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParadaViewHolder, position: Int) {
        val parada = getItem(position)
        val contexto = holder.itemView.context
        with(holder.binding) {
            txtNumero.text = String.format("%02d", position + 1)
            txtPedido.text = contexto.getString(R.string.pedido, parada.idPedido)
            txtDestinatario.text = parada.destinatario
            txtDomicilio.text = parada.domicilio
            txtVentana.text = parada.ventanaHoraria
            mostrarEstado(txtEstado, parada.estado)
            root.setOnClickListener { alSeleccionar(parada) }
        }
    }

    object ComparadorParadas : DiffUtil.ItemCallback<Parada>() {
        override fun areItemsTheSame(anterior: Parada, nueva: Parada) = anterior.idParada == nueva.idParada
        override fun areContentsTheSame(anterior: Parada, nueva: Parada) = anterior == nueva
    }
}
