# finANTzas

**Aplicación móvil para el control de gastos y ahorro del hogar**

## Descripción

finANTzas es un proyecto académico desarrollado para facilitar el registro de gastos personales y del hogar, identificar gastos hormiga y promover mejores hábitos de ahorro.

La aplicación está desarrollada en Kotlin utilizando Jetpack Compose y Material Design 3.

## Funcionalidades

- Autenticación, registro de gastos en CLP y clasificación de gastos hormiga mediante un motor de reglas.
- Indicadores, gráficos de anillos y Sankey, y simulador de ahorro con escenarios del 25 %, 50 % y 100 %.
- Presupuestos por categoría y alertas de gasto.
- Billetera NFC de demostración, con componentes HCE, biometría y firma HMAC. No se han validado pagos bancarios reales.
- Persistencia local y sincronización de gastos con Supabase.

## Tecnologías utilizadas

- Kotlin
- Android Studio
- Jetpack Compose
- Material Design 3
- Navigation Compose
- Room, SQLCipher y WorkManager
- Supabase y Dagger Hilt
- Gradle
- Git y GitHub

## Equipo de desarrollo

Proyecto académico AIEP — Grupo 1.

- Jocelyn González
- Álvaro Brizuela
- Enrique Ahumada

## Instalación y ejecución

1. Clonar el repositorio:

   `git clone https://github.com/Jocelyn-011/finANTzas.git`

2. Abrir la carpeta del proyecto en Android Studio.
3. Configurar la URL y la clave publicable de Supabase en `local.properties` con los nombres `SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY`.
4. Esperar la sincronización de Gradle y ejecutar la aplicación en un dispositivo o emulador con Android 8.0 (API 26) o superior.

## Organización del repositorio

- `main` contiene la versión principal; los cambios se integran desde ramas de trabajo mediante pull requests.
