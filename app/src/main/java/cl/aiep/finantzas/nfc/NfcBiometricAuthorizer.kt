package cl.aiep.finantzas.nfc

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter

/**
 * Resultado tipado del flujo de autenticación biométrica para pagos NFC (OWASP M3).
 */
sealed interface BiometricAuthOutcome {
    /** El hardware biométrico confirmó la identidad del titular del dispositivo. */
    data object Succeeded : BiometricAuthOutcome

    /** La lectura biométrica no coincidió; el diálogo sigue abierto para reintentar. */
    data object AttemptFailed : BiometricAuthOutcome

    /** Error terminal o cancelación que cerró el diálogo biométrico inmediatamente. */
    data class Error(
        val errorCode: Int,
        val message: String
    ) : BiometricAuthOutcome
}

/**
 * Autorizador biométrico Clase 3 (`BIOMETRIC_STRONG`) para pagos NFC en Pesos Chilenos (`CLP`).
 *
 * Mitiga **OWASP Mobile Top 10 (M3: Insecure Authentication/Authorization)** exigiendo
 * validación biométrica de hardware antes de armar cualquier comprobante en [HostApduServiceNfc].
 */
class NfcBiometricAuthorizer(
    private val activity: FragmentActivity
) {

    /** Consulta al sistema si existe un sensor biométrico Clase 3 activo y con huellas/rostro enrolados. */
    fun canAuthorizeStrongBiometrics(): Int {
        return BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG)
    }

    /**
     * Lanza el diálogo nativo [BiometricPrompt] mostrando el monto entero en CLP (`Long`)
     * sin decimales antes de permitir el armado del pago sin contacto.
     */
    fun authenticateContactlessPaymentClp(
        amountClp: Long,
        merchantName: String,
        onOutcome: (BiometricAuthOutcome) -> Unit
    ) {
        require(amountClp > MIN_EXCLUSIVE_AMOUNT_CLP) {
            "El monto en CLP debe ser un entero positivo mayor a cero."
        }

        val availability = canAuthorizeStrongBiometrics()
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            HostApduServiceNfc.clearAuthorizedPayload()
            onOutcome(
                BiometricAuthOutcome.Error(
                    errorCode = availability,
                    message = mapAvailabilityErrorMessage(availability)
                )
            )
            return
        }

        val mainExecutor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            mainExecutor,
            createAuthenticationCallback(onOutcome)
        )
        val promptInfo = buildPromptInfoClp(amountClp, merchantName)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun createAuthenticationCallback(
        onOutcome: (BiometricAuthOutcome) -> Unit
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onOutcome(BiometricAuthOutcome.Succeeded)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                HostApduServiceNfc.clearAuthorizedPayload()
                onOutcome(
                    BiometricAuthOutcome.Error(
                        errorCode = errorCode,
                        message = errString.toString()
                    )
                )
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                HostApduServiceNfc.clearAuthorizedPayload()
                onOutcome(BiometricAuthOutcome.AttemptFailed)
            }
        }
    }

    private fun buildPromptInfoClp(
        amountClp: Long,
        merchantName: String
    ): BiometricPrompt.PromptInfo {
        val formattedClp = ClpCurrencyFormatter.formatClp(amountClp)
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(PROMPT_TITLE)
            .setSubtitle("Pago de $formattedClp CLP en $merchantName")
            .setDescription(PROMPT_DESCRIPTION)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .setNegativeButtonText(NEGATIVE_BUTTON_LABEL)
            .build()
    }

    private fun mapAvailabilityErrorMessage(status: Int): String {
        return when (status) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Este dispositivo no cuenta con sensor biométrico Clase 3."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "El sensor biométrico no está disponible temporalmente."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Registra al menos una huella o rostro en Ajustes de seguridad de Android."
            else ->
                "No es posible verificar identidad biométrica en este momento."
        }
    }

    companion object {
        private const val MIN_EXCLUSIVE_AMOUNT_CLP = 0L
        private const val PROMPT_TITLE = "Autorizar pago NFC finANTzas"
        private const val PROMPT_DESCRIPTION =
            "Verifica tu huella o rostro para habilitar un único cobro sin contacto."
        private const val NEGATIVE_BUTTON_LABEL = "Cancelar pago"
    }
}

/**
 * Función de extensión para recuperar la [FragmentActivity] desde `LocalContext.current`
 * en pantallas Jetpack Compose, desenvolviendo cualquier [ContextWrapper] intermedio.
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    return generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<FragmentActivity>()
        .firstOrNull()
}
