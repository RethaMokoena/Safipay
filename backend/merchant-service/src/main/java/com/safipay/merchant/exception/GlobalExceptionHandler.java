package com.safipay.merchant.exception;
import com.safipay.merchant.dto.response.ApiResponse;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MerchantException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MerchantException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Error: " + e.getMessage()));
    }
}
