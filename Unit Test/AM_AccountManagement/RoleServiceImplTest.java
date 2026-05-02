package com.thanhtam.backend;

import com.thanhtam.backend.entity.Role;
import com.thanhtam.backend.repository.RoleRepository;
import com.thanhtam.backend.service.RoleServiceImpl;
import com.thanhtam.backend.ultilities.ERole;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class RoleServiceImplTest {

    private RoleRepository roleRepository;
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        roleService = new RoleServiceImpl(roleRepository);
    }

    // Test Case ID: UT_AM_068
    // Kiểm trhuwr tìm kiếm role theo tên role admin
    @Test
    void testFindByNameWithAdminRole() {
        Role adminRole = new Role(1L, ERole.ROLE_ADMIN);
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

        Optional<Role> result = roleService.findByName(ERole.ROLE_ADMIN);

        assertTrue(result.isPresent());
        assertEquals(ERole.ROLE_ADMIN, result.get().getName());
        verify(roleRepository).findByName(ERole.ROLE_ADMIN);

        log.info("[UT_AM_068] result={}", result);
    }

    // Test Case ID: UT_AM_069
    // Kiểm thử tìm kiếm role theo role lecturer
    @Test
    void testFindByNameWithLecturerRole() {
        Role lecturerRole = new Role(2L, ERole.ROLE_LECTURER);
        when(roleRepository.findByName(ERole.ROLE_LECTURER)).thenReturn(Optional.of(lecturerRole));

        Optional<Role> result = roleService.findByName(ERole.ROLE_LECTURER);

        assertTrue(result.isPresent());
        assertEquals(ERole.ROLE_LECTURER, result.get().getName());
        verify(roleRepository).findByName(ERole.ROLE_LECTURER);

        log.info("[UT_AM_069] result={}", result);
    }

    // Test Case ID: UT_AM_070
    // Kiểm thử tìm kiếm role theo role student
    @Test
    void testFindByNameWithStudentRole() {
        Role studentRole = new Role(3L, ERole.ROLE_STUDENT);
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));

        Optional<Role> result = roleService.findByName(ERole.ROLE_STUDENT);

        assertTrue(result.isPresent());
        assertEquals(ERole.ROLE_STUDENT, result.get().getName());
        verify(roleRepository).findByName(ERole.ROLE_STUDENT);

        log.info("[UT_AM_070] result={}", result);
    }

    // Test Case ID: UT_AM_071
    // Kiểm thử tìm kiếm role với role truyền vào không có trong hệ thống
    @Test
    void testFindByNameWhenRoleDoesNotExist() {
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());

        Optional<Role> result = roleService.findByName(ERole.ROLE_ADMIN);

        assertFalse(result.isPresent());
        verify(roleRepository).findByName(ERole.ROLE_ADMIN);

        log.info("[UT_AM_071] result={}", result);
    }

    // Test Case ID: UT_AM_072
    // Kiểm thử tìm kiếm role vói role truyền vào bị null
    @Test
    void testFindByNameWithNullRole() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> roleService.findByName(null)
        );

        assertEquals("Role name cannot be null", ex.getMessage());

        verify(roleRepository, never()).findByName(any());

        log.info("[UT_AM_072] exception={}", ex.getMessage());
    }
}
