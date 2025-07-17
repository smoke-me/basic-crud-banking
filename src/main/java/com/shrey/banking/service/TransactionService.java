package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;
import java.util.List;

public interface TransactionService {
    Transaction saveTransaction(Transaction transaction);
    void saveAllTransactions(List<Transaction> transactions);
    List<Transaction> getAllTransactions();
    Transaction getTransactionById(Long id);
    Transaction updateTransaction(Long id, Transaction updated);
    void deleteTransaction(Long id);
    void deleteAllTransactions();
    List<Transaction> upsertTransactions(List<Transaction> transactions);
}
