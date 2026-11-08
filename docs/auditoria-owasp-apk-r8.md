# Auditoria Android, APK release y evidencia para ClickUp

Fecha: 2026-11-08. Rama: `feature/auditoria-owasp-apk-r8`.

## Integracion local

Base de presupuestos: `ba29ce0`. Referencia local de main: `318f1d7`.
Se creo la rama desde presupuestos y se inicio `git merge --no-commit --no-ff origin/main`.
No hubo fetch: la referencia local coincide con el commit solicitado; no se certifica que siga siendo el ultimo remoto.
Conflicto textual: `app/build.gradle.kts`, resuelto conservando las dependencias de presupuestos, Hilt y pruebas.
Se corrigieron duplicados preexistentes en main: manifest, plugins, entidad, ViewModel, navegacion y graficos.
Se conservaron los calculadores, contratos y pruebas de main; Room version 2, SQLCipher, sincronizacion y presupuestos.
La integracion resuelta queda preparada en el indice, pendiente de commit autorizado. No se hizo commit, push ni Pull Request.
Los seis archivos locales no versionados que existian al comenzar se conservaron.

## Calidad y arquitectura

Primera ejecucion de Detekt: 82 hallazgos con severidad `error`; no eran simples warnings.
Se corrigieron imports, finales de archivo, constantes, longitud y responsabilidades de funciones.
No se desactivaron reglas ni se creo un baseline. Detekt y DetektDebug: 0 hallazgos finales.
Se separaron las tarjetas y el formulario de autenticacion para reducir responsabilidades de composables.
Se propaga CancellationException y se evita mostrar excepciones internas al usuario.
DIP parcial: los contratos y Hilt de main coexisten con repositorios Room y fabricas manuales.
El formulario mensual aun recibe RoomExpenseRepository concreto; no se certifica SOLID completo.
InMemoryExpenseRepository se conserva para compatibilidad con main; no sustituye la persistencia activa.

## Controles de seguridad

| Control | Evidencia / resultado | Limite de verificacion |
| --- | --- | --- |
| MASVS-STORAGE / CRYPTO, datos locales | SecureDatabaseProvider usa SupportOpenHelperFactory de SQLCipher y migracion 1 a 2 sin borrado destructivo | Pruebas instrumentadas de migracion, cifrado y persistencia indicadas abajo |
| Gestion de clave | AndroidKeystoreVault: 32 bytes con SecureRandom, AES-256-GCM, IV generado por Cipher, clave maestra AndroidKeyStore, persistencia cifrada atomica | Prueba de estabilidad y clave no exportable; no se exige ni certifica hardware/StrongBox en todos los dispositivos |
| Backup y transferencia | allowBackup=false; exclusiones explicitas de todos los dominios en cloud-backup y device-transfer, tambien formato anterior | Reglas revisadas; no se realizo restauracion en todos los fabricantes |
| Red / TLS | HTTPS y confianza en CAs del sistema conservados; Ktor/OkHttp rechazo una cadena no confiable en loopback (test aprobado) | No se intercepto Supabase ni se inspecciono su certificado real; no es prueba de todos los escenarios TLS |
| MASVS-NETWORK-2, pinning | No implementado por decision tecnica documentada para el endpoint gestionado actual | No se declara cumplimiento de NETWORK-2: si este control fuera obligatorio, sigue requiriendo pinning operativo o una excepcion formal aceptada |
| Sesiones | AuthGate espera inicializacion y observa cambios de sesion; contrasena de formulario se limpia tras autenticar | El SessionManager por defecto del SDK no tiene cifrado configurado por la app. Proteccion de tokens en disco pendiente de verificar y remediar |
| Autorizacion | Repositorios filtran usuario actual; presupuestos usan conflicto user_id/category y revisiones para conservar cambios pendientes | RLS existe en el esquema SQL versionado; politicas desplegadas y aislamiento remoto entre dos cuentas no comprobados |
| Componentes | Solo launcher exportado por la app; sin permisos de almacenamiento/camara/localizacion nuevos | Componentes aportados por librerias revisados en el manifest del APK |
| Release / resiliencia | AGP 9.4 usa optimization.enable=true: R8, ofuscacion y reduccion de recursos; reglas propias limitadas a metadatos | La ofuscacion no garantiza por si sola cumplimiento MASVS ni sustituye pruebas dinamicas |

