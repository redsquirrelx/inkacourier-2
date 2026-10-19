package com.dapm.inkacourier_app.controlador

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dapm.inkacourier_app.modelo.EstadoParada
import com.dapm.inkacourier_app.modelo.Parada
import com.dapm.inkacourier_app.modelo.Recepcion
import java.math.BigDecimal

/**
 * Controlador de HU-01. Contiene la ruta simulada de la jornada y es compartido
 * (alcance de Activity) por la lista, el detalle y los formularios de registro,
 * de modo que todas las pantallas leen la misma lista de paradas.
 */
class ParadasViewModel : ViewModel() {

    private val _paradas = MutableLiveData(crearRutaSimulada())
    val paradas: LiveData<List<Parada>> = _paradas

    private val _sinConexion = MutableLiveData(false)
    val sinConexion: LiveData<Boolean> = _sinConexion

    /** Momento en que se descargó la ruta simulada (HU-01/CA-05). */
    val ultimaActualizacion = "16 oct., 08:30"
    val fechaJornada = "16 de octubre · Lima"

    fun consultarParadas(): List<Parada> = _paradas.value.orEmpty()

    fun consultarParada(idParada: String): Parada =
        consultarParadas().first { it.idParada == idParada }

    fun estaSinConexion(): Boolean = _sinConexion.value == true

    /** MainActivity informa los cambios de conectividad del dispositivo. */
    fun actualizarConexion(hayConexion: Boolean) {
        if (_sinConexion.value != !hayConexion) {
            Log.d(TAG, "Conexión disponible: $hayConexion")
            _sinConexion.value = !hayConexion
        }
    }

    /**
     * Publica el estado de una parada después de guardar su resultado (DEC-SEQ-01).
     * Crea una lista nueva para que el ListAdapter detecte el cambio.
     */
    fun aplicarResultadoPersistido(idParada: String, estado: EstadoParada) {
        val actualizada = consultarParadas().map { parada ->
            if (parada.idParada == idParada) parada.copy(estado = estado) else parada
        }
        _paradas.value = actualizada
        Log.d(TAG, "Parada $idParada actualizada a $estado")
    }

    /** Datos deterministas que demuestran todos los criterios de HU-01, HU-03, HU-04 y HU-05. */
    private fun crearRutaSimulada(): List<Parada> = listOf(
        // Datos habituales, referencias de acceso y tercero autorizado (HU-01/CA-01, CA-02; HU-03).
        Parada(
            idParada = "P01", idPedido = "IC-1001", idJornada = ID_JORNADA,
            destinatario = "María Rojas", domicilio = "Av. Los Olivos 245", distrito = "Surquillo, Lima",
            contacto = "900 000 245", ventanaHoraria = "09:00 – 12:00",
            referenciasAcceso = listOf("Portón verde.", "Interior 2, timbre del lado derecho.", "Fachada de dos pisos."),
            admiteTercero = true,
            estado = EstadoParada.PENDIENTE
        ),
        // Contraentrega con todos los medios autorizados (HU-01/CA-03; HU-04; HU-05/CA-04).
        Parada(
            idParada = "P02", idPedido = "IC-1002", idJornada = ID_JORNADA,
            destinatario = "Diego Torres", domicilio = "Jr. Las Flores 180", distrito = "Surquillo, Lima",
            contacto = "900 000 180", ventanaHoraria = "10:00 – 13:00",
            importeContraentregaSoles = BigDecimal("128.50"),
            mediosPagoAutorizados = listOf(Recepcion.EFECTIVO, Recepcion.YAPE, Recepcion.PLIN),
            admiteTercero = true,
            estado = EstadoParada.EN_CAMINO
        ),
        // Resultados ya registrados al descargar la ruta (HU-01/CA-04).
        Parada(
            idParada = "P03", idPedido = "IC-1003", idJornada = ID_JORNADA,
            destinatario = "Ana Mendoza", domicilio = "Calle Los Cedros 96", distrito = "Surquillo, Lima",
            contacto = "900 000 096", ventanaHoraria = "08:00 – 11:00",
            estado = EstadoParada.ENTREGADA
        ),
        Parada(
            idParada = "P04", idPedido = "IC-1004", idJornada = ID_JORNADA,
            destinatario = "Luis Vargas", domicilio = "Av. Los Pinos 320", distrito = "Surquillo, Lima",
            contacto = "900 000 320", ventanaHoraria = "08:00 – 11:00",
            estado = EstadoParada.FALLIDA
        ),
        // Zona de alto riesgo con exoneración de foto (HU-03/CA-03; HU-05/CA-05).
        Parada(
            idParada = "P05", idPedido = "IC-1005", idJornada = ID_JORNADA,
            destinatario = "Rosa Quispe", domicilio = "Jr. Los Jazmines 410", distrito = "San Juan de Lurigancho, Lima",
            contacto = "900 000 410", ventanaHoraria = "11:00 – 14:00",
            sectorRiesgo = "Sector de riesgo registrado",
            estado = EstadoParada.PENDIENTE
        ),
        // Numeración insuficiente para localizar el predio (HU-05/CA-02).
        Parada(
            idParada = "P06", idPedido = "IC-1006", idJornada = ID_JORNADA,
            destinatario = "Jorge Salas", domicilio = "Av. Los Olivos s/n", distrito = "Surquillo, Lima",
            contacto = "900 000 600", ventanaHoraria = "12:00 – 15:00",
            referenciasAcceso = listOf("Portón verde."),
            estado = EstadoParada.PENDIENTE
        ),
        // Segunda contraentrega con tercero autorizado (HU-04/CA-03).
        Parada(
            idParada = "P07", idPedido = "IC-1007", idJornada = ID_JORNADA,
            destinatario = "Carmen Flores", domicilio = "Av. Arequipa 1520", distrito = "Lince, Lima",
            contacto = "900 001 520", ventanaHoraria = "13:00 – 16:00",
            importeContraentregaSoles = BigDecimal("64.90"),
            mediosPagoAutorizados = listOf(Recepcion.EFECTIVO, Recepcion.YAPE, Recepcion.PLIN),
            admiteTercero = true,
            estado = EstadoParada.PENDIENTE
        ),
        // Contraentrega que solo admite pago digital (HU-04/CA-02).
        Parada(
            idParada = "P08", idPedido = "IC-1008", idJornada = ID_JORNADA,
            destinatario = "Pedro Huamán", domicilio = "Calle Las Begonias 75", distrito = "San Isidro, Lima",
            contacto = "900 000 075", ventanaHoraria = "14:00 – 17:00",
            importeContraentregaSoles = BigDecimal("210.00"),
            mediosPagoAutorizados = listOf(Recepcion.YAPE, Recepcion.PLIN),
            estado = EstadoParada.PENDIENTE
        )
    )

    companion object {
        private const val TAG = "ParadasViewModel"
        const val ID_JORNADA = "J-2026-10-16"
    }
}
