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

            int rowIndex = findNextAvailableRow(sheet);
            Row dataRow = getOrCreateRow(sheet, rowIndex);
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
            int lastRowWithData = findLastRowWithData(sheet);
            for (int i = 1; i <= lastRowWithData; i++) {
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
                int rowIndex = findNextAvailableRow(sheet);
                writeTransactionRow(getOrCreateRow(sheet, rowIndex), updatedTransaction);
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
            int lastRowWithData = findLastRowWithData(sheet);
            for (int i = 1; i <= lastRowWithData; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell idCell = row.getCell(0);
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC && (long) idCell.getNumericCellValue() == id) {
                    sheet.removeRow(row);
                    int lastRow = findLastRowWithData(sheet);
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

    /**
     * Find the next available row for writing data, treating empty rows and phantom rows the same.
     * Scans from row 1 (after header) and finds the first truly empty row.
     */
    private int findNextAvailableRow(Sheet sheet) {
        // Start from row 1 (after header row 0)
        int rowIndex = 1;
        
        // Look for the first empty row by checking if it has meaningful data
        while (rowIndex < 1048575) { // Stay well below Excel's limit
            Row row = sheet.getRow(rowIndex);
            if (isRowEmpty(row)) {
                return rowIndex;
            }
            rowIndex++;
        }
        
        // If we somehow reach here, something is very wrong
        throw new ExcelExportException("Unable to find available row in Excel sheet");
    }

    /**
     * Find the last row that actually contains data, avoiding phantom rows
     */
    private int findLastRowWithData(Sheet sheet) {
        // Start from a reasonable limit and work backwards
        int maxRowToCheck = Math.min(sheet.getLastRowNum(), 10000); // Cap at 10k rows for safety
        
        for (int i = maxRowToCheck; i >= 1; i--) {
            Row row = sheet.getRow(i);
            if (row != null && hasData(row)) {
                return i;
            }
        }
        return 0; // No data rows found
    }

    /**
     * Check if a row has any meaningful data
     */
    private boolean hasData(Row row) {
        // Check if any of the relevant cells have data
        String description = getCellValueAsString(row.getCell(1));
        double amount = getCellValueAsDouble(row.getCell(2));
        
        return !description.trim().isEmpty() || amount != 0;
    }
    
    /**
     * Check if a row is empty (treats null rows and rows with empty data the same)
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        // Check if all relevant cells are empty
        Cell idCell = row.getCell(0);
        Cell descCell = row.getCell(1);
        Cell amountCell = row.getCell(2);
        Cell dateCell = row.getCell(3);
        
        return (idCell == null || getCellValueAsString(idCell).trim().isEmpty()) &&
               (descCell == null || getCellValueAsString(descCell).trim().isEmpty()) &&
               (amountCell == null || getCellValueAsDouble(amountCell) == 0) &&
               (dateCell == null || getCellValueAsString(dateCell).trim().isEmpty());
    }
    
    /**
     * Get existing row or create new one if it doesn't exist
     */
    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return (row != null) ? row : sheet.createRow(rowIndex);
    }
    
    /**
     * Helper method to get cell value as string safely
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
    
    /**
     * Helper method to get cell value as double safely
     */
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return 0;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? 0 : Double.parseDouble(value);
                default:
                    return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void clearDataRows(Sheet sheet) {
        int lastRowWithData = findLastRowWithData(sheet);
        for (int i = lastRowWithData; i >= 1; i--) {
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