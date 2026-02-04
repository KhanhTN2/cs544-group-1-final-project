package com.cs544.notification.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.cs544.notification.config.NotificationEmailProperties;
import com.cs544.notification.model.NotificationLog;
import com.cs544.notification.repository.NotificationLogRepository;

@Service
public class NotificationEmailService {
    private final JavaMailSender mailSender;
    private final NotificationEmailProperties properties;
    private final NotificationLogRepository logRepository;

    public NotificationEmailService(
            JavaMailSender mailSender,
            NotificationEmailProperties properties,
            NotificationLogRepository logRepository
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.logRepository = logRepository;
    }

    public void send(
            String eventType,
            String source,
            String eventId,
            List<String> recipients,
            String subject,
            String body,
            Map<String, Object> payload
    ) {
        List<String> targetRecipients = recipients.isEmpty()
                ? List.of(properties.getDefaultRecipient())
                : recipients;
        for (String recipient : targetRecipients) {
            sendToRecipient(eventType, source, eventId, recipient, subject, body, payload);
        }
    }

    public String resolveDeveloperEmail(String developerId) {
        return properties.getDeveloperEmails().getOrDefault(developerId, properties.getDefaultRecipient());
    }

    public List<String> systemAlertRecipients() {
        List<String> configured = properties.getSystemAlertRecipients();
        if (configured == null || configured.isEmpty()) {
            return List.of(properties.getDefaultRecipient());
        }
        return new ArrayList<>(configured);
    }

    public List<String> resolveRelatedRecipients(Map<String, Object> payload) {
        if (payload != null) {
            Object developerId = payload.get("developerId");
            if (developerId != null && !String.valueOf(developerId).isBlank()) {
                return List.of(resolveDeveloperEmail(String.valueOf(developerId)));
            }
            Object assigneeId = payload.get("assigneeId");
            if (assigneeId != null && !String.valueOf(assigneeId).isBlank()) {
                return List.of(resolveDeveloperEmail(String.valueOf(assigneeId)));
            }
        }
        return List.of(properties.getDefaultRecipient());
    }

    private void sendToRecipient(
            String eventType,
            String source,
            String eventId,
            String recipient,
            String subject,
            String body,
            Map<String, Object> payload
    ) {
        String status = "SENT";
        String error = null;
        MailException failure = null;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException ex) {
            status = "FAILED";
            error = ex.getMessage();
            failure = ex;
        }
        NotificationLog log = new NotificationLog(
                eventType,
                recipient,
                subject,
                body,
                status,
                error,
                source,
                eventId,
                Instant.now(),
                payload
        );
        logRepository.save(log);
        if (failure != null) {
            throw failure;
        }
    }
}
