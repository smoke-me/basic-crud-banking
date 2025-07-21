package com.shrey.banking.controller;

import com.shrey.banking.entity.Recipient;
import com.shrey.banking.service.RecipientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipients")
public class RecipientController {
    @Autowired
    private RecipientService recipientService;

    @PostMapping
    public Recipient create(@RequestBody Recipient recipient)
    {
        return recipientService.saveRecipient(recipient);
    }

    @GetMapping
    public List<Recipient> getAll()
    {
        return recipientService.getAllRecipients();
    }

    @GetMapping("/{id}")
    public Recipient getOne(@PathVariable Long id)
    {
        return recipientService.getRecipientById(id);
    }

    @PutMapping("/{id}")
    public Recipient update(@PathVariable Long id, @RequestBody Recipient updatedRecipient)
    {
        return recipientService.updateRecipient(id, updatedRecipient);
    }

    @DeleteMapping("/{id}")
    public Recipient delete(@PathVariable Long id)
    {
        return recipientService.deleteRecipient(id);
    }
} 