package com.safipay.ledger.exception;
import com.safipay.ledger.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(LedgerException.class)
    public ResponseEntity<ApiResponse<Void>> handle(LedgerException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception e) {
        log.error("Ledger error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Ledger error: " + e.getMessage()));
    }
}
