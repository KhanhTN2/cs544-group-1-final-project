package com.cs544.notification.service;

import java.time.Instant;

public record SystemErrorAlert(String id, String service, String message, Instant timestamp) {
}
