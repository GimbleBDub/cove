package bw.co.gimble.cove.profile.controllers;

import bw.co.gimble.cove.profile.dto.profile.OnboardingContextResponse;
import bw.co.gimble.cove.common.ApiResponse;
import bw.co.gimble.cove.profile.dto.profile.CreateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UpdateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UserProfileResponse;
import bw.co.gimble.cove.profile.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getMyProfile()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> create(
            @Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(profileService.create(request)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> update(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.update(request)));
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadPhoto(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.uploadPhoto(file)));
    }

    @GetMapping("/onboarding-context")
    public ResponseEntity<ApiResponse<OnboardingContextResponse>> onboardingContext() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.onboardingContext()));
    }

    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<Boolean>> completed() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.profileCompleted()));
    }
}
