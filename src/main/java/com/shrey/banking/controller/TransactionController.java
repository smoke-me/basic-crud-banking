package com.shrey.banking.controller;

import com.shrey.banking.entity.Transaction;
import com.shrey.banking.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction)
    {
        return transactionService.saveTransaction(transaction);
    }

    @GetMapping
    public List<Transaction> getAll()
    {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getOne(@PathVariable Long id)
    {
        return transactionService.getTransactionById(id);
    }

    @PutMapping("/{id}")
    public Transaction update(@PathVariable Long id, @RequestBody Transaction updatedTransaction)
    {
        return transactionService.updateTransaction(id, updatedTransaction);
    }

    @DeleteMapping("/{id}")
    public Transaction delete(@PathVariable Long id)
    {
        return transactionService.deleteTransaction(id);
    }
}
