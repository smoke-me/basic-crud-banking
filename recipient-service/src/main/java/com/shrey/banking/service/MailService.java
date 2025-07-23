package com.shrey.banking.service;

import com.shrey.banking.dto.TransactionDTO;
import com.shrey.banking.entity.Recipient;

public interface MailService {
    void sendPaymentEmail(Recipient recipient, TransactionDTO transaction);
}