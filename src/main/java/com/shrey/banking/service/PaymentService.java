package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;

public interface PaymentService {
    Transaction makePayment(Long recipientId, Double amount);
} 