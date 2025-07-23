package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;
import com.shrey.banking.exception.TransactionNotFoundException;
import com.shrey.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private MailService mailService;

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        Transaction savedTransaction;
        ExcelFileLock.getLock().lock();
        try {
            if (transaction.getDate() == null) {
                transaction.setDate(LocalDate.now());
            }

            savedTransaction = transactionRepository.save(transaction);
            
            excelExportService.saveTransactionToExcel(savedTransaction);

        } finally {
            ExcelFileLock.getLock().unlock();
        }
        
        mailService.sendTransactionCreatedEmail(savedTransaction);
        
        return savedTransaction;
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
        Transaction oldTransaction;
        Transaction savedTransaction;
        ExcelFileLock.getLock().lock();
        try {
            Transaction existingTransaction = getTransactionById(id);
            
            oldTransaction = Transaction.builder()
                    .id(existingTransaction.getId())
                    .description(existingTransaction.getDescription())
                    .amount(existingTransaction.getAmount())
                    .date(existingTransaction.getDate())
                    .build();
            
            existingTransaction.setDescription(updatedTransaction.getDescription());
            existingTransaction.setAmount(updatedTransaction.getAmount());
            existingTransaction.setDate(updatedTransaction.getDate());

            savedTransaction = transactionRepository.save(existingTransaction);
            
            excelExportService.updateTransactionInExcel(id, updatedTransaction);

        } finally {
            ExcelFileLock.getLock().unlock();
        }
        
        mailService.sendTransactionUpdatedEmail(oldTransaction, savedTransaction);

        return savedTransaction;
    }

    @Override
    public Transaction deleteTransaction(Long id) {
        Transaction transaction;
        ExcelFileLock.getLock().lock();
        try {
            transaction = getTransactionById(id);
            transactionRepository.deleteById(id);
            
            excelExportService.deleteTransactionInExcel(id);

        } finally {
            ExcelFileLock.getLock().unlock();
        }

        mailService.sendTransactionDeletedEmail(transaction);

        return transaction;
    }

    @Override
    @Transactional
    public void upsertTransactions(List<Transaction> transactions) {
        // Get all existing transaction IDs from database
        List<Long> existingIds = transactionRepository.findAll()
                .stream()
                .map(Transaction::getId)
                .toList();

        // Get all transaction IDs from Excel (only those with IDs)
        List<Long> excelIds = transactions.stream()
                .map(Transaction::getId)
                .filter(Objects::nonNull)
                .toList();

        // Find IDs that exist in DB but not in Excel (these should be deleted)
        List<Long> idsToDelete = existingIds.stream()
                .filter(id -> !excelIds.contains(id))
                .toList();

        // Delete transactions that are no longer in Excel
        if (!idsToDelete.isEmpty()) {
            transactionRepository.deleteAllById(idsToDelete);
        }

        // Process upserts for transactions from Excel
        for (Transaction transaction : transactions) {
            if (transaction.getId() != null) {
                try {
                    Transaction existingTransaction = getTransactionById(transaction.getId());
                    existingTransaction.setDescription(transaction.getDescription());
                    existingTransaction.setAmount(transaction.getAmount());
                    existingTransaction.setDate(transaction.getDate());

                    transactionRepository.save(existingTransaction);
                } catch (TransactionNotFoundException e) {
                    // ID present in Excel but not in DB, treat as new
                    transaction.setId(null);
                    transactionRepository.save(transaction);
                }
            } else {
                transactionRepository.save(transaction);
            }
        }
    }
}
