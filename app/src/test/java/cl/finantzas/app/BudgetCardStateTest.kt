package cl.finantzas.app

import cl.finantzas.app.ui.viewmodel.BudgetCardState
import org.junit.Assert.*
import org.junit.Test

class BudgetCardStateTest {
    @Test fun percentageKeepsOverspendingWhileProgressIsClamped() {
        val state = BudgetCardState("GLOBAL", "Global", 100L, 125L)
        assertEquals(125.0, state.percentage, 0.001)
        assertEquals(1f, state.fraction, 0f)
    }
    @Test fun supportsLongClpWithoutIntegerOverflow() {
        val state = BudgetCardState("GLOBAL", "Global", 8_000_000_000L, 4_000_000_000L)
        assertEquals(50.0, state.percentage, 0.001)
        assertEquals(0.5f, state.fraction, 0f)
    }
}
