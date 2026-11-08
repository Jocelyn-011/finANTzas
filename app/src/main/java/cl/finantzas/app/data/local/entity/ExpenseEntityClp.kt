package cl.finantzas.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntityClp(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val concept: String,
    @ColumnInfo(name = "unit_amount_clp") val unitAmountClp: Long,
    @ColumnInfo(name = "monthly_impact_clp") val monthlyImpactClp: Long,
    @ColumnInfo(name = "weekly_frequency") val weeklyFrequency: Int,
    @ColumnInfo(name = "expense_type") val expenseType: String,
    val category: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "is_ant_expense") val isAntExpense: Boolean,
    @ColumnInfo(name = "triggered_rule_code") val triggeredRuleCode: String,
    @ColumnInfo(name = "rule_reason") val ruleReason: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
﻿package cl.finantzas.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "expenses")
data class ExpenseEntityClp(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val concept: String,

    @ColumnInfo(name = "unit_amount_clp")
    val unitAmountClp: Long,

    @ColumnInfo(name = "monthly_impact_clp")
    val monthlyImpactClp: Long,

    @ColumnInfo(name = "weekly_frequency")
    val weeklyFrequency: Int,

    @ColumnInfo(name = "expense_type")
    val expenseType: String,

    val category: String,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String,

    @ColumnInfo(name = "is_ant_expense")
    val isAntExpense: Boolean,

    @ColumnInfo(name = "triggered_rule_code")
    val triggeredRuleCode: String,

    @ColumnInfo(name = "rule_reason")
    val ruleReason: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
