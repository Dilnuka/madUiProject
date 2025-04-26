package com.example.personalfinancetracker.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.databinding.FragmentCategoriesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CategoriesViewModel
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupTypeSelector()
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onEditClick = { category ->
                showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                showDeleteConfirmationDialog(category)
            }
        )

        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CategoriesFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
            binding.emptyState.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun setupTypeSelector() {
        binding.typeFilterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.expenseButton -> viewModel.setType(TransactionType.EXPENSE)
                    R.id.incomeButton -> viewModel.setType(TransactionType.INCOME)
                }
            }
        }
    }

    private fun showAddCategoryDialog() {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.category_name)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_category)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val categoryName = input.text?.toString()?.trim()
                if (!categoryName.isNullOrEmpty()) {
                    viewModel.addCategory(categoryName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditCategoryDialog(category: String) {
        val input = TextInputEditText(requireContext()).apply {
            setText(category)
            hint = getString(R.string.category_name)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_category)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text?.toString()?.trim()
                if (!newName.isNullOrEmpty()) {
                    viewModel.updateCategory(category, newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmationDialog(category: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_category)
            .setMessage(R.string.delete_category_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 