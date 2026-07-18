package bw.co.gimble.cove.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest req) {
        ApiError error = new ApiError(ex.getErrorCode().name(), ex.getMessage());
        return respond(ex.getStatus(), error, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError error = new ApiError(ErrorCode.VALIDATION_FAILED.name(), "Validation failed", fieldErrors);
        return respond(HttpStatus.BAD_REQUEST, error, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ApiError error = new ApiError(ErrorCode.ORG_ROLE_FORBIDDEN.name(), "You do not have permission to perform this action");
        return respond(HttpStatus.FORBIDDEN, error, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest req) {
        ApiError error = new ApiError(ErrorCode.UNEXPECTED_ERROR.name(), "Something went wrong");
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, error, req);
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, ApiError error, HttpServletRequest req) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .error(error)
                .path(req.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
