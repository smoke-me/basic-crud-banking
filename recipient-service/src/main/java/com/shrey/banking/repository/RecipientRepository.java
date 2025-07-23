package com.shrey.banking.repository;

import com.shrey.banking.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, Long> { } 