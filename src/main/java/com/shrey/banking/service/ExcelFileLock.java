package com.shrey.banking.service;

import lombok.Getter;

import java.util.concurrent.locks.ReentrantLock;

public class ExcelFileLock {
    @Getter
    private static final ReentrantLock lock = new ReentrantLock();

} 