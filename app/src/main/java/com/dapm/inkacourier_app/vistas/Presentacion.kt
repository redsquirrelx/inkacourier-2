package com.dapm.inkacourier_app.vistas

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.dapm.inkacourier_app.R
import com.dapm.inkacourier_app.databinding.ItemFilaDatoBinding
import com.dapm.inkacourier_app.databinding.ViewAvisoBinding
import com.dapm.inkacourier_app.databinding.ViewBarraBinding
import com.dapm.inkacourier_app.databinding.ViewDatoIconoBinding
import com.dapm.inkacourier_app.databinding.ViewExcepcionFormularioBinding
import com.dapm.inkacourier_app.databinding.ViewExcepcionSeguridadBinding
import com.dapm.inkacourier_app.databinding.ViewFechaUbicacionBinding
import com.dapm.inkacourier_app.modelo.EstadoParada
import com.dapm.inkacourier_app.modelo.Recepcion
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Funciones de presentación reutilizadas por varias pantallas, equivalentes a los
 * componentes compartidos de diseno/interfaces/sistema-diseno/componentes.css.
 */

private val LOCALE_PERU = Locale.forLanguageTag("es-PE")

/** Colores de .badge, .notice y sus variantes success, warning, info y error. */
enum class Tono(@ColorRes val fondo: Int, @ColorRes val texto: Int, @DrawableRes val icono: Int) {
    NEUTRO(R.color.md_surface_high, R.color.md_on_surface_variant, R.drawable.ic_info),
    EXITO(R.color.md_success_container, R.color.md_success, R.drawable.ic_check),
    ADVERTENCIA(R.color.md_warning_container, R.color.md_warning, R.drawable.ic_advertencia),
    INFO(R.color.md_info_container, R.color.md_info, R.drawable.ic_info),
    ERROR(R.color.md_error_container, R.color.md_on_error_container, R.drawable.ic_advertencia)
}

fun visibleSi(condicion: Boolean): Int = if (condicion) View.VISIBLE else View.GONE

fun formatearSoles(importe: BigDecimal?): String =
    "S/ " + (importe ?: BigDecimal.ZERO).setScale(2).toPlainString()

/** Ejemplo: "16 oct. 2026 · 09:41". */
fun formatearFechaHora(milisegundos: Long): String =
    SimpleDateFormat("d MMM yyyy · HH:mm", LOCALE_PERU).format(Date(milisegundos))

fun formatearHora(milisegundos: Long): String =
    SimpleDateFormat("HH:mm", LOCALE_PERU).format(Date(milisegundos))

/** Ejemplo: "−12.0931, −77.0287" (con el signo menos tipográfico del diseño). */
fun formatearGps(latitud: Double?, longitud: Double?): String {
    fun coordenada(valor: Double?) = String.format(Locale.US, "%.4f", valor ?: 0.0).replace('-', '−')
    return "${coordenada(latitud)}, ${coordenada(longitud)}"
}

fun mostrarInsignia(insignia: TextView, texto: String, tono: Tono) {
    val contexto = insignia.context
    insignia.text = texto
    insignia.setTextColor(ContextCompat.getColor(contexto, tono.texto))
    insignia.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(contexto, tono.fondo))
}

/** Insignia de estado operativo de la parada (HU-01/CA-04). */
fun mostrarEstado(insignia: TextView, estado: EstadoParada) {
    val tono = when (estado) {
        EstadoParada.PENDIENTE -> Tono.ADVERTENCIA
        EstadoParada.EN_CAMINO -> Tono.INFO
        EstadoParada.ENTREGADA -> Tono.EXITO
        EstadoParada.FALLIDA -> Tono.ERROR
    }
    mostrarInsignia(insignia, estado.etiqueta, tono)
}

