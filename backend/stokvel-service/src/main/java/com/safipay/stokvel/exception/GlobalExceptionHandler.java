package com.safipay.stokvel.exception;
import com.safipay.stokvel.dto.response.ApiResponse;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(StokvelException.class)
    public ResponseEntity<ApiResponse<Void>> handle(StokvelException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Unexpected error: " + e.getMessage()));
    }
}
