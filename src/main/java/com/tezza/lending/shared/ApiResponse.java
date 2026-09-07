package com.tezza.lending.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final String correlationId;
    private final T data;

    private ApiResponse(int status, String message, String correlationId, T data) {
        this.status = status;
        this.message = message;
        this.correlationId = correlationId;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "Success", UUID.randomUUID().toString(), data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, UUID.randomUUID().toString(), data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(1, message, UUID.randomUUID().toString(), null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(1, message, UUID.randomUUID().toString(), data);
    }
}