fun mostrarAviso(aviso: ViewAvisoBinding, tono: Tono, titulo: String, texto: String) {
    val contexto = aviso.root.context
    val colorTexto = ContextCompat.getColor(contexto, tono.texto)
    aviso.root.visibility = View.VISIBLE
    aviso.root.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(contexto, tono.fondo))
    aviso.imgAviso.setImageResource(tono.icono)
    aviso.imgAviso.imageTintList = ColorStateList.valueOf(colorTexto)
    aviso.txtTituloAviso.text = titulo
    aviso.txtTituloAviso.setTextColor(colorTexto)
    aviso.txtTextoAviso.text = texto
    aviso.txtTextoAviso.setTextColor(colorTexto)
}

fun ocultarAviso(aviso: ViewAvisoBinding) {
    aviso.root.visibility = View.GONE
}

/** Agrega una fila etiqueta/valor al final de un contenedor. */
fun agregarFilaDato(contenedor: LinearLayout, etiqueta: String, valor: String) {
    val fila = ItemFilaDatoBinding.inflate(LayoutInflater.from(contenedor.context), contenedor, true)
    fila.txtEtiqueta.text = etiqueta
    fila.txtValor.text = valor
}

/** Quita las filas agregadas y conserva el título (primer hijo) de la tarjeta. */
fun limpiarFilas(contenedor: LinearLayout) {
    while (contenedor.childCount > 1) contenedor.removeViewAt(1)
}

fun mostrarDatoIcono(dato: ViewDatoIconoBinding, @DrawableRes icono: Int, principal: String, secundario: String) {
    dato.imgDato.setImageResource(icono)
    dato.txtPrincipal.text = principal
    dato.txtSecundario.text = secundario
}

/** Tarjeta "Fecha y ubicación": pendiente hasta que el registro las asocia (RF-10, RF-17). */
fun mostrarFechaUbicacion(tarjeta: ViewFechaUbicacionBinding, fechaHora: Long?, latitud: Double?, longitud: Double?) {
    val contexto = tarjeta.root.context
    if (fechaHora == null) {
        mostrarDatoIcono(tarjeta.datoUbicacion, R.drawable.ic_ubicacion,
            contexto.getString(R.string.ubicacion_pendiente), contexto.getString(R.string.ubicacion_pendiente_texto))
        mostrarDatoIcono(tarjeta.datoFecha, R.drawable.ic_reloj,
            contexto.getString(R.string.fecha_hora_visita), contexto.getString(R.string.fecha_hora_visita_texto))
    } else {
        mostrarDatoIcono(tarjeta.datoUbicacion, R.drawable.ic_ubicacion,
            contexto.getString(R.string.ubicacion_registrada),
            contexto.getString(R.string.gps_valor, formatearGps(latitud, longitud)))
        mostrarDatoIcono(tarjeta.datoFecha, R.drawable.ic_reloj,
            formatearFechaHora(fechaHora), contexto.getString(R.string.datos_registro))
    }
}

/** Barra superior: con botón Volver o, en la pantalla inicial, con el icono de marca. */
fun configurarBarra(barra: ViewBarraBinding, titulo: String, alVolver: (() -> Unit)?) {
    barra.txtTitulo.text = titulo
    if (alVolver == null) {
        barra.btnVolver.visibility = View.GONE
        barra.imgMarca.visibility = View.VISIBLE
    } else {
        barra.btnVolver.setOnClickListener { alVolver() }
    }
}

/** Lista desplegable de solo selección (MaterialAutoCompleteTextView). */
fun configurarDesplegable(
    campo: MaterialAutoCompleteTextView,
    opciones: List<String>,
    seleccion: String?,
    alSeleccionar: (String) -> Unit
) {
    campo.setSimpleItems(opciones.toTypedArray())
    campo.setText(seleccion.orEmpty(), false)
    campo.setOnItemClickListener { _, _, posicion, _ -> alSeleccionar(opciones[posicion]) }
}

