// SettingsViewModel.kt
package com.example.personalfinancetracker.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalfinancetracker.data.TransactionRepository
import com.example.personalfinancetracker.data.model.Transaction
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private val gson = Gson()
    private val sharedPreferences = repository.getSharedPreferences()

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
    }

    fun setCurrency(currency: String) {
        repository.setCurrency(currency)
        _currency.value = currency
    }

    fun getCurrency(): String {
        return repository.getCurrency()
    }

    fun exportData(): Boolean {
        return try {
            val context = getApplication<Application>().applicationContext
            val transactions = repository.getTransactions()
            val json = gson.toJson(transactions)
            
            val file = File(context.getExternalFilesDir(null), "transactions_backup.json")
            FileOutputStream(file).use { output ->
                output.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importData(): Boolean {
        return try {
            val context = getApplication<Application>().applicationContext
            val file = File(context.getExternalFilesDir(null), "transactions_backup.json")
            
            if (!file.exists()) {
                return false
            }

            val json = FileReader(file).use { reader ->
                reader.readText()
            }

            val transactions = gson.fromJson(json, Array<Transaction>::class.java).toList()
            transactions.forEach { transaction ->
                repository.addTransaction(transaction)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}