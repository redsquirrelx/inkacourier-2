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
 * Controlador de HU-01. Fuente central de las rutas simuladas de todos los repartidores,
 * compartida (alcance de Activity) por la lista, el detalle y los formularios de registro.
 * Solo expone las asignaciones del repartidor autenticado en SesionViewModel.
 */
class ParadasViewModel(private val sesion: SesionViewModel) : ViewModel() {

    /** Instantánea central con las paradas de todos los repartidores; nunca se publica completa. */
    private var rutaCentral: List<Parada> = crearRutaSimulada()

    /** Paradas del repartidor actual; vacía sin sesión. */
    private val _paradas = MutableLiveData<List<Parada>>(emptyList())
    val paradas: LiveData<List<Parada>> = _paradas

    private val _sinConexion = MutableLiveData(false)
    val sinConexion: LiveData<Boolean> = _sinConexion

    /** Momento en que se descargó la ruta simulada del repartidor actual (HU-01/CA-05). */
    val ultimaActualizacion: String
        get() = sesion.repartidorActual?.let { ULTIMAS_ACTUALIZACIONES[it.idRepartidor] }.orEmpty()
    val fechaJornada = "16 de octubre · Lima"

    /** Filtra la instantánea central por el repartidor actual y la publica a la lista. */
    fun consultarParadas(): List<Parada> {
        val propias = paradasPropias()
        if (_paradas.value != propias) _paradas.value = propias
        return propias
    }

    /** Rechaza con IllegalStateException, sin exponer datos, si no hay sesión o la parada no es propia. */
    fun consultarParada(idParada: String): Parada =
        buscarParadaPropia(idParada) ?: error("La parada $idParada no está asignada al repartidor actual")

    /** Resuelve el ID contra la fuente central; null si no existe, es ajena o no hay sesión. */
    fun buscarParadaPropia(idParada: String): Parada? =
        paradasPropias().firstOrNull { it.idParada == idParada }

    private fun paradasPropias(): List<Parada> {
        val idRepartidor = sesion.repartidorActual?.idRepartidor ?: return emptyList()
        return rutaCentral.filter { it.idRepartidor == idRepartidor }
    }

    fun estaSinConexion(): Boolean = _sinConexion.value == true

    /** MainActivity informa los cambios de conectividad del dispositivo. */
    fun actualizarConexion(hayConexion: Boolean) {
        if (_sinConexion.value != !hayConexion) {
            Log.d(TAG, "Conexión disponible: $hayConexion")
            _sinConexion.value = !hayConexion
        }
    }

    /**
     * Actualiza el estado de una parada propia tras guardar el resultado y publica la ruta.
     * El adaptador compara la lista anterior con la nueva para notificar los cambios.
     */
    fun aplicarResultadoPersistido(idParada: String, estado: EstadoParada) {
        if (buscarParadaPropia(idParada) == null) {
            Log.w(TAG, "Parada $idParada no asignada al repartidor actual; no se actualiza")
            return
        }
        rutaCentral = rutaCentral.map { parada ->
            if (parada.idParada == idParada) parada.copy(estado = estado) else parada
        }
        consultarParadas()
        Log.d(TAG, "Parada $idParada actualizada a $estado")
    }

