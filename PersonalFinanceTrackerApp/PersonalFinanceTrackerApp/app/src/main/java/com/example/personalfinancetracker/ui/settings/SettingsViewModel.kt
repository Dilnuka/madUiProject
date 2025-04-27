// SettingsViewModel.kt
package com.example.personalfinancetracker.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalfinancetracker.data.TransactionRepository
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.receivers.BudgetBroadcastReceiver
import com.example.personalfinancetracker.utils.StorageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    private val _importExportStatus = MutableLiveData<String>()
    val importExportStatus: LiveData<String> = _importExportStatus

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
            _importExportStatus.value = "Storage permission required"
            return false
        }

        return try {
            val transactions = repository.getTransactions()
            if (transactions.isEmpty()) {
                _importExportStatus.value = "No transactions to export"
                return false
            }

            val json = gson.toJson(transactions)
            if (storageManager.writeToFile("transactions_backup.json", json)) {
                _importExportStatus.value = "Data exported successfully to ${storageManager.getBackupFilePath()}"
                true
            } else {
                _importExportStatus.value = "Failed to write backup file"
                false
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Export failed", e)
            _importExportStatus.value = "Export failed: ${e.message}"
            false
        }
    }

    fun importData(): Boolean {
        if (!hasStoragePermission()) {
            _importExportStatus.value = "Storage permission required"
            return false
        }

        return try {
            if (!storageManager.fileExists("transactions_backup.json")) {
                _importExportStatus.value = "Backup file not found"
                return false
            }

            if (storageManager.getFileSize("transactions_backup.json") == 0L) {
                _importExportStatus.value = "Backup file is empty"
                return false
            }

            val json = storageManager.readFromFile("transactions_backup.json")
            if (json == null || json.isBlank()) {
                _importExportStatus.value = "Failed to read backup file"
                return false
            }

            importDataFromJson(json)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Import failed", e)
            _importExportStatus.value = "Import failed: ${e.message}"
            false
        }
    }

    fun importDataFromJson(json: String): Boolean {
        return try {
            Log.d("SettingsViewModel", "Starting JSON import")
            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactions = try {
                gson.fromJson<List<Transaction>>(json, type)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to parse JSON", e)
                _importExportStatus.value = "Invalid backup file format"
                return false
            }

            Log.d("SettingsViewModel", "Successfully parsed ${transactions.size} transactions from JSON")

            if (transactions.isEmpty()) {
                Log.e("SettingsViewModel", "No transactions found in backup")
                _importExportStatus.value = "No transactions found in backup"
                return false
            }

            // Clear existing transactions
            Log.d("SettingsViewModel", "Clearing existing transactions")
            repository.getAllTransactions().forEach { transaction ->
                repository.deleteTransaction(transaction.id)
            }

            // Import new transactions
            Log.d("SettingsViewModel", "Importing new transactions")
            transactions.forEach { transaction ->
                repository.addTransaction(transaction)
            }

            _importExportStatus.value = "Successfully imported ${transactions.size} transactions"
            Log.d("SettingsViewModel", "JSON import completed successfully")
            true
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Import failed", e)
            _importExportStatus.value = "Import failed: ${e.message}"
            false
        }
    }

    fun importDataFromCsv(csv: String): Boolean {
        return try {
            Log.d("SettingsViewModel", "Starting CSV import")
            val lines = csv.split("\n")
            Log.d("SettingsViewModel", "Found ${lines.size} lines in CSV")
            
            if (lines.size < 2) {
                Log.e("SettingsViewModel", "CSV file is empty or invalid")
                _importExportStatus.value = "CSV file is empty or invalid"
                return false
            }

            // Skip header row
            val transactions = lines.subList(1, lines.size)
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        Log.d("SettingsViewModel", "Processing line: $line")
                        val parts = line.split(",")
                        if (parts.size < 5) {
                            Log.e("SettingsViewModel", "Invalid CSV line (not enough parts): $line")
                            return@mapNotNull null
                        }

                        // Try different date formats
                        val date = try {
                            // Try parsing as milliseconds
                            Date(parts[0].trim().toLong())
                        } catch (e: Exception) {
                            try {
                                // Try parsing as ISO date string
                                val dateStr = parts[0].trim()
                                val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                formatter.parse(dateStr)
                            } catch (e: Exception) {
                                try {
                                    // Try parsing the specific format: "Sun Apr 27 08:23:16 GMT+05:30 2025"
                                    val dateStr = parts[0].trim()
                                    val formatter = java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                                    formatter.parse(dateStr)
                                } catch (e: Exception) {
                                    Log.e("SettingsViewModel", "Invalid date in CSV: ${parts[0]}")
                                    return@mapNotNull null
                                }
                            }
                        }

                        val type = try {
                            TransactionType.valueOf(parts[1].trim().uppercase())
                        } catch (e: Exception) {
                            Log.e("SettingsViewModel", "Invalid transaction type in CSV: ${parts[1]}")
                            return@mapNotNull null
                        }

                        val category = parts[2].trim()
                        
                        // Handle different number formats
                        val amount = try {
                            parts[3].trim()
                                .replace("$", "")
                                .replace("€", "")
                                .replace("Rs", "")
                                .replace(",", "")
                                .toDouble()
                        } catch (e: Exception) {
                            Log.e("SettingsViewModel", "Invalid amount in CSV: ${parts[3]}")
                            return@mapNotNull null
                        }

                        val description = parts[4].trim()

                        Transaction(
                            id = Date().time.toString(),
                            date = date,
                            type = type,
                            category = category,
                            amount = amount,
                            description = description
                        )
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error parsing CSV line: $line", e)
                        null
                    }
                }

            Log.d("SettingsViewModel", "Successfully parsed ${transactions.size} transactions from CSV")

            if (transactions.isEmpty()) {
                Log.e("SettingsViewModel", "No valid transactions found in CSV")
                _importExportStatus.value = "No valid transactions found in CSV. Please check the file format."
                return false
            }

            // Clear existing transactions
            Log.d("SettingsViewModel", "Clearing existing transactions")
            repository.getAllTransactions().forEach { transaction ->
                repository.deleteTransaction(transaction.id)
            }

            // Import new transactions
            Log.d("SettingsViewModel", "Importing new transactions")
            transactions.forEach { transaction ->
                repository.addTransaction(transaction)
            }

            _importExportStatus.value = "Successfully imported ${transactions.size} transactions"
            Log.d("SettingsViewModel", "CSV import completed successfully")
            true
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Import failed", e)
            _importExportStatus.value = "Import failed: ${e.message}"
            false
        }
    }
}