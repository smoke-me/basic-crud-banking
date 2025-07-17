package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ResourceLoader;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
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
        ExcelFileLock.getLock().lock();
        try {
            Resource resource = resourceLoader.getResource(excelFilePath);
            InputStream inputStream = resource.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            // Ensure ID column (insert at 0 if missing)
            Row header = sheet.getRow(0);
            if (header == null || !"ID".equals(getCellValueAsString(header.getCell(0)))) {
                insertIdColumn(sheet);
            }

            // Optionally hide ID column (col 0)
            sheet.setColumnHidden(0, true);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Parse ID (col 0) as double -> Long (if numeric)
                Long id = getCellValueAsLong(row.getCell(0));

                String description = getCellValueAsString(row.getCell(1));
                double amount = getCellValueAsDouble(row.getCell(2));
                LocalDate date = getCellValueAsLocalDate(row.getCell(3));

                Transaction transaction = Transaction.builder()
                        .id(id) // Set if present
                        .description(description)
                        .amount(amount)
                        .date(date)
                        .build();
                transactions.add(transaction);
            }

            inputStream.close();

            // Upsert and get back list with IDs
            transactions = transactionService.upsertTransactions(transactions);

            // Write back new/updated IDs
            for (int i = 0; i < transactions.size(); i++) {
                Transaction tx = transactions.get(i); // Fixed: get(i), not i+1
                Row row = sheet.getRow(i + 1); // Data row index
                Cell idCell = row.getCell(0);
                if (idCell == null || idCell.getNumericCellValue() != tx.getId().doubleValue()) {
                    if (idCell == null) {
                        idCell = row.createCell(0, CellType.NUMERIC);
                    }
                    idCell.setCellValue(tx.getId()); // Value implies numeric type
                }
            }

            // Save updated workbook
            String filePath = Paths.get("src/main/resources/transactions.xlsx").toAbsolutePath().toString();
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
            workbook.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel: " + e.getMessage());
        } finally {
            ExcelFileLock.getLock().unlock();
        }
    }

    private void insertIdColumn(Sheet sheet) {
        // Compute max column index across all rows
        int maxColumn = 0;
        int lastRowNum = sheet.getLastRowNum();
        for (int r = 0; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum > maxColumn) {
                    maxColumn = lastCellNum;
                }
            }
        }

        // Shift all existing columns right by 1 if any exist
        if (maxColumn > 0) {
            sheet.shiftColumns(0, maxColumn - 1, 1);
        }

        // Insert ID cells in new column 0 (create rows if needed)
        for (int r = 0; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                row = sheet.createRow(r);
            }
            if (r == 0) {
                row.createCell(0, CellType.STRING).setCellValue("ID"); // Header string
            } else {
                row.createCell(0, CellType.NUMERIC); // Data numeric, value later
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        return cell != null ? cell.getStringCellValue() : "";
    }

    private double getCellValueAsDouble(Cell cell) {
        return cell != null ? cell.getNumericCellValue() : 0;
    }


    private Long getCellValueAsLong(Cell cell) {
        double value = getCellValueAsDouble(cell);
        long id = (long) value;
        return (id > 0) ? id : null;
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
