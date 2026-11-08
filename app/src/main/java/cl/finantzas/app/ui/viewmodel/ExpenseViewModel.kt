package cl.finantzas.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.ExpenseReaderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

class ExpenseViewModel(
    repository: ExpenseReaderRepository,
    userId: String
) : ViewModel() {
    val expenses: StateFlow<List<ExpenseEntityClp>> = repository.getExpensesByUser(userId)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalMonthlyClp: StateFlow<Long> = repository.getTotalMonthlyImpactClp(userId)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val expenses: StateFlow<List<ExpenseEntityClp>> =
        repository.getExpensesByUser(userId)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val totalMonthlyClp: StateFlow<Long> =
        repository.getTotalMonthlyImpactClp(userId)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0L
            )
}
