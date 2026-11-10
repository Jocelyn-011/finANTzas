# Validación del Manual de Usuario de finANTzas

Fecha: 10 de noviembre de 2026.

## Resultado y alcance

- Aplicación instalada: `cl.finantzas.app`, actividad `.MainActivity`.
- Android informa `versionName=1.0`, `versionCode=1`, mínimo API 26 y objetivo API 37.
- Emulador utilizado: `emulator-5556`, modelo `sdk_gphone64_x86_64`, resolución física 1080 × 2280.
- `emulator-5554` figuraba desconectado (`offline`); no se utilizó.
- La aplicación abrió y presentó una sesión existente. Se conservó esa sesión.
- Inventario: 17 vistas/estados funcionales, incluidas seis hojas de Ajustes y el simulador reutilizado dentro de Análisis.
- Entrega: 12 imágenes PNG reales, revisadas visualmente; cubren 11 elementos del inventario (incluido el simulador integrado). Seis elementos permanecen sin captura de contenido.

## Método utilizado

1. Se enumeraron los archivos de aplicación y pruebas instrumentadas, y se revisaron el manifiesto, versión y navegación.
2. Se revisaron formularios, controles, reglas de cálculo, modelos de vista y rutas de persistencia/sincronización para distinguir operaciones implementadas de textos informativos.
3. Se consultó ADB con `devices -l` y el paquete mediante `dumpsys package`.
4. Se abrió la actividad instalada con `am start`, sin reinstalar ni compilar la aplicación.
5. Se utilizaron `input tap`, `input swipe` y la tecla Atrás para consulta. No se enviaron formularios.
6. Las capturas se obtuvieron con `exec-out screencap -p`, copiando el flujo binario directamente a los PNG. No se editaron, simularon, generaron ni retocaron imágenes.
7. Se inspeccionaron individualmente las capturas. Las de transiciones o pantallas ajenas se reemplazaron con capturas correctas después de ampliar las pausas.
8. Se comprobó la existencia de los destinos relativos de las imágenes y se registraron sus hashes SHA-256 en `capturas/INDICE_SHA256.csv`.

## Inventario de capturas revisadas

| Archivo | Contenido verificado | Límite de la comprobación |
|---|---|---|
| `01_inicio.png` | Inicio, presupuesto, categorías, alerta y simulador rápido | No se capturó la parte inferior de movimientos recientes. |
| `02_transacciones.png` | Formulario, impacto mensual e historial de cero movimientos | No se guardó un gasto ni se comprobó una tarjeta existente. |
| `03_analisis.png` | Resumen y controles del simulador | Se observó el escenario existente, sin modificarlo. |
| `04_analisis_detalle.png` | Proyección acumulada, ahorro mensual y comienzo de gráficos | Vista parcial de la página desplazada. |
| `05_ajustes_menu.png` | Apariencia y cinco accesos a paneles informativos | Se evitó capturar la tarjeta de cuenta. |
| `06_notificaciones.png` | Tres interruptores y acceso a topes | No se cambiaron interruptores ni límites. |
| `07_acerca_de.png` | Identificación v1.0.0-mvp, propósito y créditos | Texto propio de la aplicación, no certificación externa. |
| `08_ayuda.png` | FAQ, estrellas y formulario vacío | No se envió comentario ni se desplegaron todas las respuestas. |
| `09_privacidad.png` | Política de privacidad | No incluye todo el final del panel. |
| `10_condiciones.png` | Condiciones de uso | Lectura informativa, sin validación legal. |
| `11_presupuestos.png` | Global, Vivienda y Alimentación | Otras categorías quedan fuera del encuadre; ningún campo fue editado. |
| `12_analisis_distribucion.png` | Ranking y flujo de dinero con leyendas | Datos ilustrativos; no prueba ingresos reales. |

## Pantallas sin captura de contenido

