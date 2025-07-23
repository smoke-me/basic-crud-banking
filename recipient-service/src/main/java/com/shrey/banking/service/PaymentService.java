package com.shrey.banking.service;

import com.shrey.banking.dto.TransactionDTO;

public interface PaymentService {
    TransactionDTO makePayment(Long recipientId, Double amount);
}