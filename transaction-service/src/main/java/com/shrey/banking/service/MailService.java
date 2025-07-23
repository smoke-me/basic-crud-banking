package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;

public interface MailService {
    public void sendTransactionCreatedEmail(Transaction transaction);
    public void sendTransactionUpdatedEmail(Transaction oldTransaction, Transaction newTransaction);
    public void sendTransactionDeletedEmail(Transaction transaction);
}
