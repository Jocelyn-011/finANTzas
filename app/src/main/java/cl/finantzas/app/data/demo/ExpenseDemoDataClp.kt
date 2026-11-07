package cl.finantzas.app.data.demo

import cl.finantzas.app.data.local.entity.ExpenseEntityClp


object ExpenseDemoDataClp {

    fun create(userId: String): List<ExpenseEntityClp> {
        val now = System.currentTimeMillis()

        fun expense(
            concept: String,
            amount: Long,
            frequency: Int,
            category: String,
            isAnt: Boolean
        ) = ExpenseEntityClp(
            id = "demo-" + concept.lowercase().replace(" ", "-"),
            userId = userId,
            concept = concept,
            unitAmountClp = amount,
            monthlyImpactClp = amount * frequency * 4L,
            weeklyFrequency = frequency,
            expenseType = "VARIABLE",
            category = category,
            paymentMethod = "DEBIT",
            isAntExpense = isAnt,
            triggeredRuleCode = "",
            ruleReason = "",
            createdAt = now
        )

        return listOf(
            expense("Cafe", 2500L, 5, "Alimentacion", true),
            expense("Transporte", 1500L, 5, "Transporte", false),
            expense("Colacion", 5500L, 5, "Alimentacion", false),
            expense("Streaming", 3000L, 1, "Entretenimiento", true),
            expense("Snack", 1800L, 3, "Alimentacion", true)
        )
    }
}
