package bw.co.gimble.cove.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Every controller returns ResponseEntity<ApiResponse<T>>. Shape never varies,
 * whether success or failure, so the frontend has exactly one thing to parse.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiError error;
    @Builder.Default
    private final Instant timestamp = Instant.now();
    private String path;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return ApiResponse.<T>builder().success(false).error(error).build();
    }
}
