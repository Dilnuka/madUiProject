package com.example.personalfinancetracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.AuthRepository
import com.example.personalfinancetracker.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        authRepository = AuthRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (validateInputs(email, password)) {
                val user = authRepository.login(email, password)
                if (user != null) {
                    authRepository.setCurrentUser(user)
                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                } else {
                    showError(getString(R.string.invalid_credentials))
                }
            }
        }

        binding.registerText.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

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