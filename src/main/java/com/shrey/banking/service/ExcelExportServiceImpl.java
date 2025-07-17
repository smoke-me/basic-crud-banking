package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;
import com.shrey.banking.repository.TransactionRepository;
import com.shrey.banking.exception.ExcelExportException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("${excel.import.file.path}")
    private String excelFilePath;
    
    @Override
    public void exportTransactionsToExcel() {
        ExcelFileLock.getLock().lock();
        try {
            // Get all transactions directly from the repository to avoid a circular dependency,
            List<Transaction> transactions = transactionRepository.findAll();
            
            // Create or load existing workbook
            Workbook workbook;
            Sheet sheet;
            
            try {
                Resource resource = resourceLoader.getResource(excelFilePath);
                InputStream inputStream = resource.getInputStream();
                workbook = new XSSFWorkbook(inputStream);
                sheet = workbook.getSheetAt(0);
                inputStream.close();
            } catch (Exception e) {
                // if the Excel file does not exist, make it.
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("transactions");
            }
            
            // Clear existing data (keep only header or create it)
            clearSheetData(sheet);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0, CellType.STRING).setCellValue("ID");
            headerRow.createCell(1, CellType.STRING).setCellValue("Description");
            headerRow.createCell(2, CellType.STRING).setCellValue("Amount");
            headerRow.createCell(3, CellType.STRING).setCellValue("Date");
            
            // Hide ID column
            sheet.setColumnHidden(0, true);
            
            // Write transaction data
            for (int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);
                Row dataRow = sheet.createRow(i + 1);
                
                // ID (column 0)
                dataRow.createCell(0, CellType.NUMERIC).setCellValue(transaction.getId());
                
                // Description (column 1)
                dataRow.createCell(1, CellType.STRING).setCellValue(transaction.getDescription());
                
                // Amount (column 2)
                dataRow.createCell(2, CellType.NUMERIC).setCellValue(transaction.getAmount());
                
                // Date (column 3)
                LocalDate date = transaction.getDate();
                if (date != null) {
                    dataRow.createCell(3, CellType.STRING).setCellValue(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } else {
                    dataRow.createCell(3, CellType.STRING).setCellValue("");
                }
            }
            
            // Save workbook
            String filePath = Paths.get("src/main/resources/transactions.xlsx").toAbsolutePath().toString();
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
            workbook.close();
            
        } catch (Exception e) {
            throw new ExcelExportException(e.getMessage());
        } finally {
            ExcelFileLock.getLock().unlock();
        }
    }
    
    private void clearSheetData(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = lastRowNum; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }
} 