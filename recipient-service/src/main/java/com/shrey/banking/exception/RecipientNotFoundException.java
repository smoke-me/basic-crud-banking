package com.shrey.banking.exception;

public class RecipientNotFoundException extends RuntimeException {
    public RecipientNotFoundException(Long id) {
        super("Recipient with ID " + id + " not found.");
    }
} 