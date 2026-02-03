package com.cs544.aichat.event;

public record SystemErrorEvent(String service, String message) {
}
