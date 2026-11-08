package cl.finantzas.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.aiep.finantzas.domain.rules.ClpImpactCalculator
import cl.finantzas.app.domain.repository.ExpenseReaderRepository
import cl.finantzas.app.domain.repository.ExpenseWriterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel principal: combina los gastos del repositorio con el motor de reglas
 * y expone un estado inmutable en CLP para la UI.
 */
@HiltViewModel
class FinantzasViewModel @Inject constructor(
    private val ruleEngine: AntExpenseRuleEngine,
    private val expenseReader: ExpenseReaderRepository,
    private val expenseWriter: ExpenseWriterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinantzasUiStateClp())
    val uiState: StateFlow<FinantzasUiStateClp> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            expenseReader.observeExpenses().collect { candidates ->
                _uiState.update { current -> current.withEvaluatedExpenses(candidates) }
            }
        }
    }

    /** Evento de la UI: el usuario registra un nuevo gasto. */
    fun onAddExpense(candidate: ExpenseCandidate) {
        viewModelScope.launch {
            try {
                expenseWriter.addExpense(candidate)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /** Evento de la UI: el mensaje de error ya se mostró al usuario. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun FinantzasUiStateClp.withEvaluatedExpenses(
        candidates: List<ExpenseCandidate>
    ): FinantzasUiStateClp {
        val evaluated = candidates.map { candidate ->
            val verdict = ruleEngine.evaluate(candidate)
            EvaluatedExpenseClp(
                candidate = candidate,
                verdict = verdict,
                annualImpactClp = ClpImpactCalculator.annualImpactClp(verdict.monthlyImpactClp)
            )
        }
        val antExpenses = evaluated.filter { it.verdict.isAntExpense }

        return copy(
            expenses = evaluated,
            totalMonthlyImpactClp = ClpImpactCalculator.sumClp(evaluated.map { it.verdict.monthlyImpactClp }),
            totalAnnualImpactClp = ClpImpactCalculator.sumClp(evaluated.map { it.annualImpactClp }),
            antMonthlyImpactClp = ClpImpactCalculator.sumClp(antExpenses.map { it.verdict.monthlyImpactClp }),
            antAnnualImpactClp = ClpImpactCalculator.sumClp(antExpenses.map { it.annualImpactClp })
        )
    }
}
