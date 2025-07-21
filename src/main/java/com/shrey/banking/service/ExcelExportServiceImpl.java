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
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("${app.excel.import.file.path}")
    private String excelFilePath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public void exportTransactionsToExcel() {
        ExcelFileLock.getLock().lock();
        try {
            List<Transaction> transactions = transactionRepository.findAll();

            Workbook workbook = openWorkbook();
            Sheet sheet = getOrCreateSheet(workbook);

            ensureHeaderRow(sheet);
            clearDataRows(sheet);

            for (int i = 0; i < transactions.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                writeTransactionRow(dataRow, transactions.get(i));
            }

            writeAndClose(workbook);
        } catch (Exception e) {
            throw new ExcelExportException(e.getMessage());
        } finally {
            ExcelFileLock.getLock().unlock();
        }
    }
    
    @Override
    public void saveTransactionToExcel(Transaction transaction) {
        try {
            Workbook workbook = openWorkbook();
            Sheet sheet = getOrCreateSheet(workbook);

            ensureHeaderRow(sheet);

            int rowIndex = sheet.getLastRowNum() + 1;
            Row dataRow = sheet.createRow(rowIndex);
            writeTransactionRow(dataRow, transaction);

            writeAndClose(workbook);
        } catch (Exception e) {
            throw new ExcelExportException(e.getMessage());
        }
    }

    @Override
    public void updateTransactionInExcel(Long id, Transaction updatedTransaction) {
        try {
            Workbook workbook = openWorkbook();
            Sheet sheet = getOrCreateSheet(workbook);

            ensureHeaderRow(sheet);

            boolean found = false;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell idCell = row.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC && (long) idCell.getNumericCellValue() == id) {
                    writeTransactionRow(row, updatedTransaction);
                    found = true;
                    break;
                }
            }

            if (!found) {
                int rowIndex = sheet.getLastRowNum() + 1;
                writeTransactionRow(sheet.createRow(rowIndex), updatedTransaction);
            }

            writeAndClose(workbook);
        } catch (Exception e) {
            throw new ExcelExportException(e.getMessage());
        }
    }

    @Override
    public void deleteTransactionInExcel(Long id) {
        try {
            Workbook workbook = openWorkbook();
            Sheet sheet = getOrCreateSheet(workbook);

            boolean deleted = false;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell idCell = row.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC && (long) idCell.getNumericCellValue() == id) {
                    sheet.removeRow(row);
                    int lastRow = sheet.getLastRowNum();
                    if (i < lastRow) {
                        sheet.shiftRows(i + 1, lastRow, -1);
                    }
                    deleted = true;
                    break;
                }
            }

            if (deleted) {
                writeAndClose(workbook);
            } else {
                workbook.close();
            }
        } catch (Exception e) {
            throw new ExcelExportException(e.getMessage());
        }
    }

    private void clearDataRows(Sheet sheet) {
        for (int i = sheet.getLastRowNum(); i >= 1; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    private Workbook openWorkbook() throws IOException {
        try {
            Resource resource = resourceLoader.getResource("file:" + excelFilePath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return new XSSFWorkbook(is);
                }
            }
        } catch (Exception ignored) {
        }
        return new XSSFWorkbook();
    }

    private Sheet getOrCreateSheet(Workbook workbook) {
        return (workbook.getNumberOfSheets() > 0) ? workbook.getSheetAt(0) : workbook.createSheet("transactions");
    }

    private void writeAndClose(Workbook workbook) throws IOException {
        try (FileOutputStream os = new FileOutputStream(excelFilePath)) {
            workbook.write(os);
        } finally {
            workbook.close();
        }
    }

    private void writeTransactionRow(Row row, Transaction transaction) {
        row.createCell(0, CellType.NUMERIC).setCellValue(transaction.getId());
        row.createCell(1, CellType.STRING).setCellValue(transaction.getDescription());
        row.createCell(2, CellType.NUMERIC).setCellValue(transaction.getAmount());
        LocalDate date = transaction.getDate();
        if (date != null) {
            row.createCell(3, CellType.STRING).setCellValue(date.format(DATE_FORMATTER));
        } else {
            row.createCell(3, CellType.STRING).setCellValue("");
        }
    }

    private void ensureHeaderRow(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            headerRow = sheet.createRow(0);
        }
        Cell idHeader = headerRow.getCell(0);
        if (idHeader == null || !"ID".equals(idHeader.getStringCellValue())) {
            headerRow.createCell(0, CellType.STRING).setCellValue("ID");
            headerRow.createCell(1, CellType.STRING).setCellValue("Description");
            headerRow.createCell(2, CellType.STRING).setCellValue("Amount");
            headerRow.createCell(3, CellType.STRING).setCellValue("Date");
        }
        sheet.setColumnHidden(0, true);
    }
} 