No se declara aprobacion global OWASP MASVS. No se publico ni genero material de firma privado.
No se imprimieron credenciales ni se modifico local.properties.

## Subtarea 2: decision tecnica sobre pinning y prueba TLS aislada

### Decision para el endpoint actual

La configuracion inspeccionada utiliza un endpoint HTTPS gestionado de Supabase (`*.supabase.co`).
No se publican el identificador del proyecto, claves ni tokens. El cliente compartido de Auth y PostgREST
es Supabase SDK 3.2.3 sobre Ktor 3.2.3; el unico motor HTTP disponible es OkHttp, seleccionado mediante
su proveedor de motor. No se configura CertificatePinner ni pin-set.

Se conserva la validacion TLS ordinaria de la plataforma: cadena de confianza, vigencia del certificado
y nombre del servidor. Manifest y Network Security Config mantienen cleartextTrafficPermitted=false,
usesCleartextTraffic=false y trust-anchors limitados a `system`, sin TrustManager permisivo ni override
de hostname. HTTPS y pinning son controles diferentes; la ausencia de pinning no desactiva TLS.

Se decide no introducir pines para este endpoint gestionado en el alcance actual:

1. El equipo configura el proyecto Supabase, pero no administra la clave privada TLS, la emision ni
   la rotacion del certificado del servicio gestionado. No se ha proporcionado un compromiso operativo
   del proveedor que garantice continuidad de claves SPKI y claves de respaldo bajo control del equipo.
2. Fijar el certificado hoja o extraer un pin de una conexion puntual crearia una dependencia no
   coordinada con el ciclo de actualizacion de la app. Una rotacion legitima podria bloquear registro,
   login y sincronizacion, incluso manteniendose valido el certificado de la nueva cadena.
3. Fijar una CA publica amplia no identifica exclusivamente el backend y tampoco elimina la necesidad
   de gestionar cambios de cadena. No se inventan pines de respaldo ni se aceptan certificados invalidos.
4. Android desaconseja el pinning general por su riesgo de incompatibilidad. Se mantiene PKI del sistema
   y rechazo de certificados no confiables. Esta decision aplica al endpoint gestionado actual; debe
   revisarse si cambia el dominio/proveedor, el modelo de amenazas o la exigencia contractual de pinning.

Riesgo residual de la decision tecnica: se confia en la PKI del sistema; una cadena fraudulenta
aceptada por una CA comprometida no seria rechazada por pines, porque no existen. El ensayo autofirmado
no demuestra proteccion frente a ese escenario.

MASVS-NETWORK-2 pide identity pinning para endpoints bajo control del desarrollador. Esta decision no
constituye una aprobacion de dicho control ni una exencion automatica por usar Supabase: si se exige
cumplimiento de NETWORK-2 para este endpoint, falta que el responsable de seguridad acepte formalmente
la justificacion o proporcione un endpoint y claves gestionables, pines SPKI verificados de actual/respaldo,
rotacion y pruebas. No se marca ninguna tarjeta de ClickUp desde esta auditoria.

