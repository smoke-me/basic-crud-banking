package com.shrey.banking.service;

import com.shrey.banking.entity.Recipient;
import com.shrey.banking.exception.RecipientNotFoundException;
import com.shrey.banking.repository.RecipientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipientServiceImpl implements RecipientService {

    @Autowired
    private RecipientRepository recipientRepository;

    @Override
    public Recipient saveRecipient(Recipient recipient) {
        return recipientRepository.save(recipient);
    }

    @Override
    public List<Recipient> getAllRecipients() {
        return recipientRepository.findAll();
    }

    @Override
    public Recipient getRecipientById(Long id) {
        return recipientRepository.findById(id).orElseThrow(() -> new RecipientNotFoundException(id));
    }

    @Override
    public Recipient updateRecipient(Long id, Recipient updatedRecipient) {
        Recipient existingRecipient = getRecipientById(id);
        
        existingRecipient.setDescription(updatedRecipient.getDescription());
        existingRecipient.setEmail(updatedRecipient.getEmail());
        existingRecipient.setAmount(updatedRecipient.getAmount());

        return recipientRepository.save(existingRecipient);
    }

    @Override
    public Recipient deleteRecipient(Long id) {
        Recipient recipient = getRecipientById(id);
        recipientRepository.deleteById(id);
        return recipient;
    }
} 