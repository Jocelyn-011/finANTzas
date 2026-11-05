-- ============================================================================
-- SCRIPT DE BASE DE DATOS POSTGRESQL + SEGURIDAD ROW-LEVEL SECURITY (RLS)
-- Moneda Oficial: Pesos Chilenos (CLP) sin decimales (Tipo entero BIGINT)
-- Archivo para adjuntar en ClickUp: Tareas T1.2, T2.2, T3.3 y T4.3 (Enrique Ahumada)
-- Proyecto: finANTzas (Jocelyn Peñailillo, Enrique Ahumada y Álvaro Brizuela)
-- Cumplimiento: Ley N° 21.719 (Protección de Datos), OWASP M3/M4 y Tríada CID
-- ============================================================================

-- 1. Tabla Principal de Gastos, Pagos Fijos y Suscripciones (Montos en CLP enteros)
CREATE TABLE IF NOT EXISTS public.expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    concept VARCHAR(160) NOT NULL CHECK (char_length(trim(concept)) >= 2),
    unit_amount_clp BIGINT NOT NULL CHECK (unit_amount_clp > 0),
    weekly_frequency INTEGER NOT NULL DEFAULT 1 CHECK (weekly_frequency >= 0), -- 0 = Mensual fijo (1 vez/mes), 1..7 = Veces por semana
    expense_type VARCHAR(32) NOT NULL CHECK (expense_type IN ('PURCHASE', 'FIXED_PAYMENT', 'SUBSCRIPTION')),
    category VARCHAR(64) NOT NULL CHECK (category IN (
        'MICRO_PURCHASES_AND_SNACKS',
        'ENTERTAINMENT_SUBSCRIPTIONS',
        'FOOD_AND_GROCERIES',
        'TRANSPORT_AND_MOBILITY',
        'HOUSING_AND_UTILITIES'
    )),
    payment_method VARCHAR(32) NOT NULL CHECK (payment_method IN (
        'CREDIT_CARD',
        'DEBIT_CARD',
        'CASH',
        'BANK_TRANSFER',
        'NFC_MOBILE_WALLET'
    )),
    is_ant_expense BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_rule_code VARCHAR(16) NOT NULL DEFAULT 'ESSENTIAL',
    rule_reason TEXT NOT NULL,
    idempotency_key VARCHAR(128) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Tabla de Topes de Presupuesto Mensual y por Categoría (Montos en CLP enteros)
CREATE TABLE IF NOT EXISTS public.category_budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category VARCHAR(64) NOT NULL,
    monthly_limit_clp BIGINT NOT NULL CHECK (monthly_limit_clp > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, category)
);

-- 3. Índices de Rendimiento para Analítica (< 100 ms en Sankey y Anillos de Jetpack Compose)
CREATE INDEX IF NOT EXISTS idx_expenses_user_category ON public.expenses (user_id, category);
CREATE INDEX IF NOT EXISTS idx_expenses_user_payment ON public.expenses (user_id, payment_method);
CREATE INDEX IF NOT EXISTS idx_expenses_user_ant ON public.expenses (user_id, is_ant_expense);

-- 4. Vista Analítica para Porcentaje de Fuga por Medio de Pago en CLP sin decimales (Regla RN-04)
CREATE OR REPLACE VIEW public.v_payment_method_leakage AS
SELECT
    user_id,
    payment_method,
    SUM(unit_amount_clp * CASE WHEN weekly_frequency = 0 THEN 1 ELSE weekly_frequency * 4 END)::BIGINT AS total_monthly_clp,
    SUM(CASE WHEN is_ant_expense THEN unit_amount_clp * CASE WHEN weekly_frequency = 0 THEN 1 ELSE weekly_frequency * 4 END ELSE 0 END)::BIGINT AS ant_leakage_monthly_clp,
    ROUND(
        (SUM(CASE WHEN is_ant_expense THEN unit_amount_clp * CASE WHEN weekly_frequency = 0 THEN 1 ELSE weekly_frequency * 4 END ELSE 0 END)::NUMERIC
        / NULLIF(SUM(unit_amount_clp * CASE WHEN weekly_frequency = 0 THEN 1 ELSE weekly_frequency * 4 END), 0)) * 100,
    0)::INTEGER AS leakage_percentage
FROM public.expenses
GROUP BY user_id, payment_method;

-- 5. Activación de Row-Level Security (Pilar de Confidencialidad CID)
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.category_budgets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Usuario autoadministrador gestiona exclusivamente sus propios gastos en CLP"
    ON public.expenses
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Usuario autoadministrador gestiona exclusivamente sus propios presupuestos en CLP"
    ON public.category_budgets
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);