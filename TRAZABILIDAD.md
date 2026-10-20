# Trazabilidad HU → código → pruebas

Fuente de diseño: `D:\Proyecto\IngSoftware` (historias, NAV-01, mapa de símbolos, DEC-NAV-01, DEC-SEQ-01, DEC-UI-01).
Paquete base: `com.dapm.inkacourier_app` (`modelo`, `vistas`, `controlador`).

## Destinos de NAV-01

| Destino | Clase | Layout | Vista HTML de referencia |
|---|---|---|---|
| `paradas` (inicio) | `vistas/ParadasFragment`, `ParadasAdapter` | `fragment_paradas`, `item_parada` | `vistas/paradas.html` |
| `detalle_parada` | `vistas/DetalleParadaFragment` | `fragment_detalle_parada` | `vistas/detalle-parada.html` |
| `registro_recepcion` | `vistas/RegistroRecepcionFragment` | `fragment_registro_recepcion` | `vistas/registro-recepcion.html` |
| `captura_firma` | `vistas/FirmaReceptorDialog`, `FirmaView` | `dialog_firma_receptor` | `vistas/firma-receptor.html` |
| `camara` (externa) | `ActivityResultContracts.TakePicturePreview` | — | — |
| `constancia_recepcion` | `vistas/ConstanciaRecepcionFragment` | `fragment_constancia_recepcion` | `vistas/constancia-recepcion.html` |
| `registro_visita_fallida` | `vistas/RegistroVisitaFallidaFragment` | `fragment_registro_visita_fallida` | `vistas/visita-fallida.html` |

Controladores (alcance de Activity): `ParadasViewModel` (ruta simulada compartida),
`RecepcionViewModel` (HU-03/HU-04), `VisitaFallidaViewModel` (HU-05).

## Criterios de aceptación

| HU / CA | Implementación | Datos simulados | Pruebas |
|---|---|---|---|
| HU-01/CA-01 | `DetalleParadaFragment.mostrarDetalle` | P01 | `ParadasViewModelTest.ca01_*`, `NavegacionTest.hu01_seleccionarParada*` |
| HU-01/CA-02 | `cardReferencias` en detalle | P01, P06 | `ParadasViewModelTest.ca02_*`, `NavegacionTest.hu01_seleccionarParada*` |
| HU-01/CA-03 | `cardContraentrega` en detalle | P02, P07, P08 | `ParadasViewModelTest.ca03_*`, `NavegacionTest.hu01_paradaContraentrega*` |
| HU-01/CA-04 | `ParadasAdapter` + `mostrarEstado` | P01–P08 | `ParadasViewModelTest.ca04_*`, `NavegacionTest.hu01_paradaEntregada*` |
| HU-01/CA-05 | `MainActivity` (conectividad) → `ParadasViewModel.actualizarConexion`; aviso en lista y detalle | `ultimaActualizacion` | `ParadasViewModelTest.ca05_*` |
| HU-03/CA-01 | `RecepcionViewModel.guardarRecepcion` (titular, DNI/CE, firma, foto, fecha, GPS) | P01 | `RecepcionViewModelTest.hu03ca01_*`, `RecepcionGuardadoTest.hu03ca01_*` |
| HU-03/CA-02 | Tercero autorizado: `autorizacionCompleta` | P01 (`admiteTercero`) | `RecepcionViewModelTest.hu03ca02_*`, `RecepcionGuardadoTest.hu03ca02_*` |
| HU-03/CA-03 | Excepción de seguridad: `fotografiaCompleta`, `configurarExcepcionFormulario` | P05 (`sectorRiesgo`) | `RecepcionViewModelTest.hu03ca03_*`, `RecepcionGuardadoTest.hu03ca03_*` |
| HU-04/CA-01 | `verificarCobro` (efectivo), `recaudacionFisicaSoles`, unicidad por `idPedido` (RF-27) | P02, P07 | `RecepcionViewModelTest.hu04ca01_*`, `RecepcionGuardadoTest.hu04ca01_*` |
| HU-04/CA-02 | `verificarCobro` (Yape/Plin: código, observación, confirmación de Despacho) | P02, P08 | `RecepcionViewModelTest.hu04ca02_*`, `RecepcionGuardadoTest.hu04ca02_*` |
| HU-04/CA-03 | Tercero + cobro en el mismo registro | P07 | `RecepcionGuardadoTest.hu04ca03_*` |
| HU-05/CA-01 | `VisitaFallidaViewModel.puedeRegistrarAusencia` (5–7 min, ≥10 min) | P01 | `VisitaFallidaViewModelTest.ca01_*` |
| HU-05/CA-02 | Causa dirección no localizada + observaciones + GPS | P06 | `VisitaFallidaViewModelTest.ca02_*` |
| HU-05/CA-03 | Motivo de rechazo y constancias | P01 | `VisitaFallidaViewModelTest.ca03_*` |
| HU-05/CA-04 | Falta de pago solo en contraentrega | P02 | `VisitaFallidaViewModelTest.ca04_*` |
| HU-05/CA-05 | Excepción de seguridad sin sustituir la causa | P05 | `VisitaFallidaViewModelTest.ca05_*` |

