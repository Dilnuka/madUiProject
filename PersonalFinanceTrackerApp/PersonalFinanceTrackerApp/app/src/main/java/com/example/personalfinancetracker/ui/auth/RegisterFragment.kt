package com.example.personalfinancetracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.AuthRepository
import com.example.personalfinancetracker.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        authRepository = AuthRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (validateInputs(username, email, password)) {
                if (authRepository.register(username, email, password)) {
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                } else {
                    showError(getString(R.string.registration_failed))
                }
            }
        }

        binding.loginText.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String
    ): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.usernameLayout.error = getString(R.string.name_required)
            isValid = false
        } else {
            binding.usernameLayout.error = null
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.invalid_email)
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        if (password.length != 4) {
            binding.passwordLayout.error = getString(R.string.invalid_password)
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }

        return isValid
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 