package com.example.personalfinancetracker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.personalfinancetracker.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class AuthRepository(private val context: Context) {
    private val gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_USERS = "users"
        const val KEY_CURRENT_USER = "current_user"
    }

    fun register(username: String, email: String, password: String): Boolean {
        val users = getAllUsers()
        
        // Check if email already exists
        if (users.any { it.email == email }) {
            return false
        }

        val newUser = User(
            id = Date().time.toString(),
            username = username,
            email = email,
            password = password
        )

        val updatedUsers = users.toMutableList()
        updatedUsers.add(newUser)
        saveUsers(updatedUsers)
        return true
    }

    fun login(email: String, password: String): User? {
        val users = getAllUsers()
        return users.find { it.email == email && it.password == password }
    }

    fun setCurrentUser(user: User?) {
        val json = if (user != null) gson.toJson(user) else null
        sharedPreferences.edit().putString(KEY_CURRENT_USER, json).apply()
    }

    fun getCurrentUser(): User? {
        val json = sharedPreferences.getString(KEY_CURRENT_USER, null)
        return if (json != null) {
            gson.fromJson(json, User::class.java)
        } else {
            null
        }
    }

    fun logout() {
        setCurrentUser(null)
    }

    private fun getAllUsers(): List<User> {
        val json = sharedPreferences.getString(KEY_USERS, "[]")
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        sharedPreferences.edit().putString(KEY_USERS, json).apply()
    }
} 