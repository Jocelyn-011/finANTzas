package cl.finantzas.app.data.local.entity

import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.ExpenseType
import cl.aiep.finantzas.domain.rules.PaymentInstrument

/**
 * Datos semilla y transacciones demo oficiales del proyecto finANTzas.
 * Basado en las 8 transacciones validadas del prototipo funcional.
 */
object ExpenseDemoDataClp {

    val initialExpenses: List<ExpenseEntityClp> = listOf(
        ExpenseEntityClp(
            id = 1L,
            concept = "Café de grano camino a clases/trabajo",
            unitAmountClp = 3_500L,
            weeklyFrequency = 5,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS,
            paymentInstrument = PaymentInstrument.CreditCard,
            isAntExpense = true,
            ruleReason = "RN-02: Micro-compra repetida >3 veces/sem (5x/sem)"
        ),
        ExpenseEntityClp(
            id = 2L,
            concept = "Snacks y bebidas en minimarket",
            unitAmountClp = 2_800L,
            weeklyFrequency = 4,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS,
            paymentInstrument = PaymentInstrument.Cash,
            isAntExpense = true,
            ruleReason = "RN-02: Micro-compra repetida >3 veces/sem (4x/sem)"
        ),
        ExpenseEntityClp(
            id = 3L,
            concept = "Netflix + Spotify + Disney+ (Combo Streaming)",
            unitAmountClp = 26_900L,
            weeklyFrequency = 0,
            type = ExpenseType.SUBSCRIPTION,
            category = ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS,
            paymentInstrument = PaymentInstrument.CreditCard,
            isAntExpense = true,
            ruleReason = "RN-03: Suscripción de Entretenimiento"
        ),
        ExpenseEntityClp(
            id = 4L,
            concept = "Compra semanal de supermercado",
            unitAmountClp = 65_000L,
            weeklyFrequency = 1,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.FOOD_AND_GROCERIES,
            paymentInstrument = PaymentInstrument.DebitCard,
            isAntExpense = false,
            ruleReason = "Gasto Esencial"
        ),
        ExpenseEntityClp(
            id = 5L,
            concept = "Arriendo, luz, agua e internet hogar",
            unitAmountClp = 380_000L,
            weeklyFrequency = 0,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.FOOD_AND_GROCERIES,
            paymentInstrument = PaymentInstrument.BankTransfer,
            isAntExpense = false,
            ruleReason = "Gasto Esencial"
        ),
        ExpenseEntityClp(
            id = 6L,
            concept = "Viajes cortos en app de transporte por prisa",
            unitAmountClp = 4_500L,
            weeklyFrequency = 4,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS,
            paymentInstrument = PaymentInstrument.CreditCard,
            isAntExpense = true,
            ruleReason = "RN-02: Micro-compra repetida >3 veces/sem (4x/sem)"
        ),
        ExpenseEntityClp(
            id = 7L,
            concept = "Carga mensual tarjeta Bip! / Transporte público",
            unitAmountClp = 42_000L,
            weeklyFrequency = 0,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.FOOD_AND_GROCERIES,
            paymentInstrument = PaymentInstrument.DebitCard,
            isAntExpense = false,
            ruleReason = "Gasto Esencial"
        ),
        ExpenseEntityClp(
            id = 8L,
            concept = "Postre o antojo nocturno por delivery",
            unitAmountClp = 5_500L,
            weeklyFrequency = 4,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS,
            paymentInstrument = PaymentInstrument.NfcMobileWallet,
            isAntExpense = true,
            ruleReason = "RN-02: Micro-compra repetida >3 veces/sem (4x/sem)"
        )
    )
}