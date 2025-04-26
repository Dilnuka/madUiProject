package com.example.personalfinancetracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.data.model.Transaction
import com.example.personalfinancetracker.data.model.TransactionType
import com.example.personalfinancetracker.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val viewModel: TransactionsViewModel
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(transaction: Transaction) {
            binding.apply {
                val currency = viewModel.getCurrency()
                amountText.text = "$currency${String.format("%.2f", transaction.amount)}"
                descriptionText.text = transaction.description
                categoryText.text = transaction.category
                dateText.text = dateFormat.format(transaction.date)

                when (transaction.type) {
                    TransactionType.INCOME -> {
                        amountText.setTextColor(ContextCompat.getColor(root.context, R.color.primary))
                    }
                    TransactionType.EXPENSE -> {
                        amountText.setTextColor(ContextCompat.getColor(root.context, R.color.primary))
                    }
                }

                root.setOnClickListener {
                    onEditClick(transaction)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(transaction)
                }
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
} 