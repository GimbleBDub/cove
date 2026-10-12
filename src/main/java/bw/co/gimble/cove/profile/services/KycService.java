package bw.co.gimble.cove.profile.services;


import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.events.KycDocumentSubmittedEvent;
import bw.co.gimble.cove.profile.dto.kyc.CreateKycRequest;
import bw.co.gimble.cove.profile.dto.kyc.KycResponse;
import bw.co.gimble.cove.profile.dto.kyc.KycSummaryResponse;
import bw.co.gimble.cove.profile.dto.kyc.UpdateKycRequest;
import bw.co.gimble.cove.profile.models.UserKyc;
import bw.co.gimble.cove.profile.repositories.UserKycRepository;
import bw.co.gimble.cove.profile.enums.KycStatus;
import bw.co.gimble.cove.storage.StorageService;
import bw.co.gimble.cove.storage.dto.FileValidation;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class KycService {

    private final UserKycRepository repository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;
    private final PermissionService permissionService;

    @Transactional(readOnly=true)
    public List<KycResponse> getMyDocuments(){

        User user = getCurrentUser();

        return repository.findByUserId(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public KycResponse get(UUID id) {

        User user = getCurrentUser();

        UserKyc kyc = repository
                .findByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        ApiException.notFound("KYC document not found"));

        return map(kyc);
    }

    /** file is the raw upload; request carries only metadata (see CreateKycRequest). */
    public KycResponse create(MultipartFile file, CreateKycRequest request){

        User user = getCurrentUser();

        FileValidation.requireKycDocument(file);
        UploadedFile uploaded = storageService.uploadKycDocument(user.getId(), file);

        UserKyc kyc = UserKyc.builder()
                .user(user)
                .documentType(request.documentType())
                // The bucket KEY is stored, not a URL - KYC docs are private, so a usable
                // link only ever exists as a freshly-generated presigned URL (see map()).
                .fileUrl(uploaded.key())
                .documentNumber(request.documentNumber())
                .issueDate(request.issueDate())
                .expiryDate(request.expiryDate())
                .status(KycStatus.PENDING)
                .build();

        kyc = repository.save(kyc);

        eventPublisher.publishEvent(new KycDocumentSubmittedEvent(kyc.getId(), user.getId(), kyc.getDocumentType()));

        return map(kyc);
    }

    public void delete(UUID id) {
        User user = getCurrentUser();
        UserKyc kyc = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("KYC document not found"));
        if (kyc.getStatus() != KycStatus.PENDING) {
            throw ApiException.conflict(ErrorCode.KYC_ALREADY_DECIDED,
                    "Only documents awaiting review can be removed");
        }
        storageService.deleteKycDocument(kyc.getFileUrl());
        repository.delete(kyc);
    }

    @Transactional(readOnly = true)
    public KycSummaryResponse getSummary() {

        User user = getCurrentUser();

        List<UserKyc> documents = repository.findByUserId(user.getId());


        int total = documents.size();

        int verified = (int) documents.stream()
                .filter(k -> k.getStatus() == KycStatus.VERIFIED)
                .count();

        int pending = (int) documents.stream()
                .filter(k -> k.getStatus() == KycStatus.PENDING ||
                        k.getStatus() == KycStatus.UNDER_REVIEW)
                .count();

        int rejected = (int) documents.stream()
                .filter(k -> k.getStatus() == KycStatus.REJECTED)
                .count();

        int expired = (int) documents.stream()
                .filter(k -> k.getStatus() == KycStatus.EXPIRED)
                .count();


        return new KycSummaryResponse(
                total,
                verified,
                pending,
                rejected,
                expired,
                total > 0 && rejected == 0 && pending == 0 && expired == 0
        );
    }

    /** file is optional - omit it to update metadata only and keep the previously uploaded document. */
    public KycResponse update(
            UUID id,
            MultipartFile file,
            UpdateKycRequest request){

        User user = getCurrentUser();


        UserKyc kyc =
                repository.findByIdAndUserId(id,user.getId())
                        .orElseThrow(() ->
                                ApiException.notFound("KYC document not found"));


        kyc.setDocumentType(request.documentType());
        kyc.setDocumentNumber(request.documentNumber());
        kyc.setIssueDate(request.issueDate());
        kyc.setExpiryDate(request.expiryDate());

        if (file != null && !file.isEmpty()) {
            FileValidation.requireKycDocument(file);
            String previousKey = kyc.getFileUrl();
            UploadedFile uploaded = storageService.uploadKycDocument(user.getId(), file);
            kyc.setFileUrl(uploaded.key());
            storageService.deleteKycDocument(previousKey);
        }

        // force re-validation
        kyc.setStatus(KycStatus.PENDING);
        kyc.setVerifiedAt(null);


        return map(kyc);
    }



    private User getCurrentUser(){

        String email =
                Objects.requireNonNull(SecurityContextHolder
                                .getContext()
                                .getAuthentication())
                        .getName();


        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow();
    }



    private KycResponse map(UserKyc kyc){

        return new KycResponse(
                kyc.getId(),
                kyc.getDocumentType(),
                kyc.getStatus(),
                // Resolved fresh on every read - never persisted, and expires after
                // app.storage.presigned-url-ttl-minutes.
                storageService.presignKycDocumentUrl(kyc.getFileUrl()),
                kyc.getDocumentNumber(),
                kyc.getIssueDate(),
                kyc.getExpiryDate(),
                kyc.getVerifiedAt(),
                kyc.getRejectedAt(),
                kyc.getRejectionReason(),
                kyc.getVerifiedBy(),
                kyc.getCreatedAt(),
                kyc.getUpdatedAt(),
                kyc.getUser() != null ? kyc.getUser().getEmail() : null
        );
    }

    @Transactional(readOnly = true)
    public List<KycResponse> listPending() {
        permissionService.requireStaff(true, bw.co.gimble.cove.staff.StaffPermission.VERIFIER);
        return repository.findByStatusIn(List.of(KycStatus.PENDING, KycStatus.UNDER_REVIEW))
                .stream().map(this::map).toList();
    }

    public KycResponse verify(UUID id) {
        return review(id, KycStatus.VERIFIED, null);
    }

    public KycResponse reject(UUID id, String reason) {
        return review(id, KycStatus.REJECTED, reason);
    }

    private KycResponse review(UUID id, KycStatus newStatus, String rejectionReason) {
        UUID adminId = requirePlatformAdmin();

        UserKyc kyc = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("KYC document not found"));

        if (kyc.getStatus() == KycStatus.VERIFIED || kyc.getStatus() == KycStatus.REJECTED) {
            throw ApiException.conflict(bw.co.gimble.cove.common.ErrorCode.KYC_ALREADY_DECIDED,
                    "This document has already been reviewed");
        }

        kyc.setStatus(newStatus);
        if (newStatus == KycStatus.VERIFIED) {
            kyc.setVerifiedAt(java.time.LocalDate.now());
            kyc.setVerifiedBy(adminId.toString());
        } else {
            kyc.setRejectedAt(java.time.LocalDate.now());
            kyc.setRejectionReason(rejectionReason);
        }

        return map(kyc);
    }

    private UUID requirePlatformAdmin() {
        return permissionService.requireStaff(false, bw.co.gimble.cove.staff.StaffPermission.VERIFIER);
    }
}