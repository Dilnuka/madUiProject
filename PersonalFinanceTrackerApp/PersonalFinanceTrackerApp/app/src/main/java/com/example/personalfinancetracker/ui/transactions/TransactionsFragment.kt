package com.example.personalfinancetracker.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.databinding.DialogTransactionBinding
import com.example.personalfinancetracker.databinding.FragmentTransactionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionsViewModel
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[TransactionsViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupFab()
        setupFilterButtons()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { showDeleteDialog(it) },
            viewModel = viewModel
        )
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionsFragment.adapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactions.collectLatest { transactions ->
                adapter.submitList(transactions)
                binding.emptyState.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {
            showAddDialog()
        }
    }

    private fun setupFilterButtons() {
        binding.typeFilterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.allButton -> viewModel.setFilter(null)
                    R.id.incomeButton -> viewModel.setFilter(TransactionType.INCOME)
                    R.id.expenseButton -> viewModel.setFilter(TransactionType.EXPENSE)
                }
            }
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogTransactionBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_transaction)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val amount = dialogBinding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogBinding.descriptionInput.text.toString()
                val type = if (dialogBinding.typeSelection.checkedRadioButtonId == R.id.incomeRadio) 
                    TransactionType.INCOME else TransactionType.EXPENSE
                
                val category = if (type == TransactionType.INCOME) {
                    dialogBinding.incomeCategoryInput.text.toString()
                } else {
                    dialogBinding.categoryInput.text.toString()
                }
                
                if (description.isNotBlank() && (type == TransactionType.INCOME || category.isNotBlank())) {
                    viewModel.addTransaction(Transaction(
                        amount = amount,
                        description = description,
                        category = category,
                        type = type
                    ))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        // Set initial visibility for category layouts
        dialogBinding.categoryLayout.visibility = View.VISIBLE
        dialogBinding.incomeCategoryLayout.visibility = View.GONE

        // Set up type selection
        dialogBinding.typeSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.incomeRadio -> {
                    dialogBinding.categoryLayout.visibility = View.GONE
                    dialogBinding.incomeCategoryLayout.visibility = View.VISIBLE
                    dialogBinding.categoryInput.setText("")
                }
                R.id.expenseRadio -> {
                    dialogBinding.categoryLayout.visibility = View.VISIBLE
                    dialogBinding.incomeCategoryLayout.visibility = View.GONE
                    dialogBinding.incomeCategoryInput.setText("")
                }
            }
        }

        // Set up expense category dropdown
        val expenseCategories = viewModel.getCategories(TransactionType.EXPENSE)
        val expenseAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, expenseCategories)
        dialogBinding.categoryInput.setAdapter(expenseAdapter)

        // Set up income category dropdown
        val incomeCategories = viewModel.getCategories(TransactionType.INCOME)
        val incomeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, incomeCategories)
        dialogBinding.incomeCategoryInput.setAdapter(incomeAdapter)

        dialog.show()
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogBinding = DialogTransactionBinding.inflate(layoutInflater)
        dialogBinding.apply {
            amountInput.setText(transaction.amount.toString())
            descriptionInput.setText(transaction.description)
            
            // Set initial type and category visibility
            if (transaction.type == TransactionType.INCOME) {
                typeSelection.check(R.id.incomeRadio)
                categoryLayout.visibility = View.GONE
                incomeCategoryLayout.visibility = View.VISIBLE
                incomeCategoryInput.setText(transaction.category)
            } else {
                typeSelection.check(R.id.expenseRadio)
                categoryLayout.visibility = View.VISIBLE
                incomeCategoryLayout.visibility = View.GONE
                categoryInput.setText(transaction.category)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_transaction)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val amount = dialogBinding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val description = dialogBinding.descriptionInput.text.toString()
                val type = if (dialogBinding.typeSelection.checkedRadioButtonId == R.id.incomeRadio) 
                    TransactionType.INCOME else TransactionType.EXPENSE
                
                val category = if (type == TransactionType.INCOME) {
                    dialogBinding.incomeCategoryInput.text.toString()
                } else {
                    dialogBinding.categoryInput.text.toString()
                }
                
                if (description.isNotBlank() && (type == TransactionType.INCOME || category.isNotBlank())) {
                    viewModel.updateTransaction(
                        id = transaction.id,
                        amount = amount,
                        description = description,
                        category = category,
                        type = type
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        // Set up type selection
        dialogBinding.typeSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.incomeRadio -> {
                    dialogBinding.categoryLayout.visibility = View.GONE
                    dialogBinding.incomeCategoryLayout.visibility = View.VISIBLE
                    dialogBinding.categoryInput.setText("")
                }
                R.id.expenseRadio -> {
                    dialogBinding.categoryLayout.visibility = View.VISIBLE
                    dialogBinding.incomeCategoryLayout.visibility = View.GONE
                    dialogBinding.incomeCategoryInput.setText("")
                }
            }
        }

        // Set up category dropdown
        val categories = viewModel.getCategories(TransactionType.EXPENSE)
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, categories)
        dialogBinding.categoryInput.setAdapter(adapter)

        dialog.show()
    }

    private fun showDeleteDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_transaction)
            .setMessage(getString(R.string.delete_transaction_confirmation, transaction.description))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteTransaction(transaction.id)
                Snackbar.make(binding.root, R.string.transaction_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
 