package com.shrey.banking.service;

import com.shrey.banking.dto.TransactionDTO;
import com.shrey.banking.entity.Recipient;
import com.shrey.banking.feignclient.TransactionClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private RecipientService recipientService;

    @Autowired
    private MailService mailService;

    @Autowired
    private TransactionClient transactionClient;

    @Override
    public TransactionDTO makePayment(Long recipientId, Double amount) {
        Recipient recipient = recipientService.getRecipientById(recipientId);

        double paymentAmount = (amount != null) ? amount : recipient.getAmount();

        String description = "Payment to " + recipient.getDescription();

        TransactionDTO transactionToCreate = new TransactionDTO(null, description, paymentAmount, null);

        TransactionDTO savedTransaction = transactionClient.createTransaction(transactionToCreate);

        mailService.sendPaymentEmail(recipient, savedTransaction);

        return savedTransaction;
    }
}