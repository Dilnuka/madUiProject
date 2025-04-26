package com.example.personalfinancetracker.data.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val password: String,
    val name: String = "",
    val age: Int = 0,
    val gender: String = ""
) 