Fuentes: [Android: riesgos de pinning](https://developer.android.com/privacy-and-security/security-ssl#RestrictingTrust),
[MASVS-NETWORK-2](https://mas.owasp.org/MASVS/controls/MASVS-NETWORK-2/),
[Ktor: seleccion del motor](https://ktor.io/docs/client-engines.html).

Ultima verificacion de almacenamiento ya ejecutada: 4 pruebas instrumentadas, 0 fallos/errores/omitidas,
Pixel_6 Android 15 (API 35), registro JUnit 2026-11-08T17:54:15 UTC. Incluye KeystoreSecurityTest (1),
BudgetPersistenceTest (2) y EncryptedExpensePerformanceTest (1). Maximo de insercion 30,11 ms frente
al limite conservado de 50 ms. La repeticion de Keystore que figuraba pendiente en la evidencia
historica anterior quedo resuelta en esta ejecucion. Evidencia: `docs/evidencias/subtarea2-almacenamiento.xml`.

### Prueba controlada: alcance y estado

Plan de prueba: servidor TLS efimero ligado exclusivamente a 127.0.0.1 y puerto asignado por el sistema;
certificado autofirmado de prueba vigente y clave efimera independiente de la clave SQLCipher. Cliente
nuevo Ktor/OkHttp con validadores TLS por defecto y la politica de red de la aplicacion, sin instanciar
SupabaseProvider, enviar credenciales, instalar CAs ni contactar servicios externos.
La evidencia debe mostrar SSLHandshakeException y causa de validacion de certificados; un timeout,
rechazo TCP o error DNS no se aceptan como prueba de rechazo TLS. Los recursos se eliminan al terminar.
El certificado de laboratorio no representa ni se incorpora al endpoint de produccion.

Implementacion autorizada: `app/src/androidTest/java/cl/finantzas/app/data/repository/TlsCertificateValidationTest.kt`.
No se agregaron dependencias ni se modificaron fuentes de produccion, manifest, Network Security Config,
SupabaseProvider o certificados reales. El test crea una identidad EC efimera con un alias unico de
laboratorio en AndroidKeyStore, sin exportar la clave y sin usar el alias de SQLCipher. Esta identidad
sirve exclusivamente al servidor loopback: no se agrega a ningun almacen de confianza ni a la app.
El certificado se comprueba vigente y con firma propia valida antes del ensayo. El cliente mantiene sus
validadores por defecto y debe fallar por cadena no confiable (SSLHandshakeException con causa
CertPathValidatorException), antes de poder aceptar una respuesta HTTP. Un fallo por nombre, timeout,
DNS o conexion rechazada no satisface las aserciones. El servidor tambien debe observar el aborto TLS.
Al terminar se cierran sockets, cliente y executor, se elimina la identidad efimera y se comprueba su ausencia.
No se prueban aqui todos los casos de expiracion, hostname o MITM contra produccion.

Resultado: APROBADO en Pixel_6 (AVD), Android 15/API 35. JUnit registra 1 prueba, 0 fallos,
0 errores y 0 omitidas; caso JUnit de 4,655 s, timestamp 2026-11-08T19:05:09 UTC. Gradle: BUILD SUCCESSFUL en 2 min 11 s.
Aserciones aprobadas: motor real OkHttp, certificado de laboratorio vigente y autofirmado correctamente,
SSLHandshakeException con causa CertPathValidatorException, aborto observado por el servidor y
eliminacion de la identidad efimera. Se distingue un rechazo criptografico de un fallo de conectividad.

Comando reproducible en PowerShell:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=cl.finantzas.app.data.repository.TlsCertificateValidationTest' --no-configuration-cache --console=plain --max-workers=1
```

Evidencia conservada:

- [Resultado JUnit TLS](evidencias/tls-loopback.xml).
- [Marcas filtradas del test](evidencias/tls-loopback.txt), sin datos de usuario, claves o tokens.
- JUnit SHA-256: `4c01ba8f9be2eea2e6c0344aee5895c8cd395228c72a5bf7531106e7c58c3f63`.
- Marcas SHA-256: `4e2427f497d339c7daf2414c6615eb30ad2f9651e17663392f441d10d23d4435`.
- `TLS_AUDIT_RESULT=REJECTED_UNTRUSTED_CERTIFICATE`.
- `TLS_AUDIT_ENGINE=Ktor_OkHttp_DEFAULT_TLS; ENDPOINT=LOOPBACK_ONLY`.
- `TLS_AUDIT_CLEANUP=EPHEMERAL_IDENTITY_REMOVED; ROOT_CA_INSTALLED=false`.

No se modificaron codigo productivo, cliente Supabase, certificados reales, configuracion Gradle,
AndroidManifest ni Network Security Config. No se instalaron raices, no se desactivo validacion TLS
y no se accedio a Room ni a datos financieros desde esta prueba. No se exporto material de clave.
Los warnings de la opcion experimental de AGP, el objetivo JVM de las herramientas y Unsafe de protobuf
no fueron fallos de la prueba. No se hizo commit ni push.

### Conclusion de la subtarea 2

Puede considerarse completada la verificacion tecnica solicitada: cifrado Room/SQLCipher y proteccion
Keystore comprobados, transporte identificado, configuracion HTTPS revisada, rechazo TLS demostrado
y decision de no implementar CertificatePinner para el endpoint gestionado justificada y registrada.
No hay bloqueo tecnico pendiente en ese alcance y no se cambia el estado de ninguna tarjeta automaticamente.

Esto NO significa que CertificatePinner este implementado ni que MASVS-NETWORK-2 se haya aprobado.
Si el criterio de aceptacion del proyecto exige expresamente pinning, ese control sigue no implementado
y la justificacion no sustituye su cumplimiento: faltarian pines gestionables/verificados y rotacion,
o la aceptacion formal de la decision por el responsable del alcance. Tampoco se afirma aprobacion
MASVS global ni prueba funcional conjunta del release firmado.


## Evidencia de ejecucion

- `:app:testDebugUnitTest`: 23 pruebas, 0 fallos, 0 errores, 0 omitidas.
- `:app:assembleRelease`: APK generado; tarea `:app:minifyReleaseWithR8` ejecutada. La segunda invocacion verifica que el artefacto esta actualizado.
- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, 24.042.317 bytes.
- SHA-256: `6F68F0A113A983CDA047CDF9232150182F1EA6B6D08D3B9558B19B99A04EFA5B`.
- Integridad ZIP/CRC correcta, un archivo DEX, minSdk 26 y targetSdk 37.
- `zipalign -c -P 16 4`: codigo 0. Segmentos ELF de SQLCipher alineados a 16 KB en arm64-v8a, armeabi-v7a, x86 y x86_64; no sustituye prueba en dispositivo de 16 KB.
- `apksigner verify`: NO verifica porque el release no tiene firma (Missing META-INF/MANIFEST.MF). No es un APK listo para instalar/distribuir.
- Manifest binario: sin debuggable habilitado, allowBackup=false, usesCleartextTraffic=false y networkSecurityConfig aplicado. Servicios/receptores exportados de WorkManager/ProfileInstaller protegidos por BIND_JOB_SERVICE o DUMP.
- `app/build/outputs/mapping/release/configuration.txt`: sin dontshrink/dontoptimize/dontobfuscate; incluye reglas consumer Room/SQLCipher/WorkManager.
- `mapping.txt`: Room `_Impl` conserva nombre; BudgetLimitEntityClp, ExpenseRemoteDto y BudgetViewModel estan ofuscados.
- Lint vital release termino sin errores fatales. No se ejecuto una auditoria completa de todas las reglas Lint.
- Configuracion local: endpoint HTTPS y clave de tipo publico para cliente; solo se comprobaron booleanos, sin imprimir valores.
- Warnings de herramientas: opcion experimental android.disallowKotlinSourceSets, fallback de objetivo JVM en herramientas Kotlin y simbolos nativos que no se pueden eliminar. No se confundieron con aprobacion de controles.
- `:app:connectedDebugAndroidTest`: 5 pruebas, 0 fallos/errores/omitidas en Pixel_6 (Android 15). Incluye BudgetPersistenceTest (2), EncryptedExpensePerformanceTest, KeystoreSecurityTest y ExampleInstrumentedTest.
- Benchmark debug: inicializacion 738,67 ms, primera escritura 4,58 ms; 20 inserciones posteriores, promedio 2,90 ms y maximo 4,50 ms, limite conservado de 50 ms. No implica garantia de rendimiento en todo dispositivo ni en release.
- Evidencia JUnit conservada: `docs/evidencias/instrumentacion-debug.xml` y `docs/evidencias/unitarias-debug.xml`.
- Reportes: `app/build/reports/detekt/`, `app/build/test-results/testDebugUnitTest/` y `app/build/outputs/androidTest-results/connected/debug/`.
- `:app:detekt` y `:app:detektDebug`: 0 hallazgos. Se incluyen las clases Java generadas (BuildConfig) sin reemplazar las dependencias Android; resolucion diferida para no resolver debugCompileClasspath durante configuracion.
- Evidencia conservada de Detekt: `docs/evidencias/detekt.xml` y `docs/evidencias/detekt-debug.xml`.
- Validacion final sin dispositivo: `:app:detekt :app:detektDebug :app:testDebugUnitTest :app:assembleRelease --continue --max-workers=2`: BUILD SUCCESSFUL. Sin errores de resolucion de tipos ni warning de resolucion anticipada de debugCompileClasspath.
- La prueba Keystore se ajusto para que una comparacion fallida no imprima bytes de la clave. Compilo correctamente. La suite completa anterior (5/5) esta conservada, pero la reejecucion aislada posterior NO se valida: Gradle/ADB agoto el tiempo consultando propiedades y reporto Unknown API Level en dos intentos. ADB directo reporto Process crashed y el evento de sistema indico bg anr; no se atribuye causa al codigo sin mas evidencia.
- Historico, resuelto en la verificacion de almacenamiento 17:54:15 UTC indicada en subtarea 2: el emulador habia bloqueado la repeticion de Keystore. No se reinicio, borro ni restauro automaticamente.
- Comando de repeticion: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.finantzas.app.data.repository.KeystoreSecurityTest`.
- Archivos principales: configuracion Gradle/catalogo y reglas release; manifest/XML de seguridad; autenticacion, worker y funciones de tarjetas; prueba Keystore y evidencias. Cambios adicionales corresponden a integracion de main y correcciones de Detekt.

## Subtarea 3: APK debug instalable y release con R8

Se verifico `optimization { enable = true }` en release (DSL AGP 9.4). El artefacto anterior,
el mapping y la configuracion efectiva demuestran ofuscacion y ausencia de dontshrink/dontoptimize/dontobfuscate.
Nueva comprobacion: `:app:assembleDebug :app:assembleRelease --console=plain --max-workers=1`,
BUILD SUCCESSFUL en 12 s; `minifyReleaseWithR8` UP-TO-DATE y ambos assemble satisfactorios.
No se forzo recompilacion de R8 ni se modificaron funcionalidades o configuraciones.

| Propiedad | Debug utilizado en el emulador | Release optimizado |
| --- | --- | --- |
| Archivo | app/build/outputs/apk/debug/app-debug.apk | app/build/outputs/apk/release/app-release-unsigned.apk |
| Tamano | 45.924.532 bytes | 24.042.317 bytes |
| DEX | 17 | 1 |
| R8 | No habilitado por la configuracion debug existente | Activo en release |
| Firma | Debug existente, valida (v2) | Sin firma de release configurada |
| Instalable en esta verificacion | Si: instalacion -r exitosa | No se instalo ni se presenta como APK firmado |
| Depuracion | debuggable=true | debuggable no habilitado |

`signingReport` confirma el keystore debug existente (`~/.android/debug.keystore`, AndroidDebugKey)
y release sin signingConfig. No se creo keystore de release, certificados ni nuevas claves de firma.
Se conservaron allowBackup=false, usesCleartextTraffic=false y Network Security Config en ambos manifest binarios.
Los SHA-256 actuales y metadatos estan en [subtarea3-apks.json](evidencias/subtarea3-apks.json).
El hash del release aqui es una nueva captura del artefacto empaquetado; los hashes anteriores del informe
pertenecen a las verificaciones historicas y no se sustituyen como si fueran el mismo archivo.
El debug permite validar funcionamiento, pero no demuestra por si mismo el comportamiento despues de R8.
SHA-256 del debug entregable: `8A7694017E3973089E827521FBCAB9E21B20FA7AD6CC2A2B64A09D5251425190`.
SHA-256 del release verificado ahora: `3794A78DA265E0149FF2DFE5506078942B296A898F8D93EFCE325CF53AFEBDBF`.

### Instalacion y arranque

Pixel_6, Android 15/API 35: `adb install -r` devolvio Success. No se uso uninstall, pm clear, wipe ni
restauracion de datos. El paquete no tenia ruta instalada antes de este paso; no se infiere que el resto
del emulador estuviera vacio. Se conserva cualquier dato existente mediante la instalacion con -r.
`am start -W` devolvio Status: ok, LaunchState: COLD, TotalTime 25.913 ms y WaitTime 26.248 ms.
Se comprobo MainActivity en primer plano y proceso activo, con la pantalla de inicio de sesion visible.
Estos tiempos son mediciones observadas, no una aprobacion de rendimiento.
Evidencia sin campos ni credenciales: [subtarea3-arranque.txt](evidencias/subtarea3-arranque.txt).

### Prueba funcional y cierre

Estado actual: pendiente de sesion autenticada. Se solicito iniciar sesion directamente en el emulador
con una cuenta de pruebas, sin compartir la contrasena. No se elude AuthGate ni se extraen tokens.
Mientras no haya sesion, no se dan por probadas las pestanas internas, el registro de gastos ni la edicion
de presupuestos. No se ejecutan pruebas instrumentadas que puedan desinstalar la app despues de esta instalacion.
Comprobacion final de UI: continuan visibles Iniciar sesion e Ingresar; Registro/Alertas no estan accesibles.
No se registraron gastos ni se editaron presupuestos. No se sustituyen estos pasos por pruebas antiguas
ni por acceso directo a componentes que omita la autenticacion.
La generacion e instalacion debug y la comprobacion de R8 estan completas; el cierre de toda la subtarea 3
queda pendiente del ensayo funcional autenticado. No se marca ninguna tarjeta ni se afirma conformidad
con criterios adicionales de una pauta PRO403 que no haya sido proporcionada.

## Checklist para Jocelyn y Alvaro

Prueba conjunta NO realizada ni confirmada. Registrar dispositivo, version, participantes, fecha y resultado de cada punto.

- [ ] Configurar firma release fuera del repositorio: keystore existente, alias, storePassword y keyPassword mediante configuracion local o secretos de CI; no usar firma debug para distribucion.
- [ ] Instalar el APK release firmado y verificar arranque, autenticacion, restauracion de sesion y cierre.
- [ ] Navegar por las cinco pestanas; simulador y NFC conservan el alcance existente y no se declaran completos.
- [ ] Registrar gastos CLP reales en las cinco categorias; comprobar Room y conciliacion de KPIs/graficos.
- [ ] Verificar anillos por categoria y medio de pago, flujo proporcional y ranking RN-04.
- [ ] Editar los seis topes; comprobar formato, rechazo de cero/negativos/desbordamiento y persistencia tras reiniciar.
- [ ] Registrar un gasto y observar presupuesto global y categoria sin reiniciar; comprobar semaforo 75% y 100%.
- [ ] Actualizar una instalacion con Room version 1 y comprobar que la migracion conserva gastos.
- [ ] Probar offline, reconexion y cambios pendientes; confirmar ausencia de duplicados y que ediciones concurrentes locales no se pierden.
- [ ] Usar dos cuentas y comprobar aislamiento local y rechazo remoto por RLS en gastos/presupuestos.
- [ ] Verificar bloqueo de HTTP, rechazo de certificados no confiables y pinning cuando se configure con pines verificados.
- [ ] Revisar que backups/transferencias no incluyan base, vault ni tokens, y que los logs no contengan datos sensibles.
- [ ] Repetir el flujo completo con el APK optimizado firmado; comprobar JNI SQLCipher y serializacion Supabase despues de R8.
- [ ] Revisar compatibilidad de librerias nativas con dispositivos de paginas de 16 KB antes de distribuir.
- [ ] Adjuntar evidencias y confirmar con el equipo el estado de las 15 tarjetas antes de cerrar cualquiera.

## Tarjetas de ClickUp: informacion realmente disponible

No existe conexion disponible a ClickUp; la busqueda de integracion no devolvio un plugin.
No hay exportacion ni IDs de las 15 tarjetas. No se cambiaron estados.
Las siguientes son referencias de comentarios, no IDs ni estados confirmados del servicio:

| Referencia | Fuente disponible | Estado real en ClickUp |
| --- | --- | --- |
| T1.2 | Comentario de supabase/migrations/001_init_finantzas_rls.sql | No verificable |
| T2.2 | Mismo comentario SQL | No verificable |
| T3.3 | Mismo comentario SQL | No verificable |
| T4.3 | Mismo comentario SQL | No verificable |
| T1.3 | Comentario de AntExpenseRuleEngineTest.kt | No verificable |
| Conjunto de 15 tarjetas solicitado | No hay lista real para confirmar siquiera la correspondencia de las cinco referencias anteriores | No verificable |

La tarea de auditoria y sus cuatro subtareas son las descritas por el solicitante; no se infiere su correspondencia con esos codigos.
Para cerrar: obtener la lista real de 15 tarjetas, resolver los controles pendientes, configurar firma y confirmar la prueba conjunta.

## Referencias tecnicas

- [OWASP MASVS-NETWORK](https://mas.owasp.org/MASVS/08-MASVS-NETWORK/)
- [R8 y DSL AGP 9.3+](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
- [Seguridad de red](https://developer.android.com/privacy-and-security/security-config)
- [Backup y transferencia](https://developer.android.com/identity/data/autobackup)
- [Configuracion de sesiones, SDK 3.2.3](https://github.com/supabase-community/supabase-kt/blob/3.2.3/Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/AuthConfig.kt)
