package com.example.personalfinancetracker.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe username and update greeting
        viewModel.username.observe(viewLifecycleOwner) { username ->
            binding.greetingText.text = "Welcome, $username"
        }

        setupObservers()
        setupIncomeExpensePieChart()
        setupCategoriesPieChart()
    }

    private fun setupObservers() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateDashboard(transactions)
        }

        viewModel.monthlyBudget.observe(viewLifecycleOwner) { budget ->
            binding.totalBudget.text = formatCurrency(budget)
        }

        viewModel.budgetWarning.observe(viewLifecycleOwner) { warningState ->
            when (warningState) {
                BudgetWarningState.APPROACHING -> {
                    binding.budgetWarningLayout.visibility = View.VISIBLE
                    binding.budgetWarningText.text = getString(R.string.budget_warning)
                    binding.budgetWarningText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.warningIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
                }
                BudgetWarningState.EXCEEDED -> {
                    binding.budgetWarningLayout.visibility = View.VISIBLE
                    binding.budgetWarningText.text = getString(R.string.budget_exceeded)
                    binding.budgetWarningText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.warningIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
                }
                BudgetWarningState.NORMAL -> {
                    binding.budgetWarningLayout.visibility = View.GONE
                }
            }
        }

        viewModel.expenseByCategory.observe(viewLifecycleOwner) { expenseMap ->
            updateCategoriesPieChart(expenseMap)
        }
    }

    private fun setupIncomeExpensePieChart() {
        binding.incomeExpensePieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setHoleColor(android.R.color.transparent)
            setTransparentCircleAlpha(0)
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.white))
            setEntryLabelTextSize(12f)
            setDrawEntryLabels(true)
        }
    }

    private fun setupCategoriesPieChart() {
        binding.categoriesPieChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.white)
            legend.textSize = 12f
            setHoleColor(android.R.color.transparent)
            setTransparentCircleAlpha(0)
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.white))
            setEntryLabelTextSize(12f)
            setDrawEntryLabels(false)
        }
    }

    private fun updateDashboard(transactions: List<Transaction>) {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expenses

        binding.totalIncome.text = formatCurrency(income)
        binding.totalExpense.text = formatCurrency(expenses)
        binding.balance.text = formatCurrency(balance)

        updateIncomeExpensePieChart(income, expenses)
    }

    private fun updateIncomeExpensePieChart(income: Double, expenses: Double) {
        val entries = listOf(
            PieEntry(income.toFloat(), getString(R.string.income)),
            PieEntry(expenses.toFloat(), getString(R.string.expense))
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.chart_color_2),
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
            valueTextSize = 12f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.white)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
        }

        binding.incomeExpensePieChart.data = PieData(dataSet)
        binding.incomeExpensePieChart.invalidate()
    }

    private fun updateCategoriesPieChart(expenseMap: Map<String, Double>) {
        val entries = expenseMap.entries.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.chart_color_1),
            ContextCompat.getColor(requireContext(), R.color.chart_color_2),
            ContextCompat.getColor(requireContext(), R.color.chart_color_3),
            ContextCompat.getColor(requireContext(), R.color.chart_color_4),
            ContextCompat.getColor(requireContext(), R.color.chart_color_5)
        ).let { colorList ->
            List(entries.size) { i -> colorList[i % colorList.size] }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.white)
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
        }

        binding.categoriesPieChart.data = PieData(dataSet)
        binding.categoriesPieChart.invalidate()
    }

    private fun formatCurrency(amount: Double): String {
        val currency = viewModel.getCurrency()
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 2
        format.minimumFractionDigits = 2

        val symbols = DecimalFormatSymbols()
        symbols.currencySymbol = currency
        (format as DecimalFormat).decimalFormatSymbols = symbols

        return format.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}