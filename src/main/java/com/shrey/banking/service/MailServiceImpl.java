package com.shrey.banking.service;

import com.shrey.banking.entity.Transaction;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    public void sendTransactionCreatedEmail(Transaction transaction) {
        String toEmail = fromEmail; // Send to self

        String userName = fromEmail.split("@")[0];;

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

            // Attach Excel
            File attachment = new File(excelPath);
            helper.addAttachment(attachment.getName(), attachment);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}