| Vista | Motivo |
|---|---|
| Presentación inicial | Estado transitorio; no se reinició la sesión para capturarlo. |
| Inicio de sesión | La sesión autenticada existente se conservó. |
| Crear cuenta | No se cerró sesión ni se creó una cuenta. |
| Pago NFC | Se abrió por ADB y `dumpsys window windows` confirmó `SECURE` en la ventana de finANTzas. No se intentó eludir la protección ni producir un comprobante. |
| Dashboard de tableta | El emulador disponible mostraba el diseño de teléfono; no se cambiaron densidad, orientación ni configuración. |
| Editar perfil / CLP | No se abrió el editor de información personal ni se capturó la tarjeta de cuenta. |

Estas vistas sí tienen descripción basada en código en el manual. Permanecen pendientes su evidencia visual y, cuando corresponda, sus pruebas funcionales completas. También quedan pendientes los estados con movimientos guardados, confirmación de borrado, comprobante NFC y resultados de errores/sincronización.

## Funciones revisadas sin ejecutar escritura

Registro de gastos; clasificación automática y manual; filtros; eliminación con confirmación; restauración de datos demo; presupuestos; edición de perfil; cambio de apariencia; controles de notificaciones; proyecciones; preparación y anulación de token NFC; registro y acceso de cuentas; cierre de sesión; comentarios; tareas de sincronización.

La evidencia de consulta no valida el resultado de esas operaciones de escritura. Las pruebas instrumentadas existentes se refieren principalmente a contexto, seguridad, persistencia y rendimiento; no se identificó un recorrido UI completo listo para producir este manual. No se ejecutaron: algunas crean datos y no son compatibles con la restricción de preservar registros.

## Limitaciones y observaciones importantes

- El primer intento ADB falló por intentar crear `\\.android`. Se utilizó el entorno habitual de usuario en las consultas posteriores; no se cambió configuración del proyecto.
- UI Automator no proporcionó un árbol útil: hubo una respuesta `null root node` y otras respuestas sin contenido de nodos. Se pudo continuar mediante coordenadas y revisión de imágenes.
- El emulador demoró en actualizar algunos fotogramas. Se repitieron las capturas con esperas más amplias y se revisaron las imágenes finales para impedir asociaciones falsas.
- Los importes de demostración en Inicio, Análisis y Simulador no representan necesariamente movimientos existentes. El historial observado estaba vacío.
- NFC tiene una ruta alternativa a la biometría que también inserta un registro. Por ese motivo no se pulsó «Pagar Ahora» ni se validó la afirmación de que siempre exige una biometría correcta.
- Los interruptores de notificaciones modifican estado de interfaz; no se verificó entrega de notificaciones del sistema. Los comentarios se mantienen en memoria, sin envío remoto identificado.
- El nombre y la apariencia utilizan estado de aplicación; no se validó persistencia tras terminar el proceso.
- No se verificaron servidor, políticas remotas, operaciones bancarias, cumplimiento normativo ni recuperación de contraseña. No se incorporaron afirmaciones de funcionamiento basadas solamente en los textos de presentación.
- No se comparó el APK instalado con un hash de compilación del árbol fuente; coinciden paquete y versión, pero no se afirma identidad exacta de binarios. La evidencia del emulador prevalece para describir las capturas.

## Preservación y restricciones

Solo se crearon archivos de entrega dentro de `documentacion/`. No se editó código, configuración ni archivos de base de datos, no se instalaron dependencias y no se ejecutaron comandos SQL, limpieza de datos, reinstalaciones, restauraciones ni pruebas que creen movimientos. No se guardaron formularios, presupuestos, perfiles o comentarios; tampoco se confirmó ningún pago o borrado. No se hicieron commits ni push.

La aplicación contiene inicialización de presupuestos y tareas automáticas de sincronización al abrirse. No se alteraron esas funciones ni se inspeccionó la base de datos para certificar igualdad binaria antes y después. Por tanto, esta revisión garantiza que no se ordenaron modificaciones de registros, pero no certifica ausencia de escrituras internas realizadas automáticamente por la propia aplicación.

No se ejecutó compilación en esta tarea documental, para evitar generar archivos fuera del directorio autorizado. Las capturas provienen de la aplicación ya instalada.
