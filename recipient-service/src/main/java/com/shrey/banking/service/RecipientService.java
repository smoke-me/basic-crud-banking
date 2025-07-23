package com.shrey.banking.service;

import com.shrey.banking.entity.Recipient;
import java.util.List;

public interface RecipientService {
    Recipient saveRecipient(Recipient recipient);
    List<Recipient> getAllRecipients();
    Recipient getRecipientById(Long id);
    Recipient updateRecipient(Long id, Recipient updated);
    Recipient deleteRecipient(Long id);
} 