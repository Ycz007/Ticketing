package com.ticketing.auth.model;

public record Account(Long id, String username, String displayName, String passwordHash, boolean enabled) { }
