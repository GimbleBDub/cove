package bw.co.gimble.cove.storage.dto;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FileValidation {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");
    private static final Set<String> KYC_DOCUMENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    /** Used when a client sends no type or a generic one (mobile pickers often send application/octet-stream). */
    private static final Map<String, String> TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("m4v", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("webm", "video/webm")
    );

    private FileValidation() {}

    /** Profile photos, organisation logos. */
    public static void requireImage(MultipartFile file) {
        require(file, IMAGE_TYPES, "Only JPEG, PNG or WEBP images are accepted");
    }

    /** KYC documents. */
    public static void requireKycDocument(MultipartFile file) {
        require(file, KYC_DOCUMENT_TYPES, "Only JPEG, PNG, WEBP or PDF files are accepted");
    }

    /** Property photos and walkthrough videos. Returns true when the file is a video. */
    public static boolean requirePropertyMedia(MultipartFile file) {
        String type = require(file, union(IMAGE_TYPES, VIDEO_TYPES),
                "Only photos (JPEG, PNG, WEBP) or videos (MP4, MOV, WEBM) are accepted");
        return VIDEO_TYPES.contains(type);
    }

    /**
     * The effective content type: the declared one when it is specific, otherwise
     * inferred from the file extension. Falls back to application/octet-stream.
     */
    public static String resolveContentType(MultipartFile file) {
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        int semicolon = declared.indexOf(';');
        if (semicolon >= 0) declared = declared.substring(0, semicolon).trim();
        if (!declared.isBlank() && !declared.equals("application/octet-stream")) {
            return declared;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            String inferred = TYPES_BY_EXTENSION.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (inferred != null) return inferred;
        }
        return "application/octet-stream";
    }

    private static String require(MultipartFile file, Set<String> allowedTypes, String message) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "A file is required");
        }
        String contentType = resolveContentType(file);
        if (!allowedTypes.contains(contentType)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, message);
        }
        return contentType;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        var all = new java.util.HashSet<>(a);
        all.addAll(b);
        return all;
    }
}
