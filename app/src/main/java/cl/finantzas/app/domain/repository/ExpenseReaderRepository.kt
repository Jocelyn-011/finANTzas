package cl.finantzas.app.domain.repository

import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de SOLO LECTURA de gastos.
 *
 * Quien solo necesita leer (por ejemplo, el ViewModel del dashboard) no
 * depende de operaciones de escritura (principio ISP).
 */
interface ExpenseReaderRepository {

    /** Flujo reactivo con la lista actual de gastos registrados (montos en CLP como Long). */
    fun observeExpenses(): Flow<List<ExpenseCandidate>>
}