    /** Ruta de prueba con casos de HU-01, HU-03, HU-04 y HU-05. */
    private fun crearRutaSimulada(): List<Parada> = listOf(
        // Datos habituales, referencias de acceso y tercero autorizado (HU-01/CA-01, CA-02; HU-03).
        Parada(
            idParada = "P01", idPedido = "IC-1001", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "María Rojas", domicilio = "Av. Los Olivos 245", distrito = "Surquillo, Lima",
            contacto = "900 000 245", ventanaHoraria = "09:00 – 12:00",
            referenciasAcceso = listOf("Portón verde.", "Interior 2, timbre del lado derecho.", "Fachada de dos pisos."),
            admiteTercero = true,
            estado = EstadoParada.PENDIENTE
        ),
        // Contraentrega con todos los medios autorizados (HU-01/CA-03; HU-04; HU-05/CA-04).
        Parada(
            idParada = "P02", idPedido = "IC-1002", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Diego Torres", domicilio = "Jr. Las Flores 180", distrito = "Surquillo, Lima",
            contacto = "900 000 180", ventanaHoraria = "10:00 – 13:00",
            importeContraentregaSoles = BigDecimal("128.50"),
            mediosPagoAutorizados = listOf(Recepcion.EFECTIVO, Recepcion.YAPE, Recepcion.PLIN),
            admiteTercero = true,
            estado = EstadoParada.EN_CAMINO
        ),
        // Resultados ya registrados al descargar la ruta (HU-01/CA-04).
        Parada(
            idParada = "P03", idPedido = "IC-1003", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Ana Mendoza", domicilio = "Calle Los Cedros 96", distrito = "Surquillo, Lima",
            contacto = "900 000 096", ventanaHoraria = "08:00 – 11:00",
            estado = EstadoParada.ENTREGADA
        ),
        Parada(
            idParada = "P04", idPedido = "IC-1004", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Luis Vargas", domicilio = "Av. Los Pinos 320", distrito = "Surquillo, Lima",
            contacto = "900 000 320", ventanaHoraria = "08:00 – 11:00",
            estado = EstadoParada.FALLIDA
        ),
        // Zona de alto riesgo con exoneración de foto (HU-03/CA-03; HU-05/CA-05).
        Parada(
            idParada = "P05", idPedido = "IC-1005", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Rosa Quispe", domicilio = "Jr. Los Jazmines 410", distrito = "San Juan de Lurigancho, Lima",
            contacto = "900 000 410", ventanaHoraria = "11:00 – 14:00",
            sectorRiesgo = "Sector de riesgo registrado",
            estado = EstadoParada.PENDIENTE
        ),
        // Numeración insuficiente para localizar el predio (HU-05/CA-02).
        Parada(
            idParada = "P06", idPedido = "IC-1006", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Jorge Salas", domicilio = "Av. Los Olivos s/n", distrito = "Surquillo, Lima",
            contacto = "900 000 600", ventanaHoraria = "12:00 – 15:00",
            referenciasAcceso = listOf("Portón verde."),
            estado = EstadoParada.PENDIENTE
        ),
        // Segunda contraentrega con tercero autorizado (HU-04/CA-03).
        Parada(
            idParada = "P07", idPedido = "IC-1007", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Carmen Flores", domicilio = "Av. Arequipa 1520", distrito = "Lince, Lima",
            contacto = "900 001 520", ventanaHoraria = "13:00 – 16:00",
            importeContraentregaSoles = BigDecimal("64.90"),
            mediosPagoAutorizados = listOf(Recepcion.EFECTIVO, Recepcion.YAPE, Recepcion.PLIN),
            admiteTercero = true,
            estado = EstadoParada.PENDIENTE
        ),
        // Contraentrega que solo admite pago digital (HU-04/CA-02).
        Parada(
            idParada = "P08", idPedido = "IC-1008", idJornada = ID_JORNADA, idRepartidor = SesionViewModel.ID_REPARTIDOR_LUCHO,
            destinatario = "Pedro Huamán", domicilio = "Calle Las Begonias 75", distrito = "San Isidro, Lima",
            contacto = "900 000 075", ventanaHoraria = "14:00 – 17:00",
            importeContraentregaSoles = BigDecimal("210.00"),
            mediosPagoAutorizados = listOf(Recepcion.YAPE, Recepcion.PLIN),
            estado = EstadoParada.PENDIENTE
        ),
        // Ruta de muestra de REP-02: IDs propios, sin compartir paradas ni pedidos con REP-01.
        Parada(
            idParada = "P09", idPedido = "IC-2001", idJornada = ID_JORNADA_REP02, idRepartidor = SesionViewModel.ID_REPARTIDOR_ROSA,
            destinatario = "Elena Castro", domicilio = "Jr. Huáscar 512", distrito = "Jesús María, Lima",
            contacto = "900 002 512", ventanaHoraria = "09:00 – 12:00",
            referenciasAcceso = listOf("Edificio azul, departamento 301."),
            estado = EstadoParada.PENDIENTE
        ),
        Parada(
            idParada = "P10", idPedido = "IC-2002", idJornada = ID_JORNADA_REP02, idRepartidor = SesionViewModel.ID_REPARTIDOR_ROSA,
            destinatario = "Hugo Ramírez", domicilio = "Av. Brasil 1840", distrito = "Pueblo Libre, Lima",
            contacto = "900 001 840", ventanaHoraria = "11:00 – 14:00",
            importeContraentregaSoles = BigDecimal("45.00"),
            mediosPagoAutorizados = listOf(Recepcion.EFECTIVO, Recepcion.YAPE),
            estado = EstadoParada.EN_CAMINO
        )
    )

    companion object {
        private const val TAG = "ParadasViewModel"
        const val ID_JORNADA = "J-2026-10-16"
        const val ID_JORNADA_REP02 = "J-2026-10-16-02"

        /** Última descarga de la ruta de cada repartidor. */
        private val ULTIMAS_ACTUALIZACIONES = mapOf(
            SesionViewModel.ID_REPARTIDOR_LUCHO to "16 oct., 08:30",
            SesionViewModel.ID_REPARTIDOR_ROSA to "16 oct., 08:45"
        )
    }
}
