package com.jonesys.vitalsy.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message
) {}