Retornos de NAV-01: guardar con éxito publica `aplicarResultadoPersistido` y vuelve al
detalle; Cancelar/Atrás descarta el borrador sin confirmación (D18) —
`NavegacionTest.registroIncompleto*`, `NavegacionTest.visitaFallida*`.

## Datos simulados (ParadasViewModel)

| Parada | Pedido | Escenario |
|---|---|---|
| P01 | IC-1001 | Pendiente, referencias, admite tercero |
| P02 | IC-1002 | En camino, contraentrega S/ 128.50 (Efectivo·Yape·Plin), admite tercero |
| P03 | IC-1003 | Entregada (resultado previo) |
| P04 | IC-1004 | Fallida (resultado previo) |
| P05 | IC-1005 | Zona de alto riesgo con exoneración |
| P06 | IC-1006 | Numeración insuficiente (s/n) |
| P07 | IC-1007 | Contraentrega S/ 64.90, admite tercero |
| P08 | IC-1008 | Contraentrega S/ 210.00 solo Yape·Plin |

Los datos viven en memoria y se restablecen al reiniciar el proceso.

## Alineación con los diagramas de secuencia

| Diagrama | Correspondencia en el código |
|---|---|
| SEQ-01/02 | `ParadasFragment` / `DetalleParadaFragment` → `ParadasViewModel.consultarParadas`, `consultarParada`, `estaSinConexion` |
| SEQ-03/09 | `RegistroRecepcionFragment` → `RecepcionViewModel.obtenerBorrador`; el borrador fija `importeCobradoSoles` y `verificarCobro(parada)` lo compara con el importe exacto |
| SEQ-04 | El Fragment abre `FirmaReceptorDialog` y la cámara; con los datos completos invoca `completarFechaUbicacion()` |
| SEQ-05/11 | `guardarRecepcion(parada)` → `aplicarResultadoPersistido(ENTREGADA)` → detalle → `obtenerRecepcionGuardada` → constancia |
| SEQ-06/07 | `RegistroVisitaFallidaFragment` → `obtenerBorrador`, `actualizarContacto`, `puedeRegistrarAusencia(ahora)`, `completarFechaUbicacion(ahora)` |
| SEQ-08 | `guardarVisitaFallida(parada, ahora)` → `aplicarResultadoPersistido(FALLIDA)` → `mostrarResultadoVisita`; Cancelar → `descartarBorrador()` |

Simplificaciones respecto de los contratos provisionales del mapa de símbolos: en lugar de
`actualizarBorrador(recepcion)` se usan operaciones por sección (`actualizarIdentidad`,
`seleccionarMedioPago`, …); el guardado devuelve el registro o `null` en lugar de `Result`
suspendido; las fechas son `Long` (minSdk 24) y firma/foto son `Bitmap` en memoria.

## Inicio de sesión y aislamiento de asignaciones (SEQ-12)

Fuente: `IngSoftware/implementacion` (INSTRUCCIONES.md, SEQ-12, navegacion.yaml, interfaz/inicio-sesion*.html).

| Contrato | Implementación |
|---|---|
| `InicioSesionFragment` (`usuario`, `contrasena`, `btnIniciarSesion`) | `vistas/InicioSesionFragment`, `fragment_inicio_sesion` |
| `SesionViewModel` (`repartidores`, `repartidorActual`, `iniciarSesion`) | `controlador/SesionViewModel` |
| `Repartidor` / `Parada.idRepartidor` | `modelo/Repartidor`; P01–P08 → REP-01, P09–P10 → REP-02 |
| Contexto compartido | `MainActivity.defaultViewModelProviderFactory`: una `SesionViewModel` inyectada en `ParadasViewModel`, que a su vez se inyecta en `RecepcionViewModel` y `VisitaFallidaViewModel` |
| Filtrado y verificación | `ParadasViewModel.consultarParadas` (filtra y publica), `consultarParada` (`IllegalStateException`), `buscarParadaPropia` |
| Revalidación en registros | `obtenerBorrador`, operaciones del borrador, `guardarRecepcion`, `guardarVisitaFallida`, `obtenerRecepcionGuardada`, `obtenerVisitaGuardada`, `descartarBorrador`, `aplicarResultadoPersistido` |
| Rechazo sin cierre de la app | `MainActivity.conAccesoPropio` en detalle y formularios |
| Proceso nuevo / rotación | `MainActivity.onCreate`: sin sesión descarta el estado guardado y abre `inicio_sesion`; con sesión (rotación) lo restaura |

Pruebas: `SesionViewModelTest` (JVM), `InicioSesionTest` (Espresso), `RecepcionGuardadoTest.aislamiento_*`.
`NavegacionTest` inicia sesión como REP-01 antes de cada recorrido.
