package com.expirymate.auth.event;

public record UserRegisteredEvent(Long userId, String name, String email) {

}
