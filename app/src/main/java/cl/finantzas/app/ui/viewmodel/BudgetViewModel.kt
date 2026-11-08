package cl.finantzas.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.finantzas.app.data.repository.BudgetRepository
import cl.finantzas.app.data.repository.ExpenseReaderRepository
import cl.finantzas.app.data.repository.budgetDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

 data class BudgetCardState(val category: String, val title: String, val limit: Long, val spent: Long) {
    val fraction: Float get() = (spent.toDouble() / limit).toFloat().coerceIn(0f, 1f)
    val percentage: Double get() = spent.toDouble() / limit * 100
}

class BudgetViewModel(
    private val repository: BudgetRepository,
    expenses: ExpenseReaderRepository,
    private val userId: String,
    private val scheduleSync: () -> Unit
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    val status = message.asStateFlow()
    val cards = combine(repository.observe(userId), expenses.getExpensesByUser(userId)) { limits, rows ->
        budgetDefaults.mapNotNull { (category, default) ->
            limits.firstOrNull { it.category == category }?.let { limit ->
                BudgetCardState(category, default.first, limit.monthlyLimitClp,
                    rows.filter { category == "GLOBAL" || it.category == category }.sumOf { it.monthlyImpactClp })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    init { viewModelScope.launch {
        try { repository.initialize(userId); scheduleSync() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { message.value = "No se pudieron cargar los presupuestos" }
    } }
    fun save(category: String, text: String) {
        val amount = text.toLongOrNull()
        if (amount == null || amount <= 0) { message.value = "Ingresa un monto CLP entero mayor que cero"; return }
        viewModelScope.launch {
            try {
                repository.save(userId, category, amount)
                message.value = "Guardado en el dispositivo; sincronización pendiente de conexión y sesión"
                scheduleSync()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message.value = "No se pudo guardar el presupuesto" }
        }
    }
}
