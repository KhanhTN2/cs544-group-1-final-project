package com.cs544.notification.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
public class NotificationEmailProperties {
    private String from;
    private String defaultRecipient;
    private Map<String, String> developerEmails = new HashMap<>();
    private List<String> systemAlertRecipients = List.of();

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDefaultRecipient() {
        return defaultRecipient;
    }

    public void setDefaultRecipient(String defaultRecipient) {
        this.defaultRecipient = defaultRecipient;
    }

    public Map<String, String> getDeveloperEmails() {
        return developerEmails;
    }

    public void setDeveloperEmails(Map<String, String> developerEmails) {
        this.developerEmails = developerEmails;
    }

    public List<String> getSystemAlertRecipients() {
        return systemAlertRecipients;
    }

    public void setSystemAlertRecipients(List<String> systemAlertRecipients) {
        this.systemAlertRecipients = systemAlertRecipients;
    }
}
