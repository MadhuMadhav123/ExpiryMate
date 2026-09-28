package com.expirymate.notification.event;

public record UserRegisteredEvent(
        Long userId,
        String name,
        String email
) {
}
