package com.shrey.banking.config;

import com.shrey.banking.service.ExcelImportService;
import com.shrey.banking.service.ExcelFileLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;

@Configuration
public class FileWatcherConfig {

    @Autowired
    private ExcelFileWatcher excelFileWatcher;

    @PostConstruct
    public void startWatcher() {
        excelFileWatcher.startWatching();
    }

    @PreDestroy
    public void stopWatcher() {
        excelFileWatcher.stopWatching();
    }

    @Component
    public static class ExcelFileWatcher {

        @Autowired
        private ExcelImportService excelImportService;

        @Value("${app.excel.import.file.path}")
        private String excelFilePath;

        private WatchService watchService;
        private volatile boolean running = false;

        @Async("fileOperationExecutor")
        public void startWatching() {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(excelFilePath).getParent();
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                running = true;
                while (running) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context().toString().equals("transactions.xlsx") && isUserModification()) {
                            processFileChange();
                        }
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                // Handle exceptions silently
            }
        }

        public void stopWatching() {
            running = false;
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    // Handle exception silently
                }
            }
        }

        private boolean isUserModification() {
            // Try to acquire lock immediately - if available, it's user modification
            if (ExcelFileLock.getLock().tryLock()) {
                ExcelFileLock.getLock().unlock();
                return true;
            }
            return false; // Lock is not available because the system is modifying
        }

        @Async("fileOperationExecutor")
        public void processFileChange() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            excelImportService.importTransactionsFromExcel();
        }
    }
} 