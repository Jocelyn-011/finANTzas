package cl.finantzas.app.domain.repository

import cl.aiep.finantzas.domain.rules.ExpenseCandidate

/**
 * Contrato de SOLO ESCRITURA de gastos.
 *
 * Separado del lector para que cada consumidor dependa únicamente
 * de lo que realmente usa (principio ISP).
 */
interface ExpenseWriterRepository {

    /**
     * Registra un nuevo gasto.
     *
     * @throws IllegalArgumentException si el gasto no cumple las validaciones de entrada.
     */
    suspend fun addExpense(candidate: ExpenseCandidate)
}
