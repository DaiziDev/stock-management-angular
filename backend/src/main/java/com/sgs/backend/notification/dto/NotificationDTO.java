package com.sgs.backend.notification.dto;

public record NotificationDTO(
        String type,
        String message,
        Long articleId,
        String articleDesignation
) {}