/** Excepción de seguridad registrada, de solo lectura (detalle y constancia). */
fun mostrarExcepcion(tarjeta: ViewExcepcionSeguridadBinding, sector: String?, motivo: String?, nota: String) {
    val contexto = tarjeta.root.context
    if (sector == null) {
        tarjeta.root.visibility = View.GONE
        return
    }
    tarjeta.root.visibility = View.VISIBLE
    mostrarAviso(tarjeta.avisoExoneracion, Tono.ADVERTENCIA,
        contexto.getString(R.string.fotografia_exonerada), contexto.getString(R.string.fotografia_exonerada_texto))
    tarjeta.contenedorExcepcion.removeAllViews()
    agregarFilaDato(tarjeta.contenedorExcepcion, contexto.getString(R.string.sector), sector)
    agregarFilaDato(tarjeta.contenedorExcepcion, contexto.getString(R.string.motivo), motivo.orEmpty())
    if (nota.isNotBlank()) agregarFilaDato(tarjeta.contenedorExcepcion, contexto.getString(R.string.nota), nota)
}

/**
 * Excepción de seguridad editable, compartida por recepción (HU-03/CA-03) y visita fallida (HU-05/CA-05).
 * Solo se muestra si la parada está en un sector de alto riesgo.
 */
fun configurarExcepcionFormulario(
    tarjeta: ViewExcepcionFormularioBinding,
    sector: String?,
    motivo: String?,
    nota: String,
    alCambiar: (motivo: String?, nota: String) -> Unit
) {
    val contexto = tarjeta.root.context
    if (sector == null) {
        tarjeta.root.visibility = View.GONE
        return
    }
    tarjeta.root.visibility = View.VISIBLE
    mostrarAviso(tarjeta.avisoExoneracion, Tono.ADVERTENCIA,
        contexto.getString(R.string.fotografia_exonerada), contexto.getString(R.string.fotografia_exonerada_texto))
    tarjeta.edtSectorRiesgo.setText(sector)
    tarjeta.edtNotaSeguridad.setText(nota)

    var motivoElegido = motivo
    configurarDesplegable(tarjeta.selMotivoExoneracion, Recepcion.MOTIVOS_EXONERACION, motivo) { elegido ->
        motivoElegido = elegido
        alCambiar(motivoElegido, tarjeta.edtNotaSeguridad.text.toString())
    }
    tarjeta.edtNotaSeguridad.doAfterTextChanged { texto -> alCambiar(motivoElegido, texto.toString()) }
}

/**
 * Filas del cobro registrado (HU-04). En efectivo se muestra el aporte a la recaudación
 * física; en Yape o Plin, el código, la confirmación de Despacho y que no se suma (RF-26).
 */
fun agregarFilasCobro(contenedor: LinearLayout, recepcion: Recepcion) {
    val contexto = contenedor.context
    agregarFilaDato(contenedor, contexto.getString(R.string.importe_exacto), formatearSoles(recepcion.importeCobradoSoles))
    agregarFilaDato(contenedor, contexto.getString(R.string.medio), recepcion.medioPago.orEmpty())
    agregarFilaDato(contenedor, contexto.getString(R.string.estado_cobro), contexto.getString(R.string.pago_integro_verificado))
    if (recepcion.medioPago == Recepcion.EFECTIVO) {
        agregarFilaDato(contenedor, contexto.getString(R.string.recaudacion_fisica),
            contexto.getString(R.string.incorporados, formatearSoles(recepcion.aporteEfectivoSoles)))
    } else {
        agregarFilaDato(contenedor, contexto.getString(R.string.codigo_operacion), recepcion.codigoOperacion)
        agregarFilaDato(contenedor, contexto.getString(R.string.confirmacion_de_despacho), contexto.getString(R.string.abono_confirmado_fila))
        agregarFilaDato(contenedor, contexto.getString(R.string.efectivo_fisico),
            contexto.getString(R.string.no_se_suma, formatearSoles(recepcion.aporteEfectivoSoles)))
    }
}
