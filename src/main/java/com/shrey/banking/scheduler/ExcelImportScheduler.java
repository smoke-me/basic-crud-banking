package com.shrey.banking.scheduler;

import com.shrey.banking.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExcelImportScheduler {
    @Autowired
    private ExcelImportService excelImportService;

    @Scheduled(fixedRate = 15000) // 15 seconds
    public void scheduleExcelImport() {
        excelImportService.importTransactionsFromExcel();
    }
}
