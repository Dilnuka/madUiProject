// Transaction.kt
package com.example.personalfinancetracker.data.model

import java.util.Date
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val date: Date = Date()
)