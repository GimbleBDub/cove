package bw.co.gimble.cove.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ApiException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public static ApiException notFound(ErrorCode code, String message) {
        return new ApiException(code, message, HttpStatus.NOT_FOUND);
    }

    public static ApiException conflict(ErrorCode code, String message) {
        return new ApiException(code, message, HttpStatus.CONFLICT);
    }

    public static ApiException forbidden(ErrorCode code, String message) {
        return new ApiException(code, message, HttpStatus.FORBIDDEN);
    }

    public static ApiException unauthorized(ErrorCode code, String message) {
        return new ApiException(code, message, HttpStatus.UNAUTHORIZED);
    }

    public static ApiException badRequest(ErrorCode code, String message) {
        return new ApiException(code, message, HttpStatus.BAD_REQUEST);
    }
}