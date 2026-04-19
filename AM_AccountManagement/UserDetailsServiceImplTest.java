package com.thanhtam.backend;

import com.thanhtam.backend.entity.Profile;
import com.thanhtam.backend.entity.Role;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.repository.UserRepository;
import com.thanhtam.backend.service.UserDetailsServiceImpl;
import com.thanhtam.backend.ultilities.ERole;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new UserDetailsServiceImpl();
        ReflectionTestUtils.setField(userDetailsService, "userRepository", userRepository);
    }

    // Test Case ID: UT_AM_066
    // Tìm kiếm user tồn tại trong hệ thống
    @Test
    void testLoadUserByUsernameWithExistingUser() {
        User user = createUser(1L, "existingUser", "existing@example.com", "secret", roleSet(ERole.ROLE_STUDENT));
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("existingUser");

        assertEquals("existingUser", result.getUsername());
        assertEquals("secret", result.getPassword());
        assertEquals(1, result.getAuthorities().size());
        assertTrue(hasAuthority(result.getAuthorities(), "ROLE_STUDENT"));
        verify(userRepository).findByUsername("existingUser");

        log.info("[UT_AM_066] result username={}, authorities={}", result.getUsername(), result.getAuthorities());
    }

    // Test Case ID: UT_AM_067
    // Kiểm tra tìm kiếm user theo username không tồn tại
    @Test
    void testLoadUserByUsernameWithNonExistentUser() {
        when(userRepository.findByUsername("missingUser")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missingUser"));

        assertEquals("User Not Found with username: missingUser", ex.getMessage());
        verify(userRepository).findByUsername("missingUser");

        log.info("[UT_AM_067] exception={}", ex.getMessage());
    }

    // Test Case ID: UT_AM_068
    // Kiểm tra tìm kiếm user với username truyền vào null
    @Test
    void testLoadUserByUsernameWithNullUsername() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null));

        assertEquals("User Not Found with username: null", ex.getMessage());
        verify(userRepository).findByUsername(null);

        log.info("[UT_AM_068] exception={}", ex.getMessage());
    }

    // Test Case ID: UT_AM_069
    // Kiểm tra tìm kiếm user với username rỗng
    @Test
    void testLoadUserByUsernameWithEmptyUsername() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(""));

        assertEquals("User Not Found with username: ", ex.getMessage());
        verify(userRepository).findByUsername("");

        log.info("[UT_AM_069] exception={}", ex.getMessage());
    }

    private User createUser(Long id, String username, String email, String password, Set<Role> roles) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(roles);
        user.setProfile(new Profile(id, "First" + id, "Last" + id, null));
        return user;
    }

    private Set<Role> roleSet(ERole... roleNames) {
        return Arrays.stream(roleNames)
                .map(roleName -> new Role(null, roleName))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean hasAuthority(Collection<? extends GrantedAuthority> authorities, String authority) {
        return authorities.stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
