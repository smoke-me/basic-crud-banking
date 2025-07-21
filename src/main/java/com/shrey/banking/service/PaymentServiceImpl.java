package com.shrey.banking.service;

import com.shrey.banking.entity.Recipient;
import com.shrey.banking.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private RecipientService recipientService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MailService mailService;

    @Override
    public Transaction makePayment(Long recipientId, Double amount) {
        Recipient recipient = recipientService.getRecipientById(recipientId);
        
        double paymentAmount = (amount != null) ? amount : recipient.getAmount();
        
        String description = "Payment to " + recipient.getDescription();
        
        Transaction transaction = Transaction.builder()
                .description(description)
                .amount(paymentAmount)
                .build();
        
        Transaction savedTransaction = transactionService.saveTransaction(transaction);
        
        mailService.sendPaymentEmail(recipient, savedTransaction);
        
        return savedTransaction;
    }
} 