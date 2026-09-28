package com.expirymate.notification.event;

import java.time.LocalDate;

public record DocumentExpiringEvent(Long id, Long userId, String ownerEmail, String name, String category,
                                    String documentNumber,
                                    LocalDate issueDate, LocalDate expiryDate, String notes, String status) {
}
