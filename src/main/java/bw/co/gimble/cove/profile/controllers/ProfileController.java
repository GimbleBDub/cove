package bw.co.gimble.cove.profile.controllers;


import bw.co.gimble.cove.profile.dto.profile.CreateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UpdateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UserProfileResponse;
import bw.co.gimble.cove.profile.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public UserProfileResponse getProfile() {
        return profileService.getMyProfile();
    }

    @PostMapping
    public UserProfileResponse create(
            @Valid @RequestBody CreateProfileRequest request) {
        return profileService.create(request);
    }

    @PutMapping
    public UserProfileResponse update(
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(request);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadPhoto(@RequestPart("file") MultipartFile file) {
        return profileService.uploadPhoto(file);
    }

    @GetMapping("/completed")
    public boolean completed() {
        return profileService.profileCompleted();
    }
}