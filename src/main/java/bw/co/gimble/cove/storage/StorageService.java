package bw.co.gimble.cove.storage;

import bw.co.gimble.cove.storage.dto.FileValidation;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;
    private final Set<String> readyBuckets = ConcurrentHashMap.newKeySet();


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

    public UploadedFile uploadPropertyMedia(UUID propertyId, MultipartFile file) {
        String bucket = properties.getPropertyMediaBucket();
        String key = put(bucket, "properties/" + propertyId, file);
        return new UploadedFile(key, publicUrl(bucket, key));
    }

    public void deletePropertyMedia(String key) {
        delete(properties.getPropertyMediaBucket(), key);
    }

    public UploadedFile uploadSignature(UUID applicationId, MultipartFile file) {
        String bucket = properties.getPropertyMediaBucket();
        String key = put(bucket, "signatures/" + applicationId, file);
        return new UploadedFile(key, publicUrl(bucket, key));
    }

    private String put(String bucket, String keyPrefix, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "A file is required");
        }
        ensureBucket(bucket);

        String key = buildKey(keyPrefix, file.getOriginalFilename());

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(FileValidation.resolveContentType(file))
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

    private void ensureBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Storage bucket is not configured");
        }
        try {
            if (readyBuckets.contains(bucket)) return;
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            applyPublicReadPolicy(bucket);
            readyBuckets.add(bucket);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to ensure bucket {}: {}", bucket, e.getMessage());
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Failed to store the uploaded file",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Make sure public buckets are readable on startup, so existing media works without a fresh upload. */
    @EventListener(ApplicationReadyEvent.class)
    public void preparePublicBuckets() {
        for (String bucket : List.of(properties.getPropertyMediaBucket(), properties.getProfileBucket(), properties.getOrganisationsBucket())) {
            try {
                ensureBucket(bucket);
            } catch (Exception e) {
                log.warn("Could not prepare storage bucket {} at startup: {}", bucket, e.getMessage());
            }
        }
    }

    /**
     * Listing photos/videos, profile photos and organisation logos are served
     * straight from storage via publicUrl(), so their prefixes must be
     * anonymously readable - without this every image 403s. Everything else
     * (KYC documents, lease signatures) stays private and is only reachable
     * through presigned URLs.
     */
    private void applyPublicReadPolicy(String bucket) throws Exception {
        List<String> prefixes = new ArrayList<>();
        if (bucket.equals(properties.getPropertyMediaBucket())) prefixes.add("properties/");
        if (bucket.equals(properties.getProfileBucket())) prefixes.add("profile-photos/");
        if (bucket.equals(properties.getOrganisationsBucket())) prefixes.add("org-logos/");
        if (prefixes.isEmpty()) return;
        String resources = prefixes.stream()
                .map(prefix -> "\"arn:aws:s3:::" + bucket + "/" + prefix + "*\"")
                .collect(Collectors.joining(","));
        String policy = """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
                "Action":["s3:GetObject"],"Resource":[%s]}]}""".formatted(resources);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
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