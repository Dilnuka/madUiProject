package com.example.personalfinancetracker.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.databinding.FragmentEditTransactionBinding
import com.google.android.material.snackbar.Snackbar

class EditTransactionFragment : Fragment() {

    private var _binding: FragmentEditTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionsViewModel
    private val args: EditTransactionFragmentArgs by navArgs()
    private var selectedType: TransactionType = TransactionType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[TransactionsViewModel::class.java]

        val transaction = viewModel.getTransactionById(args.transactionId)
        if (transaction != null) {
            selectedType = transaction.type
            binding.amountInput.setText(transaction.amount.toString())
            binding.descriptionInput.setText(transaction.description)
            binding.categoryInput.setText(transaction.category)

            if (transaction.type == TransactionType.INCOME) {
                binding.typeSelector.check(R.id.btn_income)
            } else {
                binding.typeSelector.check(R.id.btn_expense)
            }

            setupTypeSelector()
            setupCategoryDropdown()
            setupClickListeners()
        } else {
            Snackbar.make(binding.root, R.string.transaction_not_found, Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupTypeSelector() {
        binding.typeSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = when (checkedId) {
                    R.id.btn_income -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                setupCategoryDropdown()
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categories = viewModel.getCategories(selectedType)
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, categories)
        binding.categoryInput.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val amount = binding.amountInput.text.toString().toDoubleOrNull()
            val description = binding.descriptionInput.text.toString()
            val category = binding.categoryInput.text.toString()

            if (amount != null && description.isNotBlank() && category.isNotBlank()) {
                viewModel.updateTransaction(args.transactionId, amount, description, category, selectedType)
                findNavController().navigateUp()
            } else {
                Snackbar.make(binding.root, R.string.invalid_input, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.deleteButton.setOnClickListener {
            viewModel.deleteTransaction(args.transactionId)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}