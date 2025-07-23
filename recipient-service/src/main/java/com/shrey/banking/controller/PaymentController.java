package com.shrey.banking.controller;

import com.shrey.banking.dto.TransactionDTO;
import com.shrey.banking.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/{recipientId}")
    public TransactionDTO makePayment(@PathVariable Long recipientId, @RequestParam(required = false) Double amount)
    {
        return paymentService.makePayment(recipientId, amount);
    }
}