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
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public KycResponse create(CreateKycRequest request){

        User user = getCurrentUser();


        UserKyc kyc = UserKyc.builder()
                .user(user)
                .documentType(request.documentType())
                .fileUrl(request.fileUrl())
                .documentNumber(request.documentNumber())
                .issueDate(request.issueDate())
                .expiryDate(request.expiryDate())
                .status(KycStatus.PENDING)
                .build();

        kyc = repository.save(kyc);

        eventPublisher.publishEvent(new KycDocumentSubmittedEvent(kyc.getId(), user.getId(), kyc.getDocumentType()));

        return map(kyc);
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

    public KycResponse update(
            UUID id,
            UpdateKycRequest request){

        User user = getCurrentUser();


        UserKyc kyc =
                repository.findByIdAndUserId(id,user.getId())
                        .orElseThrow(() ->
                                ApiException.notFound("KYC document not found"));


        kyc.setDocumentType(request.documentType());
        kyc.setFileUrl(request.fileUrl());
        kyc.setDocumentNumber(request.documentNumber());
        kyc.setIssueDate(request.issueDate());
        kyc.setExpiryDate(request.expiryDate());

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
                kyc.getFileUrl(),
                kyc.getDocumentNumber(),
                kyc.getIssueDate(),
                kyc.getExpiryDate(),
                kyc.getVerifiedAt(),
                kyc.getRejectedAt(),
                kyc.getRejectionReason(),
                kyc.getVerifiedBy(),
                kyc.getCreatedAt(),
                kyc.getUpdatedAt()
        );
    }
}