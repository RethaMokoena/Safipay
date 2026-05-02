package com.safipay.webhook.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*; import java.time.LocalDateTime;
@Data @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success; private String message; private T data;
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
    public static <T> ApiResponse<T> success(T d) { return ApiResponse.<T>builder().success(true).data(d).build(); }
    public static <T> ApiResponse<T> success(String m, T d) { return ApiResponse.<T>builder().success(true).message(m).data(d).build(); }
    public static <T> ApiResponse<T> error(String m) { return ApiResponse.<T>builder().success(false).message(m).build(); }
}
