package cl.finantzas.app.data.nfc

import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.ExpenseType
import cl.aiep.finantzas.domain.rules.PaymentInstrument
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Comercios ficticios configurados para el modelo de gastos periódicos. */
enum class NfcDemoMerchant(
    val displayName: String,
    val amountClp: Long,
    val category: ExpenseCategory,
    val type: ExpenseType
) {
    COFFEE("Cafetería Express", 3_200L, ExpenseCategory.MICRO_PURCHASES_AND_SNACKS, ExpenseType.PURCHASE),
    GAMING("Pase Gaming Cloud", 12_990L, ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS, ExpenseType.SUBSCRIPTION),
    GROCERY("Mercado Vecinal", 24_500L, ExpenseCategory.FOOD_AND_GROCERIES, ExpenseType.PURCHASE)
}

/** Se registra la frecuencia seleccionada por la persona, sin modificar los gastos existentes. */
fun createNfcDemoExpense(
    merchant: NfcDemoMerchant,
    weeklyFrequency: Int,
    userId: String,
    expenseId: String,
    createdAt: Long,
    engine: AntExpenseRuleEngine = AntExpenseRuleEngine()
): ExpenseEntityClp {
    require(weeklyFrequency in 0..7)
    val candidate = ExpenseCandidate(
        concept = "Simulación NFC: ${merchant.displayName}",
        unitAmountClp = merchant.amountClp,
        weeklyFrequency = weeklyFrequency,
        type = merchant.type,
        category = merchant.category,
        paymentMethodCode = PaymentInstrument.NfcMobileWallet.code
    )
    val verdict = engine.evaluate(candidate)
    return ExpenseEntityClp(
        id = expenseId, userId = userId, concept = candidate.concept,
        unitAmountClp = candidate.unitAmountClp, monthlyImpactClp = verdict.monthlyImpactClp,
        weeklyFrequency = weeklyFrequency, expenseType = candidate.type.name,
        category = candidate.category.name, paymentMethod = candidate.paymentMethodCode,
        isAntExpense = verdict.isAntExpense, triggeredRuleCode = verdict.triggeredRuleCode,
        ruleReason = "Simulación: " + verdict.description.orEmpty(), createdAt = createdAt
    )
}

/** Datos firmados en el dispositivo; todavía no hay un contrato de recepción web. */
@Serializable
data class NfcDemoPayloadClp(
    @SerialName("event_id") val eventId: String,
    @SerialName("created_at_ms") val createdAt: Long,
    val merchant: String,
    @SerialName("amount_clp") val amountClp: Long,
    @SerialName("weekly_frequency") val weeklyFrequency: Int,
    @SerialName("monthly_impact_clp") val monthlyImpactClp: Long,
    val category: String,
    @SerialName("expense_type") val expenseType: String,
    @SerialName("is_ant_expense") val isAntExpense: Boolean,
    @SerialName("rule_code") val ruleCode: String,
    @SerialName("payment_method") val paymentMethod: String,
    val simulated: Boolean = true
)

fun ExpenseEntityClp.toNfcDemoPayload(merchant: NfcDemoMerchant) = NfcDemoPayloadClp(
    eventId = id, createdAt = createdAt, merchant = merchant.displayName,
    amountClp = unitAmountClp, weeklyFrequency = weeklyFrequency,
    monthlyImpactClp = monthlyImpactClp, category = category, expenseType = expenseType,
    isAntExpense = isAntExpense, ruleCode = triggeredRuleCode, paymentMethod = paymentMethod
)

/** Sobre firmado localmente; no hay aún un servicio web que lo verifique. */
@Serializable
data class SignedNfcWebhookEnvelopeClp(
    @SerialName("payload_json") val payloadJson: String,
    val algorithm: String,
    @SerialName("signature_base64") val signatureBase64: String,
    @SerialName("public_key_base64") val publicKeyBase64: String
)
