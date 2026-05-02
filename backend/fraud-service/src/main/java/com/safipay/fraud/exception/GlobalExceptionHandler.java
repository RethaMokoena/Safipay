package com.safipay.fraud.exception;
import com.safipay.fraud.dto.response.ApiResponse;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(FraudException.class)
    public ResponseEntity<ApiResponse<Void>> handle(FraudException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Error: " + e.getMessage()));
    }
}
