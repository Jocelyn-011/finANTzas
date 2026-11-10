package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.finantzas.app.data.demo.ExpenseDemoDataClp
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import cl.finantzas.app.workers.ExpenseSyncScheduler
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ExpenseDemoScreen() {
    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val database = remember(context) { SecureDatabaseProvider.getDatabase(context) }
    val repository = remember(database) { RoomExpenseRepository(database.expenseDao()) }
    val viewModel = remember(repository, userId) {
        ExpenseViewModel(
            repository = repository,
            userId = userId,
            budgetLimitsFlow = database.budgetLimitDao().observe(userId)
        )
    }
    ExpenseDemoScreen(viewModel = viewModel)
}

@Composable
fun ExpenseDemoScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val expenses by viewModel.expenses.collectAsState()
    val scope = rememberCoroutineScope()
    val ruleEngine = remember { AntExpenseRuleEngine() }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var pendingDelete by remember { mutableStateOf<ExpenseEntityClp?>(null) }
    var operationError by remember { mutableStateOf<String?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf<String?>(null) }

    val filteredExpenses = remember(expenses, selectedFilter) {
        filterExpenses(expenses, selectedFilter)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mitad Superior: Tarjeta Registrar Nuevo Gasto
        item {
            ExpenseRegistrationForm { candidate, manuallyConfirmedAsAnt ->
                val verdict = ruleEngine.evaluate(candidate)
                val finalIsAnt = manuallyConfirmedAsAnt
                val expense = ExpenseEntityClp(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    concept = candidate.concept,
                    unitAmountClp = candidate.unitAmountClp,
                    monthlyImpactClp = verdict.monthlyImpactClp,
                    weeklyFrequency = candidate.weeklyFrequency,
                    expenseType = candidate.type.name,
                    category = candidate.category.name,
                    paymentMethod = candidate.paymentMethodCode,
                    isAntExpense = finalIsAnt,
                    triggeredRuleCode = when {
                        finalIsAnt && verdict.isAntExpense -> verdict.triggeredRuleCode
                        finalIsAnt -> "RN-01"
                        else -> "ESSENTIAL"
                    },
                    ruleReason = when {
                        finalIsAnt && verdict.isAntExpense -> verdict.description.orEmpty()
                        finalIsAnt -> "Gasto hormiga confirmado manualmente (RN-01)."
                        else -> "Clasificado como gasto esencial."
                    },
                    createdAt = System.currentTimeMillis()
                )
                repository.insert(expense)
                if (userId != LOCAL_EXPENSE_USER) ExpenseSyncScheduler.schedule(context)
                null
            }
        }

        // Mitad Inferior: Historial de Pagos
        item {
            Text(
                text = "Historial de Pagos (${filteredExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            ExpenseMovementFilterChips(
                selectedKey = selectedFilter,
                onSelected = { selectedFilter = it }
            )
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Text(
                    if (expenses.isEmpty()) "Todavía no hay gastos. Registra uno arriba para comenzar."
                    else "No hay movimientos que coincidan con este filtro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        } else {
            items(filteredExpenses, key = ExpenseEntityClp::id) { expense ->
                ExpenseMovementCard(
                    expense = expense,
                    onToggleClassification = {
                        val updatedAntStatus = !expense.isAntExpense
                        val updated = expense.copy(
                            isAntExpense = updatedAntStatus,
                            triggeredRuleCode = if (updatedAntStatus) "RN-01" else "ESSENTIAL",
                            ruleReason = if (updatedAntStatus) {
                                "Gasto hormiga confirmado manualmente en la lista (RN-01)."
                            } else {
                                "Marcado manualmente como gasto esencial."
                            }
                        )
                        scope.launch {
                            operationError = try {
                                repository.update(updated)
                                if (userId != LOCAL_EXPENSE_USER) {
                                    ExpenseSyncScheduler.schedule(context)
                                }
                                null
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                error.message ?: "No se pudo actualizar el gasto."
                            }
                        }
                    },
                    onDelete = { pendingDelete = expense }
                )
            }
        }

        if (operationError != null) {
            item {
                Text(
                    text = operationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (!restoring) {
                            restoring = true
                            restoreError = null
                            scope.launch {
                                try {
                                    dao.restoreDemoExpenses(
                                        userId,
                                        ExpenseDemoDataClp.create(userId)
                                    )
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    restoreError = "No se pudieron restaurar los datos de ejemplo."
                                } finally {
                                    restoring = false
                                }
                            }
                        }
                    },
                    enabled = !restoring && userId == LOCAL_EXPENSE_USER,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (restoring) "Restaurando..." else "Restaurar datos de ejemplo CLP")
                }
                restoreError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar gasto") },
            text = { Text("¿Quieres eliminar «${expense.concept}» del historial?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            operationError = try {
                                repository.delete(expense)
                                null
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                error.message ?: "No se pudo eliminar el gasto."
                            }
                            pendingDelete = null
                        }
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
