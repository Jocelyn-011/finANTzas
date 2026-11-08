package cl.finantzas.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada central del ciclo de vida de la aplicación finANTzas.
 *
 * [HiltAndroidApp] activa el grafo de dependencias de la aplicacion.
 * de Dagger Hilt a nivel de aplicación (SingletonComponent), permitiendo que los componentes,
 * ViewModels y repositorios sean inyectados de forma segura y desacoplada.
 */
@HiltAndroidApp
class FinantzasApplication : Application()
