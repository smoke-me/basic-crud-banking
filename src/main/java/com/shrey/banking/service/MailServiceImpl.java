package com.shrey.banking.service;

import com.shrey.banking.entity.Recipient;
import com.shrey.banking.entity.Transaction;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;

@Service
public class MailServiceImpl implements MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.excel.import.file.path}")
    private String excelPath;

    @Async("emailExecutor")
    public void sendTransactionCreatedEmail(Transaction transaction) {
        String toEmail = fromEmail; // Send to self

        String userName = fromEmail.split("@")[0];

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for attachments

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("New Transaction Created");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("description", transaction.getDescription());
            context.setVariable("amount", transaction.getAmount());
            context.setVariable("date", transaction.getDate());

            String htmlContent = templateEngine.process("transaction-created", context);

            helper.setText(htmlContent, true); // true for HTML

            // Attach Excel file with simple lock coordination
            ExcelFileLock.getLock().lock();
            try {
                File attachment = new File(excelPath);
                if (attachment.exists()) {
                    helper.addAttachment("transactions.xlsx", attachment);
                }
            } finally {
                ExcelFileLock.getLock().unlock();
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendTransactionUpdatedEmail(Transaction oldTransaction, Transaction newTransaction) {
        String toEmail = fromEmail; // Send to self

        String userName = fromEmail.split("@")[0];

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for attachments

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Transaction Updated");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("oldDescription", oldTransaction.getDescription());
            context.setVariable("oldAmount", oldTransaction.getAmount());
            context.setVariable("oldDate", oldTransaction.getDate());
            context.setVariable("newDescription", newTransaction.getDescription());
            context.setVariable("newAmount", newTransaction.getAmount());
            context.setVariable("newDate", newTransaction.getDate());

            String htmlContent = templateEngine.process("transaction-updated", context);

            helper.setText(htmlContent, true); // true for HTML

            // Attach Excel file with simple lock coordination
            ExcelFileLock.getLock().lock();
            try {
                File attachment = new File(excelPath);
                if (attachment.exists()) {
                    helper.addAttachment("transactions.xlsx", attachment);
                }
            } finally {
                ExcelFileLock.getLock().unlock();
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendTransactionDeletedEmail(Transaction transaction) {
        String toEmail = fromEmail; // Send to self

        String userName = fromEmail.split("@")[0];

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for attachments

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Transaction Deleted");

            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("description", transaction.getDescription());
            context.setVariable("amount", transaction.getAmount());
            context.setVariable("date", transaction.getDate());

            String htmlContent = templateEngine.process("transaction-deleted", context);

            helper.setText(htmlContent, true); // true for HTML

            // Attach Excel file with simple lock coordination
            ExcelFileLock.getLock().lock();
            try {
                File attachment = new File(excelPath);
                if (attachment.exists()) {
                    helper.addAttachment("transactions.xlsx", attachment);
                }
            } finally {
                ExcelFileLock.getLock().unlock();
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Async("emailExecutor")
    @Override
    public void sendPaymentEmail(Recipient recipient, Transaction transaction) {
        String toEmail = recipient.getEmail();

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true for attachments

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Payment Received");

            Context context = new Context();
            context.setVariable("recipientDescription", recipient.getDescription());
            context.setVariable("description", transaction.getDescription());
            context.setVariable("amount", transaction.getAmount());
            context.setVariable("date", transaction.getDate());

            String htmlContent = templateEngine.process("payment-sent", context);

            helper.setText(htmlContent, true); // true for HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}