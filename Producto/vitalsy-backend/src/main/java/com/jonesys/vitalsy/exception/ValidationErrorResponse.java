package com.jonesys.vitalsy.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    
    private int status;
    private String message;
    private ZonedDateTime timestamp;
    private String path;
    private Map<String, String> errors;
}
