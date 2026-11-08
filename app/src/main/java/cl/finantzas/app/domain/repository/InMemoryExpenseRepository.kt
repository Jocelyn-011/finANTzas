package cl.finantzas.app.domain.repository

import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.aiep.finantzas.domain.rules.FinancialThresholds
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria de los repositorios de gastos.
 *
 * Una sola instancia (@Singleton) atiende ambos contratos. Valida la entrada
 * con rangos cerrados (mínimo y máximo) para que ningún monto pueda
 * desbordar los cálculos en Long (OWASP: validación de entrada).
 */
@Singleton
class InMemoryExpenseRepository @Inject constructor() :
    ExpenseReaderRepository,
    ExpenseWriterRepository {

    private val expenses = MutableStateFlow<List<ExpenseCandidate>>(emptyList())

    override fun observeExpenses(): Flow<List<ExpenseCandidate>> = expenses.asStateFlow()

    override suspend fun addExpense(candidate: ExpenseCandidate) {
        require(candidate.concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(candidate.concept.length <= MAX_CONCEPT_LENGTH) {
            "El concepto supera los $MAX_CONCEPT_LENGTH caracteres"
        }
        require(candidate.unitAmountClp in MIN_UNIT_AMOUNT_CLP..MAX_UNIT_AMOUNT_CLP) {
            "El monto debe estar entre $MIN_UNIT_AMOUNT_CLP y $MAX_UNIT_AMOUNT_CLP CLP"
        }
        require(candidate.weeklyFrequency in FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE..MAX_WEEKLY_FREQUENCY) {
            "La frecuencia semanal debe estar entre 0 y $MAX_WEEKLY_FREQUENCY"
        }

        expenses.update { current -> current + candidate }
    }

    private companion object {
        const val MIN_UNIT_AMOUNT_CLP: Long = 1L
        const val MAX_UNIT_AMOUNT_CLP: Long = 100_000_000L
        const val MAX_WEEKLY_FREQUENCY: Int = 70
        const val MAX_CONCEPT_LENGTH: Int = 60
    }
}
