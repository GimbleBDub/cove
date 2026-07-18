package bw.co.gimble.cove.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ApiError {
    private String code;
    private String message;
    private List<FieldError> fieldErrors;

    public ApiError(String code, String message) {
        this(code, message, null);
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
