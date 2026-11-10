package cl.finantzas.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import cl.finantzas.app.ui.auth.AuthGate
import cl.finantzas.app.ui.theme.FinANTzasTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            FinANTzasTheme {
                AuthGate()
            }
        }
    }
}
