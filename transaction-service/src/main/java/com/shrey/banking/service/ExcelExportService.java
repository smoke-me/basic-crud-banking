package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;

public interface ExcelExportService {
    void exportTransactionsToExcel();
    void saveTransactionToExcel(Transaction transaction);
    void updateTransactionInExcel(Long id, Transaction updatedTransaction);
    void deleteTransactionInExcel(Long id);
} 