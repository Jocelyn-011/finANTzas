package cl.finantzas.app.data.demo

import cl.finantzas.app.data.local.entity.ExpenseEntityClp

object ExpenseDemoDataClp {
    fun create(userId: String): List<ExpenseEntityClp> {
        val now = System.currentTimeMillis()

        fun expense(concept: String, monthlyAmount: Long, category: String, isAnt: Boolean) =
            ExpenseEntityClp(
                id = "demo-" + concept.lowercase().replace(" ", "-"),
                userId = userId,
                concept = concept,
                unitAmountClp = monthlyAmount,
                monthlyImpactClp = monthlyAmount,
                weeklyFrequency = 0,
                expenseType = "FIXED",
                category = category,
                paymentMethod = "DEBIT",
                isAntExpense = isAnt,
                triggeredRuleCode = "",
                ruleReason = "",
                createdAt = now
            )

        // Los valores de ClickUp se interpretan como montos mensuales de demostracion.
        return listOf(
            expense("Cafe", 3500L, "Alimentacion", true),
            expense("Streaming", 26900L, "Entretenimiento", false),
            expense("Arriendo", 380000L, "Vivienda", false),
            expense("Bip", 42000L, "Transporte", false)
        )
    }
}