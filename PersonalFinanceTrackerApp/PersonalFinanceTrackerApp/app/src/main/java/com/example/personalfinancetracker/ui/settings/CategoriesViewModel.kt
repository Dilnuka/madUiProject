// CategoriesViewModel.kt
package com.example.personalfinancetracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.personalfinancetracker.data.TransactionRepository
import com.example.personalfinancetracker.data.model.TransactionType

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(application)
    private var currentType: TransactionType = TransactionType.EXPENSE

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    init {
        loadCategories()
    }

    fun setType(type: TransactionType) {
        currentType = type
        loadCategories()
    }

    private fun loadCategories() {
        _categories.value = repository.getCategories(currentType)
    }

    fun addCategory(category: String) {
        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()
        if (category !in currentCategories) {
            currentCategories.add(category)
            repository.saveCategories(currentCategories, currentType)
            _categories.value = currentCategories
        }
    }

    fun updateCategory(oldCategory: String, newCategory: String) {
        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()
        val index = currentCategories.indexOf(oldCategory)
        if (index != -1) {
            currentCategories[index] = newCategory
            repository.saveCategories(currentCategories, currentType)
            _categories.value = currentCategories
        }
    }

    fun deleteCategory(category: String) {
        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()
        currentCategories.remove(category)
        repository.saveCategories(currentCategories, currentType)
        _categories.value = currentCategories
    }
}