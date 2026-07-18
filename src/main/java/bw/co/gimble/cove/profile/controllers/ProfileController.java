package bw.co.gimble.cove.profile.controllers;


import bw.co.gimble.cove.profile.dto.profile.CreateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UpdateProfileRequest;
import bw.co.gimble.cove.profile.dto.profile.UserProfileResponse;
import bw.co.gimble.cove.profile.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/completed")
    public boolean completed() {
        return profileService.profileCompleted();
    }
}
