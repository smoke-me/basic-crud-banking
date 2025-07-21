package com.shrey.banking.scheduler;

import com.shrey.banking.service.ExcelExportService;
import com.shrey.banking.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class ExcelImportScheduler {
    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private ExcelExportService excelExportService;

    @PostConstruct
    public void runOnStartup() {
        scheduleExcelImport();
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void scheduleExcelImport() {
        excelImportService.importTransactionsFromExcel();
        excelExportService.exportTransactionsToExcel();
    }
}
