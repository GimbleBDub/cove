package bw.co.gimble.cove.profile.services;


import bw.co.gimble.cove.common.ApiException;
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
        profile.setProfilePhoto(request.profilePhoto());
        profile.setBio(request.bio());
        profile.setAddressLine1(request.addressLine1());
        profile.setAddressLine2(request.addressLine2());
        profile.setCity(request.city());
        profile.setDistrict(request.district());
        profile.setCountry(request.country());
        profile.setPostalCode(request.postalCode());


        return map(profile);
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