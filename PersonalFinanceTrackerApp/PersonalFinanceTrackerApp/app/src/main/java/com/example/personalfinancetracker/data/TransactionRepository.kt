// TransactionRepository.kt
package com.example.personalfinancetracker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.Calendar
import java.util.Date

class TransactionRepository(private val context: Context) {
    private val gson = Gson()
    private val transactionsFile = File(context.filesDir, "transactions.json")
    private val type = object : TypeToken<List<Transaction>>() {}.type
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_MONTHLY_BUDGET = "monthly_budget"
        const val KEY_CURRENCY = "currency"
        const val KEY_CATEGORIES = "categories"
        const val KEY_INCOME_CATEGORIES = "income_categories"
        const val PREFS_NAME = "transactions_prefs"
        const val KEY_TRANSACTIONS = "transactions"

        private val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "Food & Dining",
            "Transportation",
            "Housing",
            "Insurance",
            "Health & Medical",
            "Entertainment",
            "Shopping",
            "Education",
            "Travel",
            "Debt Payments",
            "Subscriptions",
            "Taxes",
            "Gifts & Donations",
            "Miscellaneous"
        )

        private val DEFAULT_INCOME_CATEGORIES = listOf(
            "Salary",
            "Business Income",
            "Investment Income",
            "Rental Income",
            "Interest Income",
            "Cash Gifts",
            "Refunds/Reimbursements",
            "Side Hustle",
            "Royalties",
            "Other Income"
        )
    }

    init {
        initializeDefaultCategories()
    }

    private fun initializeDefaultCategories() {
        // Initialize expense categories if not already set
        if (getCategories(TransactionType.EXPENSE).isEmpty()) {
            saveCategories(DEFAULT_EXPENSE_CATEGORIES, TransactionType.EXPENSE)
        }

        // Initialize income categories if not already set
        if (getCategories(TransactionType.INCOME).isEmpty()) {
            saveCategories(DEFAULT_INCOME_CATEGORIES, TransactionType.INCOME)
        }
    }

    fun getAllTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getTransactionsByType(type: TransactionType): List<Transaction> {
        return getAllTransactions().filter { it.type == type }
    }

    fun getTransactionsByMonth(month: Int, year: Int): List<Transaction> {
        return getAllTransactions().filter {
            val cal = Calendar.getInstance()
            cal.time = it.date
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }
    }

    fun addTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()
        transactions.add(transaction.copy(id = Date().time.toString()))
        saveTransactions(transactions)
    }

    fun updateTransaction(
        id: String,
        amount: Double,
        description: String,
        category: String,
        type: TransactionType
    ) {
        val transactions = getAllTransactions().toMutableList()
        val index = transactions.indexOfFirst { it.id == id }
        if (index != -1) {
            transactions[index] = transactions[index].copy(
                amount = amount,
                description = description,
                category = category,
                type = type
            )
            saveTransactions(transactions)
        }
    }

    fun deleteTransaction(id: String) {
        val transactions = getAllTransactions().toMutableList()
        transactions.removeAll { it.id == id }
        saveTransactions(transactions)
    }

    fun getTransactionById(id: String): Transaction? {
        return getAllTransactions().find { it.id == id }
    }

    fun setMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun setCurrency(currency: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "Rs") ?: "Rs"
    }

    fun getCategories(type: TransactionType): List<String> {
        val key = if (type == TransactionType.EXPENSE) KEY_CATEGORIES else KEY_INCOME_CATEGORIES
        val json = sharedPreferences.getString(key, "[]")
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, listType) ?: emptyList()
    }

    fun saveCategories(categories: List<String>, type: TransactionType) {
        val key = if (type == TransactionType.EXPENSE) KEY_CATEGORIES else KEY_INCOME_CATEGORIES
        val json = gson.toJson(categories)
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun getExpenseByCategory(): Map<String, Double> {
        return getTransactionsByType(TransactionType.EXPENSE)
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    // Alias for getAllTransactions to maintain compatibility
    fun getTransactions(): List<Transaction> = getAllTransactions()
}