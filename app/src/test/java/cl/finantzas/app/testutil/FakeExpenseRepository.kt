package cl.finantzas.app.testutil

import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.finantzas.app.domain.repository.ExpenseReaderRepository
import cl.finantzas.app.domain.repository.ExpenseWriterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Doble de prueba (Fake) que implementa los mismos contratos que el repositorio real.
 * Permite simular errores de validación sin depender de las reglas del repositorio real.
 */
class FakeExpenseRepository : ExpenseReaderRepository, ExpenseWriterRepository {

    private val expenses = MutableStateFlow<List<ExpenseCandidate>>(emptyList())

    /** Si no es null, la próxima escritura falla con este mensaje. */
    var validationError: String? = null

    override fun observeExpenses(): Flow<List<ExpenseCandidate>> = expenses

    override suspend fun addExpense(candidate: ExpenseCandidate) {
        val error = validationError
        require(error == null) { error.orEmpty() }
        expenses.update { current -> current + candidate }
    }
}
