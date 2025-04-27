// SettingsViewModel.kt
package com.example.personalfinancetracker.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalfinancetracker.data.TransactionRepository
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.receivers.BudgetBroadcastReceiver
import com.example.personalfinancetracker.utils.StorageManager
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private val gson = Gson()
    private val sharedPreferences = repository.getSharedPreferences()
    private val storageManager = StorageManager(application)

    private val _monthlyBudget = MutableLiveData<Double>()
    val monthlyBudget: LiveData<Double> = _monthlyBudget

    private val _currency = MutableLiveData<String>()
    val currency: LiveData<String> = _currency

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    init {
        loadSettings()
        loadTransactions()
    }

    private fun loadSettings() {
        _monthlyBudget.value = repository.getMonthlyBudget()
        _currency.value = repository.getCurrency()
    }

    private fun loadTransactions() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        _transactions.value = repository.getTransactionsByMonth(currentMonth, currentYear)
    }

    fun setMonthlyBudget(budget: Double) {
        repository.setMonthlyBudget(budget)
        _monthlyBudget.value = budget
        checkBudgetWarning(budget)
    }

    private fun checkBudgetWarning(newBudget: Double) {
        val totalExpenses = _transactions.value?.filter { it.type == TransactionType.EXPENSE }?.sumOf { it.amount } ?: 0.0
        
        if (totalExpenses > newBudget) {
            sendBudgetExceededBroadcast(totalExpenses, newBudget)
        }
    }

    private fun sendBudgetExceededBroadcast(totalExpenses: Double, budget: Double) {
        val intent = Intent(getApplication(), BudgetBroadcastReceiver::class.java).apply {
            putExtra("total_expenses", totalExpenses)
            putExtra("monthly_budget", budget)
        }
        getApplication<Application>().sendBroadcast(intent)
    }

    fun setCurrency(currency: String) {
        repository.setCurrency(currency)
        _currency.value = currency
    }

    fun getCurrency(): String {
        return repository.getCurrency()
    }

    fun hasStoragePermission(): Boolean {
        return storageManager.hasStoragePermission()
    }

    fun requestStoragePermission() {
        storageManager.requestStoragePermission()
    }

    fun exportData(): Boolean {
        if (!hasStoragePermission()) {
            return false
        }

        return try {
            val transactions = repository.getTransactions()
            val json = gson.toJson(transactions)
            
            if (storageManager.writeToFile("transactions_backup.json", json)) {
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun importData(): Boolean {
        if (!hasStoragePermission()) {
            return false
        }

        return try {
            if (!storageManager.fileExists("transactions_backup.json")) {
                return false
            }

            if (storageManager.getFileSize("transactions_backup.json") == 0L) {
                return false
            }

            val json = storageManager.readFromFile("transactions_backup.json")
            if (json == null || json.isBlank()) {
                return false
            }

            val transactions = try {
                gson.fromJson(json, Array<Transaction>::class.java).toList()
            } catch (e: Exception) {
                return false
            }

            if (transactions.isEmpty()) {
                return false
            }

            transactions.forEach { transaction ->
                repository.addTransaction(transaction)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}