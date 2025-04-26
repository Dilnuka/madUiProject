// TransactionsViewModel.kt
package com.example.personalfinancetracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalfinancetracker.data.TransactionRepository
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import java.util.Date
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _expenseCategories = MutableLiveData<List<String>>()
    val expenseCategories: LiveData<List<String>> = _expenseCategories

    private val _incomeCategories = MutableLiveData<List<String>>()
    val incomeCategories: LiveData<List<String>> = _incomeCategories

    private var currentFilter: TransactionType? = null

    init {
        loadTransactions()
        loadCategories()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            val allTransactions = repository.getAllTransactions()
            _transactions.value = if (currentFilter != null) {
                allTransactions.filter { it.type == currentFilter }
            } else {
                allTransactions
            }
        }
    }

    private fun loadCategories() {
        _expenseCategories.value = repository.getCategories(TransactionType.EXPENSE)
        _incomeCategories.value = repository.getCategories(TransactionType.INCOME)
    }

    fun setFilter(type: TransactionType?) {
        currentFilter = type
        loadTransactions()
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            loadTransactions()
        }
    }

    fun refreshData() {
        loadTransactions()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            loadTransactions()
        }
    }

    fun updateTransaction(
        id: String,
        amount: Double,
        description: String,
        category: String,
        type: TransactionType
    ) {
        viewModelScope.launch {
            repository.updateTransaction(id, amount, description, category, type)
            loadTransactions()
        }
    }

    fun getTransactionById(id: String): Transaction? {
        return repository.getTransactionById(id)
    }

    fun getCategories(type: TransactionType): List<String> {
        return repository.getCategories(type)
    }

    fun getCurrency(): String {
        return repository.getCurrency()
    }
}