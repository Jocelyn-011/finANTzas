package cl.finantzas.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada central del ciclo de vida de la aplicación finANTzas.
 *
 * La anotación [@HiltAndroidApp](class://dagger.hilt.android.HiltAndroidApp) activa la generación del grafo de dependencias
 * de Dagger Hilt a nivel de aplicación (SingletonComponent), permitiendo que los componentes,
 * ViewModels y repositorios sean inyectados de forma segura y desacoplada.
 */
@HiltAndroidApp
class FinantzasApplication : Application()