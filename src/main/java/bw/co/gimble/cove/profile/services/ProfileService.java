package bw.co.gimble.cove.profile.services;


import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.profile.dto.profile.OnboardingContextResponse;
import bw.co.gimble.cove.profile.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;
import bw.co.gimble.cove.profile.dto.profile.CreateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UpdateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UserProfileResponse;
import bw.co.gimble.cove.profile.models.UserProfile;
import bw.co.gimble.cove.profile.repositories.UserProfileRepository;
import bw.co.gimble.cove.storage.StorageService;
import bw.co.gimble.cove.storage.dto.FileValidation;
import bw.co.gimble.cove.storage.dto.UploadedFile;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final PermissionService permissionService;
    private final bw.co.gimble.cove.role.UserRoleRepository userRoleRepository;
    private final bw.co.gimble.cove.organisations.OrganisationRepository organisationRepository;


    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {

        User user = getCurrentUser();

        UserProfile profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        ApiException.notFound("Profile not found"));

        return map(profile);
    }


    public UserProfileResponse create(CreateProfileRequest request) {

        User user = getCurrentUser();

        if(userProfileRepository.existsByUserId(user.getId())) {
            throw ApiException.conflict(bw.co.gimble.cove.common.ErrorCode.PROFILE_ALREADY_EXISTS, "Profile already exists");
        }
        requirePersonalDetails(user, request.gender(), request.dateOfBirth());


        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(request.firstName())
                .middleName(request.middleName())
                .lastName(request.lastName())
                .preferredName(request.preferredName())
                .gender(request.gender())
                .dateOfBirth(request.dateOfBirth())
                .nationalId(request.nationalId())
                .passportNumber(request.passportNumber())
                .nationality(request.nationality())
                .occupation(request.occupation())
                .profilePhoto(request.profilePhoto())
                .bio(request.bio())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .district(request.district())
                .country(request.country())
                .postalCode(request.postalCode())
                .build();


        return map(userProfileRepository.save(profile));
    }


    /** Requires a profile to already exist - create one first via POST /api/profile. */
    public UserProfileResponse uploadPhoto(MultipartFile file) {

        User user = getCurrentUser();

        UserProfile profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        ApiException.notFound("Create your profile before uploading a photo"));

        FileValidation.requireImage(file);
        UploadedFile uploaded = storageService.uploadProfilePhoto(user.getId(), file);
        profile.setProfilePhoto(uploaded.publicUrl());

        return map(profile);
    }


    public UserProfileResponse update(UpdateProfileRequest request) {

        User user = getCurrentUser();

        UserProfile profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        ApiException.notFound("Profile not found"));
        requirePersonalDetails(user, request.gender(), request.dateOfBirth());


        profile.setFirstName(request.firstName());
        profile.setMiddleName(request.middleName());
        profile.setLastName(request.lastName());
        profile.setPreferredName(request.preferredName());
        profile.setGender(request.gender());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setNationalId(request.nationalId());
        profile.setPassportNumber(request.passportNumber());
        profile.setNationality(request.nationality());
        profile.setOccupation(request.occupation());
        // The photo is normally set via POST /photo; don't wipe it when a form omits it.
        if (request.profilePhoto() != null) profile.setProfilePhoto(request.profilePhoto());
        profile.setBio(request.bio());
        profile.setAddressLine1(request.addressLine1());
        profile.setAddressLine2(request.addressLine2());
        profile.setCity(request.city());
        profile.setDistrict(request.district());
        profile.setCountry(request.country());
        profile.setPostalCode(request.postalCode());


        return map(profile);
    }


    /** What the client needs before the profile exists: whether it does, and whether onboarding is admin-lite. */
    @Transactional(readOnly = true)
    public OnboardingContextResponse onboardingContext() {
        User user = getCurrentUser();
        var profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        var roles = userRoleRepository.findByUserIdAndStatus(user.getId(), bw.co.gimble.cove.role.RoleStatus.ACTIVE);
        var byOrg = new java.util.LinkedHashMap<UUID, java.util.List<String>>();
        roles.stream().filter(r -> r.getOrganisationId() != null)
                .forEach(r -> byOrg.computeIfAbsent(r.getOrganisationId(), k -> new java.util.ArrayList<>()).add(r.getRole().name()));
        var organisations = byOrg.entrySet().stream()
                .map(e -> organisationRepository.findById(e.getKey())
                        .map(o -> new OnboardingContextResponse.OrganisationMembership(
                                o.getId(), o.getName(), o.getVerificationStatus().name(), e.getValue(),
                                e.getValue().contains("AGENCY_ADMIN")))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new OnboardingContextResponse(
                profile != null,
                permissionService.isPlatformAdmin(user.getId()),
                user.getEmail(),
                permissionService.staffPermissions(user.getId()).stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                user.isPasswordChangeRequired(),
                profile != null ? profile.getFullName() : null,
                organisations);
    }

    /** Platform admins only need names; everyone else must also give gender and date of birth. */
    private void requirePersonalDetails(User user, Gender gender, LocalDate dateOfBirth) {
        if (permissionService.isPlatformAdmin(user.getId())) return;
        if (gender == null || dateOfBirth == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Gender and date of birth are required");
        }
        if (dateOfBirth.isAfter(LocalDate.now().minusYears(16))) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "You must be at least 16 years old");
        }
    }

    @Transactional(readOnly = true)
    public boolean profileCompleted(){

        User user = getCurrentUser();

        return userProfileRepository.existsByUserId(user.getId());
    }



    private User getCurrentUser(){

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        assert authentication != null;
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        ApiException.notFound("User not found"));
    }



    private UserProfileResponse map(UserProfile profile){

        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getFirstName(),
                profile.getMiddleName(),
                profile.getLastName(),
                profile.getPreferredName(),
                profile.getGender(),
                profile.getDateOfBirth(),
                profile.getNationalId(),
                profile.getPassportNumber(),
                profile.getNationality(),
                profile.getOccupation(),
                profile.getProfilePhoto(),
                profile.getBio(),
                profile.getAddressLine1(),
                profile.getAddressLine2(),
                profile.getCity(),
                profile.getDistrict(),
                profile.getCountry(),
                profile.getPostalCode(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}