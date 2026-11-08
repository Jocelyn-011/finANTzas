package cl.finantzas.app.data.demo

import cl.finantzas.app.data.local.entity.ExpenseEntityClp

object ExpenseDemoDataClp {
    private const val COFFEE_MONTHLY_CLP = 3500L
    private const val STREAMING_MONTHLY_CLP = 26900L
    private const val RENT_MONTHLY_CLP = 380000L
    private const val TRANSPORT_MONTHLY_CLP = 42000L

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
            expense("Cafe", COFFEE_MONTHLY_CLP, "Alimentacion", true),
            expense("Streaming", STREAMING_MONTHLY_CLP, "Entretenimiento", false),
            expense("Arriendo", RENT_MONTHLY_CLP, "Vivienda", false),
            expense("Bip", TRANSPORT_MONTHLY_CLP, "Transporte", false)
        )
    }
}
