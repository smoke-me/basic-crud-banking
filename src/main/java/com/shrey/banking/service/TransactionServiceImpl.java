package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;
import com.shrey.banking.exception.TransactionNotFoundException;
import com.shrey.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ExcelExportService excelExportService;

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDate.now());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Export to Excel after saving
        excelExportService.exportTransactionsToExcel();
        
        return savedTransaction;
    }

    @Override
    public void saveAllTransactions(List<Transaction> transactions) {
        transactionRepository.saveAll(transactions);
        
        // Export to Excel after batch save
        excelExportService.exportTransactionsToExcel();
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
    @Transactional
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        Transaction existingTransaction = getTransactionById(id);
        existingTransaction.setDescription(updatedTransaction.getDescription());
        existingTransaction.setAmount(updatedTransaction.getAmount());
        existingTransaction.setDate(updatedTransaction.getDate());

        Transaction savedTransaction = transactionRepository.save(existingTransaction);
        
        // Export to Excel after update
        excelExportService.exportTransactionsToExcel();
        
        return savedTransaction;
    }

    @Override
    public void deleteTransaction(Long id) {
        getTransactionById(id); // Check if it exists

        transactionRepository.deleteById(id);
        
        // Export to Excel after delete
        excelExportService.exportTransactionsToExcel();
    }

    @Override
    public void deleteAllTransactions() {
        transactionRepository.deleteAll();
        
        // Export to Excel after delete all
        excelExportService.exportTransactionsToExcel();
    }

    @Override
    @Transactional
    public List<Transaction> upsertTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (transaction.getId() != null) {
                try {
                    updateTransaction(transaction.getId(), transaction);
                } catch (TransactionNotFoundException e) {
                    // ID present in Excel but not in DB, treat as new
                    Long id = transaction.getId();
                    transaction.setId(null);
                    
                    Transaction newTransaction = transactionRepository.save(transaction);
                    newTransaction.setId(id);
                }
            } else {
                transactionRepository.save(transaction);
            }
        }

        return transactions;
    }
}
