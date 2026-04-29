package com.thanhtam.backend;

import com.thanhtam.backend.entity.Profile;
import com.thanhtam.backend.repository.ProfileRepository;
import com.thanhtam.backend.service.ProfileServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class ProfileServiceImplTest {

    private ProfileRepository profileRepository;
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        profileService = new ProfileServiceImpl(profileRepository);
    }

    // Test Case ID: UT_AM_073
    // Kiểm thử lưu profile thành công
    @Test
    void testCreateProfile_Success() {
        Profile profile = new Profile(1L, "Thanh", "Tam", "avatar.png");
        when(profileRepository.save(profile)).thenReturn(profile);

        Profile result = profileService.createProfile(profile);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Thanh", result.getFirstName());
        assertEquals("Tam", result.getLastName());
        assertEquals("avatar.png", result.getImage());
        verify(profileRepository).save(profile);

        log.info("[UT_AM_073] result={}", result);
    }

    // Test Case ID: UT_AM_074
    // Kiểm thử lưu profile với field null
    @Test
    void testCreateProfile_WithNullFields() {
        Profile profile = new Profile();
        when(profileRepository.save(profile)).thenReturn(profile);

        Profile result = profileService.createProfile(profile);

        assertNotNull(result);
        assertEquals(null, result.getFirstName());
        assertEquals(null, result.getLastName());
        assertEquals(null, result.getImage());
        verify(profileRepository).save(profile);

        log.info("[UT_AM_074] result={}", result);
    }

    // Test Case ID: UT_AM_075
    // Kiểm tra lấy ra hàm profile thành công
    @Test
    void testGetAllProfiles_WithData() {
        List<Profile> profiles = Arrays.asList(
                new Profile(1L, "Thanh", "Tam", "img1.png"),
                new Profile(2L, "Minh", "Anh", "img2.png")
        );
        when(profileRepository.findAll()).thenReturn(profiles);

        List<Profile> result = profileService.getAllProfiles();

        assertEquals(2, result.size());
        assertEquals("Thanh", result.get(0).getFirstName());
        assertEquals("Minh", result.get(1).getFirstName());
        verify(profileRepository).findAll();

        log.info("[UT_AM_075] result={}", result);
    }
}
