// DashboardViewModel.kt
package com.example.personalfinancetracker.ui.dashboard

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalfinancetracker.data.AuthRepository
import com.example.personalfinancetracker.data.TransactionRepository
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import java.util.Calendar

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private val authRepository = AuthRepository(application)
    private val calendar = Calendar.getInstance()
    private val sharedPreferences = repository.getSharedPreferences()

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _monthlyBudget = MutableLiveData<Double>()
    val monthlyBudget: LiveData<Double> = _monthlyBudget

    private val _expenseByCategory = MutableLiveData<Map<String, Double>>()
    val expenseByCategory: LiveData<Map<String, Double>> = _expenseByCategory

    private val _budgetWarning = MutableLiveData<BudgetWarningState>()
    val budgetWarning: LiveData<BudgetWarningState> = _budgetWarning

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            TransactionRepository.KEY_TRANSACTIONS -> {
                loadTransactions()
                loadExpenseByCategory()
            }
            TransactionRepository.KEY_MONTHLY_BUDGET -> {
                loadMonthlyBudget()
            }
        }
    }

    init {
        loadData()
        loadUsername()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun loadUsername() {
        _username.value = authRepository.getCurrentUser()?.username ?: ""
    }

    private fun loadData() {
        loadTransactions()
        loadMonthlyBudget()
        loadExpenseByCategory()
        checkBudgetWarning()
    }

    private fun checkBudgetWarning() {
        val budget = _monthlyBudget.value ?: 0.0
        val totalExpenses = _transactions.value?.filter { it.type == TransactionType.EXPENSE }?.sumOf { it.amount } ?: 0.0
        
        _budgetWarning.value = when {
            totalExpenses >= budget -> BudgetWarningState.EXCEEDED
            totalExpenses >= budget * 0.8 -> BudgetWarningState.APPROACHING
            else -> BudgetWarningState.NORMAL
        }
    }

    private fun loadTransactions() {
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        _transactions.value = repository.getTransactionsByMonth(currentMonth, currentYear)
        checkBudgetWarning()
    }

    private fun loadMonthlyBudget() {
        _monthlyBudget.value = repository.getMonthlyBudget()
        checkBudgetWarning()
    }

    private fun loadExpenseByCategory() {
        _expenseByCategory.value = repository.getExpenseByCategory()
    }

    fun refreshData() {
        loadData()
    }

    fun getCurrency(): String {
        return repository.getCurrency()
    }

    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

enum class BudgetWarningState {
    NORMAL,
    APPROACHING,
    EXCEEDED
}