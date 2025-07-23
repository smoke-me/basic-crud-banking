package com.shrey.banking.feignclient;

import com.shrey.banking.dto.TransactionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service", url = "${transaction-service.url}")
public interface TransactionClient {

    @PostMapping("/transactions")
    TransactionDTO createTransaction(@RequestBody TransactionDTO transaction);
}