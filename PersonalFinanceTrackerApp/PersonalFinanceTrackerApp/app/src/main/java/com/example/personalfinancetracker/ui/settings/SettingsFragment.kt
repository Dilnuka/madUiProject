package com.example.personalfinancetracker.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.databinding.DialogBudgetInputBinding
import com.example.personalfinancetracker.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { exportDataToFile(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importDataFromFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.monthlyBudget.observe(viewLifecycleOwner) { budget ->
            binding.monthlyBudgetText.text = "${viewModel.getCurrency()}${String.format("%.2f", budget)}"
        }
        
        viewModel.currency.observe(viewLifecycleOwner) { currency ->
            binding.currencyText.text = currency
        }
    }

    private fun setupClickListeners() {
        binding.editBudgetButton.setOnClickListener {
            showBudgetDialog()
        }

        binding.selectCurrencyButton.setOnClickListener {
            showCurrencyDialog()
        }

        binding.manageCategoriesButton.setOnClickListener {
            findNavController().navigate(R.id.categoriesFragment)
        }

        binding.exportDataButton.setOnClickListener {
            exportData()
        }

        binding.importDataButton.setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun showBudgetDialog() {
        val dialogBinding = DialogBudgetInputBinding.inflate(layoutInflater)
        dialogBinding.amountInput.setText(viewModel.monthlyBudget.value?.toString() ?: "")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.set_budget)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.set) { _, _ ->
                val amount = dialogBinding.amountInput.text.toString().toDoubleOrNull()
                if (amount != null) {
                    viewModel.setMonthlyBudget(amount)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf(
            getString(R.string.currency_dollar),
            getString(R.string.currency_rupee),
            getString(R.string.currency_euro)
        )
        
        val currentCurrency = viewModel.getCurrency()
        val selectedIndex = when (currentCurrency) {
            "$-US Dollar" -> 0
            "Rs-SriLankan Rupee" -> 1
            "€-Euro" -> 2
            else -> 1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_currency)
            .setSingleChoiceItems(currencies, selectedIndex) { dialog, which ->
                val selectedCurrency = when (which) {
                    0 -> "$ "
                    1 -> "Rs "
                    2 -> "€ "
                    else -> "Rs "
                }
                viewModel.setCurrency(selectedCurrency)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportData() {
        val fileName = "transactions_${System.currentTimeMillis()}.csv"
        exportLauncher.launch(fileName)
    }

    private fun exportDataToFile(uri: Uri) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = BufferedWriter(OutputStreamWriter(outputStream))
                
                // Write CSV header
                writer.write("Date,Type,Category,Amount,Description\n")
                
                // Write transaction data
                viewModel.transactions.value?.forEach { transaction ->
                    writer.write("${transaction.date},${transaction.type},${transaction.category},${transaction.amount},${transaction.description}\n")
                }
                
                writer.close()
                Toast.makeText(requireContext(), R.string.data_exported, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importDataFromFile(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                Log.d("SettingsFragment", "Read content from file: ${content.take(100)}...")
                
                val success = if (uri.toString().endsWith(".csv", ignoreCase = true)) {
                    Log.d("SettingsFragment", "Detected CSV file")
                    viewModel.importDataFromCsv(content)
                } else if (uri.toString().endsWith(".json", ignoreCase = true)) {
                    Log.d("SettingsFragment", "Detected JSON file")
                    viewModel.importDataFromJson(content)
                } else {
                    Log.d("SettingsFragment", "Trying to detect file format")
                    if (content.trim().startsWith("[")) {
                        Log.d("SettingsFragment", "Detected JSON format from content")
                        viewModel.importDataFromJson(content)
                    } else {
                        Log.d("SettingsFragment", "Detected CSV format from content")
                        viewModel.importDataFromCsv(content)
                    }
                }
                
                if (success) {
                    Snackbar.make(binding.root, R.string.data_imported, Snackbar.LENGTH_SHORT).show()
                } else {
                    val errorMessage = viewModel.importExportStatus.value ?: "Import failed"
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            } ?: run {
                Log.e("SettingsFragment", "Failed to open input stream for URI: $uri")
                Snackbar.make(binding.root, "Failed to open file", Snackbar.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Import failed", e)
            Snackbar.make(binding.root, "Import failed: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 