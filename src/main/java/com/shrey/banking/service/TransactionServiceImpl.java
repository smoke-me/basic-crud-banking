package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;
import com.shrey.banking.exception.TransactionNotFoundException;
import com.shrey.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDate.now());
        }

        return transactionRepository.save(transaction);
    }

    @Override
    public void saveAllTransactions(List<Transaction> transactions) {
        transactionRepository.saveAll(transactions);
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Override
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        getTransactionById(id); // Check if it exists
        updatedTransaction.setId(id); // Necessary otherwise it will create a new object

        return saveTransaction(updatedTransaction);
    }

    @Override
    public void deleteTransaction(Long id) {
        getTransactionById(id); // Check if it exists

        transactionRepository.deleteById(id);
    }

    @Override
    public void deleteAllTransactions() {
        transactionRepository.deleteAll();
    }

    @Override
    public List<Transaction> upsertTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (transaction.getId() != null) {
                try {
                    updateTransaction(transaction.getId(), transaction);
                } catch (TransactionNotFoundException e) {
                    saveTransaction(transaction);
                }
                continue;
            }

            saveTransaction(transaction);
        }

        return transactions;
    }
}
