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
| Red / TLS | Sin cleartext y confianza explicita solo en CAs del sistema; dependencia Ktor OkHttp y cliente Supabase compartido | No se realizo prueba MITM ni inspeccion del certificado del servidor |
| MASVS-NETWORK-2, pinning | No hay CertificatePinner ni pin-set en las conexiones de autenticacion o PostgREST | PENDIENTE: propietario debe aportar host, pines SPKI actual/respaldo verificados y plan de rotacion; no se inventaron pines |
| Sesiones | AuthGate espera inicializacion y observa cambios de sesion; contrasena de formulario se limpia tras autenticar | El SessionManager por defecto del SDK no tiene cifrado configurado por la app. Proteccion de tokens en disco pendiente de verificar y remediar |
| Autorizacion | Repositorios filtran usuario actual; presupuestos usan conflicto user_id/category y revisiones para conservar cambios pendientes | RLS existe en el esquema SQL versionado; politicas desplegadas y aislamiento remoto entre dos cuentas no comprobados |
| Componentes | Solo launcher exportado por la app; sin permisos de almacenamiento/camara/localizacion nuevos | Componentes aportados por librerias revisados en el manifest del APK |
| Release / resiliencia | AGP 9.4 usa optimization.enable=true: R8, ofuscacion y reduccion de recursos; reglas propias limitadas a metadatos | La ofuscacion no garantiza por si sola cumplimiento MASVS ni sustituye pruebas dinamicas |

No se declara aprobacion global OWASP MASVS. No se publico ni genero material de firma privado.
No se imprimieron credenciales ni se modifico local.properties.

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
- Pendiente imprescindible: estabilizar/reiniciar el emulador con el equipo y repetir KeystoreSecurityTest. No se reinicio, borro ni restauro el dispositivo automaticamente.
- Comando de repeticion: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.finantzas.app.data.repository.KeystoreSecurityTest`.
- Archivos principales: configuracion Gradle/catalogo y reglas release; manifest/XML de seguridad; autenticacion, worker y funciones de tarjetas; prueba Keystore y evidencias. Cambios adicionales corresponden a integracion de main y correcciones de Detekt.

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
