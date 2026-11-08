package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.data.repository.budgetDefaults
import cl.finantzas.app.workers.ExpenseSyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MonthlyExpenseEntry(repository: RoomExpenseRepository, userId: String) {
    var concept by remember(userId) { mutableStateOf("") }
    var amount by remember(userId) { mutableStateOf("") }
    var category by remember(userId) { mutableStateOf("FOOD_AND_GROCERIES") }
    var expanded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember(userId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(concept, { concept = it }, label = { Text("Concepto del gasto") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it }, label = { Text("Gasto mensual CLP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        Box {
            TextButton(onClick = { expanded = true }) { Text(budgetDefaults.getValue(category).first) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                budgetDefaults.filterKeys { it != "GLOBAL" }.forEach { (key, value) ->
                    DropdownMenuItem(text = { Text(value.first) }, onClick = { category = key; expanded = false })
                }
            }
        }
        Button(enabled = !saving, onClick = {
            val clp = amount.toLongOrNull()
            if (clp == null || clp <= 0 || concept.trim().length !in 2..160) {
                message = "Ingresa un concepto de 2 a 160 caracteres y un monto entero positivo"
            } else {
                val row = ExpenseEntityClp(UUID.randomUUID().toString(), userId, concept.trim(), clp, clp,
                    0, "PURCHASE", category, "CASH", category == "ENTERTAINMENT_SUBSCRIPTIONS",
                    if (category == "ENTERTAINMENT_SUBSCRIPTIONS") "RN-03" else "ESSENTIAL",
                    "Gasto mensual registrado", System.currentTimeMillis())
                saving = true
                scope.launch {
                    try {
                        repository.insert(row)
                        concept = ""; amount = ""; message = "Gasto guardado"
                        if (userId != LOCAL_EXPENSE_USER) ExpenseSyncScheduler.schedule(context)
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { message = "No se pudo completar el registro o programar su sincronización" }
                    finally { saving = false }
                }
            }
        }) { Text("Registrar gasto mensual") }
        message?.let { Text(it) }
    }
}
