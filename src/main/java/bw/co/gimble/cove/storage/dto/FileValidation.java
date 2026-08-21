package bw.co.gimble.cove.storage.dto;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public final class FileValidation {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> KYC_DOCUMENT_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");

    private FileValidation() {}

    /** Profile photos, organisation logos. */
    public static void requireImage(MultipartFile file) {
        require(file, IMAGE_TYPES, "Only JPEG, PNG or WEBP images are accepted");
    }

    /** KYC documents. */
    public static void requireKycDocument(MultipartFile file) {
        require(file, KYC_DOCUMENT_TYPES, "Only JPEG, PNG or PDF files are accepted");
    }

    private static void require(MultipartFile file, Set<String> allowedTypes, String message) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "A file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, message);
        }
    }
}
