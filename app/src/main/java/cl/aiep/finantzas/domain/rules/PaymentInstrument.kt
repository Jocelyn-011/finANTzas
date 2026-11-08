package cl.aiep.finantzas.domain.rules

/**
 * Contrato polimórfico para instrumentos de pago en finANTzas (Principio de Sustitución de Liskov - LSP).
 *
 * Cualquier subtipo puede sustituir a [PaymentInstrument] de forma uniforme e indistinta en los cálculos
 * analíticos sin alterar el contrato matemático ni requerir downcasting manual.
 */
sealed interface PaymentInstrument {
    val code: String
    val displayName: String

    data object CreditCard : PaymentInstrument {
        override val code: String = CODE_CREDIT_CARD
        override val displayName: String = "Tarjeta de Crédito"
    }

    data object DebitCard : PaymentInstrument {
        override val code: String = CODE_DEBIT_CARD
        override val displayName: String = "Tarjeta de Débito"
    }

    data object NfcMobileWallet : PaymentInstrument {
        override val code: String = CODE_NFC_WALLET
        override val displayName: String = "Billetera NFC"
    }

    data object Cash : PaymentInstrument {
        override val code: String = CODE_CASH
        override val displayName: String = "Efectivo"
    }

    data object BankTransfer : PaymentInstrument {
        override val code: String = CODE_TRANSFER
        override val displayName: String = "Transferencia"
    }

    companion object {
        const val CODE_CREDIT_CARD: String = "CREDIT_CARD"
        const val CODE_DEBIT_CARD: String = "DEBIT_CARD"
        const val CODE_NFC_WALLET: String = "NFC_MOBILE_WALLET"
        const val CODE_CASH: String = "CASH"
        const val CODE_TRANSFER: String = "BANK_TRANSFER"

        fun fromCode(code: String): PaymentInstrument {
            return when (code.trim().uppercase()) {
                CODE_CREDIT_CARD, "TARJETA DE CRÉDITO", "TARJETA DE CREDITO" -> CreditCard
                CODE_DEBIT_CARD, "TARJETA DE DÉBITO", "TARJETA DE DEBITO" -> DebitCard
                CODE_NFC_WALLET, "BILLETERA NFC" -> NfcMobileWallet
                CODE_CASH, "EFECTIVO" -> Cash
                CODE_TRANSFER, "TRANSFERENCIA" -> BankTransfer
                else -> DebitCard
            }
        }
    }
}