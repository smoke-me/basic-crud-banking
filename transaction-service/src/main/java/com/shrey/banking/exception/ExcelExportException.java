package com.shrey.banking.exception;

public class ExcelExportException extends RuntimeException {
    public ExcelExportException(String errorMessage) {
        super("Failed to export to Excel: " + errorMessage);
    }
} 