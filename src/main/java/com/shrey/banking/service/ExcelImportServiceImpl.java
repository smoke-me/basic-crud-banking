package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportServiceImpl implements ExcelImportService {
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${excel.import.file.path}")
    private String excelFilePath;

    @Override
    public void importTransactionsFromExcel() {
        List<Transaction> transactions = new ArrayList<>();

        try  {
            Resource resource = resourceLoader.getResource(excelFilePath);
            InputStream inputStream = resource.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) { continue; }

                String description = getCellValueAsString(row.getCell(0));
                double amount = getCellValueAsDouble(row.getCell(1));
                LocalDate date = getCellValueAsLocalDate(row.getCell(2));

                Transaction transaction = Transaction.builder()
                        .description(description)
                        .amount(amount)
                        .date(date)
                        .build();
                transactions.add(transaction);
            }

            workbook.close();
            inputStream.close();

            transactionService.deleteAllTransactions();
            transactionService.saveAllTransactions(transactions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel: " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        return cell != null ? cell.getStringCellValue() : "";
    }

    private double getCellValueAsDouble(Cell cell) {
        return cell != null ? cell.getNumericCellValue() : 0;
    }

    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell != null) {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {

                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {

                String dateStr = cell.getStringCellValue();
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        }

        return LocalDate.now();
    }
}
