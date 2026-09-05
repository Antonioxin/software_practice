package com.wemove.identity.api;

public record ApiEnvelope<T>(T data, Object meta) {
    public static <T> ApiEnvelope<T> of(T data) { return new ApiEnvelope<>(data, null); }
    public static <T> ApiEnvelope<T> page(T data, Object meta) { return new ApiEnvelope<>(data, meta); }
}
