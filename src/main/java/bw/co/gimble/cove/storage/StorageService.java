package bw.co.gimble.cove.storage;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;


    public UploadedFile uploadKycDocument(UUID userId, MultipartFile file) {
        String key = put(properties.getKycBucket(), "kyc/" + userId, file);
        return new UploadedFile(key, null);
    }

    public String presignKycDocumentUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return presign(properties.getKycBucket(), key);
    }

    public void deleteKycDocument(String key) {
        delete(properties.getKycBucket(), key);
    }


    public UploadedFile uploadProfilePhoto(UUID userId, MultipartFile file) {
        String key = put(properties.getProfileBucket(), "profile-photos/" + userId, file);
        return new UploadedFile(key, publicUrl(properties.getProfileBucket(), key));
    }

    public void deleteProfilePhoto(String key) {
        delete(properties.getProfileBucket(), key);
    }


    public UploadedFile uploadOrganisationLogo(UUID organisationId, MultipartFile file) {
        String key = put(properties.getOrganisationsBucket(), "org-logos/" + organisationId, file);
        return new UploadedFile(key, publicUrl(properties.getOrganisationsBucket(), key));
    }

    public void deleteOrganisationLogo(String key) {
        delete(properties.getOrganisationsBucket(), key);
    }

    private String put(String bucket, String keyPrefix, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "A file is required");
        }

        String key = buildKey(keyPrefix, file.getOriginalFilename());

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload {} to bucket {}: {}", key, bucket, e.getMessage());
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Failed to store the uploaded file",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return key;
    }

    private String presign(String bucket, String key) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) properties.getPresignedUrlTtlMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("Failed to presign {} in bucket {}: {}", key, bucket, e.getMessage());
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Failed to generate a document link",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void delete(String bucket, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete storage object {} from bucket {}: {}", key, bucket, e.getMessage());
        }
    }

    private String buildKey(String keyPrefix, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            extension = extension.replaceAll("[^a-zA-Z0-9.]", "");
        }
        String prefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        return prefix + UUID.randomUUID() + extension;
    }

    private String publicUrl(String bucket, String key) {
        String base = (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank())
                ? properties.getPublicBaseUrl()
                : properties.getEndpoint();
        return base.replaceAll("/+$", "") + "/" + bucket + "/" + key;
    }
}