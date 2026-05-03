package com.thanhtam.backend.service;

import com.thanhtam.backend.config.JwtUtils;
import com.thanhtam.backend.dto.UserExport;
import com.thanhtam.backend.entity.PasswordResetToken;
import com.thanhtam.backend.entity.Profile;
import com.thanhtam.backend.entity.Role;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.repository.PasswordResetTokenRepository;
import com.thanhtam.backend.repository.UserRepository;
import com.thanhtam.backend.ultilities.Constants;
import com.thanhtam.backend.ultilities.ERole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.mail.MessagingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
class UserServiceImplTest {

    private UserRepository userRepository;
    private RoleService roleService;
    private PasswordEncoder passwordEncoder;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private EmailService emailService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleService = mock(RoleService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        emailService = mock(EmailService.class);

        userService = new UserServiceImpl(
                userRepository,
                roleService,
                passwordEncoder,
                passwordResetTokenRepository,
                emailService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Test Case ID: UT_AM_036
    // Kiểm tra lấy ra user với username thành công
    @Test
    void testGetUserByUsername_Success() {
        User user = new User();
        user.setUsername("testUser_04");
        when(userRepository.findByUsername("testUser_04")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByUsername("testUser_04");

        assertTrue(result.isPresent());
        assertEquals("testUser_04", result.get().getUsername());

        log.info("[UT_AM_036] result={}", result);
    }

    // Test Case ID: UT_AM_037
    // Kiểm tra lấy ra user name của user
    @Test
    void testGetUserName_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("current.user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String result = userService.getUserName();

        assertEquals("current.user", result);

        log.info("[UT_AM_037] username={}", result);
    }

    // Test Case ID: UT_AM_038
    // Kiểm tra user tồn tại với username
    @Test
    void testExistsByUsername() {
        when(userRepository.existsByUsername("testUser_05")).thenReturn(true);

        boolean result = userService.existsByUsername("testUser_05");

        assertTrue(result);

        log.info("[UT_AM_038] result={}", result);
    }

    // Test Case ID: UT_AM_039
    // Kiểm tra user tồn tại theo email
    @Test
    void testExistsByEmail() {
        when(userRepository.existsByEmail("testUser_06@example.com")).thenReturn(true);

        boolean result = userService.existsByEmail("testUser_06@example.com");

        assertTrue(result);

        log.info("[UT_AM_039] result={}", result);
    }

    // Test Case ID: UT_AM_040
    // Kiểm tra Lấy ra User theo pageable
    @Test
    void testFindUsersByPage() {
        Pageable pageable = PageRequest.of(0, 2);

        User u1 = createUser(1L, "user1", "user1@example.com");
        User u2 = createUser(2L, "user2", "user2@example.com");

        Page<User> page = new PageImpl<>(Arrays.asList(u1, u2), pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.findUsersByPage(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());

        //check đúng data
        User res1 = result.getContent().get(0);
        assertEquals(1L, res1.getId());
        assertEquals("user1", res1.getUsername());
        assertEquals("user1@example.com", res1.getEmail());

        User res2 = result.getContent().get(1);
        assertEquals(2L, res2.getId());
        assertEquals("user2", res2.getUsername());
        assertEquals("user2@example.com", res2.getEmail());

        log.info("[UT_AM_040] pageSize={}", result.getContent());
    }

    // Test Case ID: UT_AM_041
    // Kiểm tra tìm kiếm user với trạng thái là đã xóa
    @Test
    void testFindUsersByDeletedStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(Collections.singletonList(createDeletedUser(1L, true)), pageable, 1);
        when(userRepository.findAllByDeleted(true, pageable)).thenReturn(page);

        Page<User> result = userService.findUsersDeletedByPage(pageable, true);

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isDeleted());

        log.info("[UT_AM_041] result={}", result.getContent());
    }

    // Test Case ID: UT_AM_042
    // Kiểm tra tìm kiếm user theo username
    @Test
    void testFindUsersByUsernameSearch_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(Collections.singletonList(createUser(9L, "search-user", "search@example.com")), pageable, 1);
        when(userRepository.findAllByDeletedAndUsernameContains(false, "search", pageable)).thenReturn(page);

        Page<User> result = userService.findAllByDeletedAndUsernameContains(false, "search", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("search-user", result.getContent().get(0).getUsername());

        log.info("[UT_AM_042] result={}", result.getContent());
    }

    // Test Case ID: UT_AM_043
    // Kiểm tra tạo user với dữ liệu hợp lệ
    @Test
    void testCreateUserWithValidData() {
        Profile profile = new Profile(1L, "Test", "User", null);
        User request = new User();
        request.setUsername("testUser_01");
        request.setPassword("password123");
        request.setEmail("testUser_01@example.com");
        request.setProfile(profile);

        Role studentRole = new Role(1L, ERole.ROLE_STUDENT);
        Set<Role> requestRoles = new HashSet<>();
        requestRoles.add(studentRole);
        request.setRoles(requestRoles);

        when(passwordEncoder.encode("testUser_01")).thenReturn("encoded-username");
        when(roleService.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(request);

        assertNotNull(created);
        assertEquals("testUser_01", created.getUsername());
        assertEquals("encoded-username", created.getPassword());
        assertEquals("testUser_01@example.com", created.getEmail());
        assertSame(profile, created.getProfile());
        assertNotNull(created.getRoles());
        assertEquals(1, created.getRoles().size());
        assertTrue(created.getRoles().contains(studentRole));

        log.info("[UT_AM_043] created={}", created);
    }

    // Test Case ID: UT_AM_044
    // Kiểm tra tạo user với role admin sẽ được thêm các role về lecturer, student
    @Test
    void testCreateUserWithAdminRole() {
        Profile profile = new Profile(1L, "Admin", "User", null);
        User request = new User();
        request.setUsername("adminUser");
        request.setEmail("admin@example.com");
        request.setProfile(profile);

        Role adminRole = new Role(1L, ERole.ROLE_ADMIN);
        Role lecturerRole = new Role(2L, ERole.ROLE_LECTURER);
        Role studentRole = new Role(3L, ERole.ROLE_STUDENT);
        request.setRoles(new HashSet<>(Collections.singletonList(adminRole)));

        when(passwordEncoder.encode("adminUser")).thenReturn("encoded-admin");
        when(roleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleService.findByName(ERole.ROLE_LECTURER)).thenReturn(Optional.of(lecturerRole));
        when(roleService.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(request);

        assertEquals(3, created.getRoles().size());
        assertTrue(created.getRoles().contains(adminRole));
        assertTrue(created.getRoles().contains(lecturerRole));
        assertTrue(created.getRoles().contains(studentRole));

        log.info("[UT_AM_044] roles={}", created.getRoles());
    }

    // Test Case ID: UT_AM_045
    // Kiểm tra tạo user với role là lecturer sẽ được thêm role student
    @Test
    void testCreateUserWithLecturerRole() {
        Profile profile = new Profile(1L, "Lecturer", "User", null);
        User request = new User();
        request.setUsername("lecturerUser");
        request.setEmail("lecturer@example.com");
        request.setProfile(profile);

        Role lecturerRole = new Role(2L, ERole.ROLE_LECTURER);
        Role studentRole = new Role(3L, ERole.ROLE_STUDENT);
        request.setRoles(new HashSet<>(Collections.singletonList(lecturerRole)));

        when(passwordEncoder.encode("lecturerUser")).thenReturn("encoded-lecturer");
        when(roleService.findByName(ERole.ROLE_LECTURER)).thenReturn(Optional.of(lecturerRole));
        when(roleService.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(request);

        assertEquals(2, created.getRoles().size());
        assertTrue(created.getRoles().contains(lecturerRole));
        assertTrue(created.getRoles().contains(studentRole));

        log.info("[UT_AM_045] roles={}", created.getRoles());
    }

    // Test Case ID: UT_AM_046
    // Kiểm tra tạo user với role là student
    @Test
    void testCreateUserWithStudentRole() {
        Profile profile = new Profile(1L, "Student", "User", null);
        User request = new User();
        request.setUsername("studentUser");
        request.setEmail("student@example.com");
        request.setProfile(profile);

        Role studentRole = new Role(3L, ERole.ROLE_STUDENT);
        request.setRoles(new HashSet<>(Collections.singletonList(studentRole)));

        when(passwordEncoder.encode("studentUser")).thenReturn("encoded-student");
        when(roleService.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(request);

        assertEquals(1, created.getRoles().size());
        assertTrue(created.getRoles().contains(studentRole));

        log.info("[UT_AM_046] roles={}", created.getRoles());
    }

    // Test Case ID: UT_AM_047
    // Kiểm tra tạo user với role null sẽ được thêm role student mặc định
    @Test
    void testCreateUserWithNullRoleName() {
        Profile profile = new Profile(1L, "Broken", "Role", null);
        User request = new User();
        request.setUsername("nullRoleUser");
        request.setEmail("nullrole@example.com");
        request.setProfile(profile);
        request.setRoles(null);

        Role studentRole = new Role(3L, ERole.ROLE_STUDENT);
        when(passwordEncoder.encode("nullRoleUser")).thenReturn("encoded-null-role");
        when(roleService.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(request);

        assertEquals(1, created.getRoles().size());
        assertTrue(created.getRoles().contains(studentRole));
        verify(roleService).findByName(ERole.ROLE_STUDENT);

        log.info("[UT_AM_047] created with default ROLE_STUDENT={}", created.getRoles());
    }


    // Test Case ID: UT_AM_048
    // Kiểm tra tạo user với role là rỗng
    @Test
    void testCreateUserWithEmptyRoles() {
        Profile profile = new Profile(1L, "Empty", "Roles", null);
        User request = new User();
        request.setUsername("emptyRolesUser");
        request.setEmail("empty@example.com");
        request.setProfile(profile);
        request.setRoles(new HashSet<>());

        when(passwordEncoder.encode("emptyRolesUser")).thenReturn("encoded-empty");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.createUser(request));

        assertNotNull(ex);

        log.info("[UT_AM_048] empty roles -> throws RuntimeException: {}", ex.getMessage());
    }

    // Test Case ID: UT_AM_049
    // Kiểm tra tạo user với role không xác định
    @Test
    void testCreateUserWithUnknownRole() {
        Profile profile = new Profile(1L, "Unknown", "Role", null);
        User request = new User();
        request.setUsername("unknownRoleUser");
        request.setEmail("unknown@example.com");
        request.setProfile(profile);

        Role unknownRole = new Role(99L, ERole.ROLE_STUDENT);
        unknownRole.setName(null); // Role không xác định
        request.setRoles(new HashSet<>(Collections.singletonList(unknownRole)));

        when(passwordEncoder.encode("unknownRoleUser")).thenReturn("encoded-unknown");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.createUser(request));

        assertNotNull(ex);

        log.info("[UT_AM_049] unknown role (null name) -> throws RuntimeException: {}", ex.getMessage());
    }

    // Test Case ID: UT_AM_050
    // Kiểm thử tìm kiếm user theo id
    @Test
    void testFindUserById_Success() {
        User user = createUser(10L, "find-by-id", "find@example.com");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findUserById(10L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());

        log.info("[UT_AM_050] result={}", result);
    }

    // Test Case ID: UT_AM_051
    // Tìm kiếm user không bị xóa để xuất file thành công
    @Test
    void testFindAllByNotDeletedToExport_Success() {
        User user = createUser(14L, "export-user", "export@example.com");
        user.setProfile(new Profile(2L, "Export", "User", null));
        when(userRepository.findAllByDeleted(false)).thenReturn(Collections.singletonList(user));

        List<UserExport> result = userService.findAllByDeletedToExport(false);

        assertEquals(1, result.size());
        assertEquals("export-user", result.get(0).getUsername());
        assertEquals("export@example.com", result.get(0).getEmail());
        assertEquals("Export", result.get(0).getFirstName());
        assertEquals("User", result.get(0).getLastName());

        log.info("[UT_AM_051] exportList={}", result);
    }

    // Test Case ID: UT_AM_052
    // Kiểm tra update user thành công
    @Test
    void testUpdateUser_Success() {
        User user = createUser(8L, "testUser_08", "testUser_08@example.com");
        user.setProfile(new Profile(1L, "Updated", "Name", null));

        userService.updateUser(user);

        log.info("[UT_AM_052] updatedUser={}", user);
    }

    // Test Case ID: UT_AM_053
    // Tìm kiếm tất cả user bới intakeId
    @Test
    void testFindAllByIntakeId_Success() {
        List<User> users = Arrays.asList(
                createUser(11L, "intake-user-1", "user1@example.com"),
                createUser(12L, "intake-user-2", "user2@example.com")
        );

        when(userRepository.findAllByIntakeId(11L)).thenReturn(users);

        List<User> result = userService.findAllByIntakeId(11L);

        assertNotNull(result);
        assertEquals(2, result.size());

        User u1 = result.get(0);
        assertEquals(11L, u1.getId());
        assertEquals("intake-user-1", u1.getUsername());
        assertEquals("user1@example.com", u1.getEmail());

        User u2 = result.get(1);
        assertEquals(12L, u2.getId());
        assertEquals("intake-user-2", u2.getUsername());
        assertEquals("user2@example.com", u2.getEmail());

        log.info("[UT_AM_053] result={}", result);
    }

    // Test Case ID: UT_AM_054
    // Kiểm tra yêu câu reset password thành công
    @Test
    void testRequestPasswordReset_Success() throws MessagingException {
        User user = createUser(15L, "reset-user", "reset@example.com");
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));

        boolean result = userService.requestPasswordReset("reset@example.com");

        assertTrue(result);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        verify(emailService).resetPassword(eq("reset@example.com"), anyString());

        PasswordResetToken savedToken = captor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getToken());
        assertFalse(savedToken.getToken().isEmpty());

        log.info("[UT_AM_054] savedToken={}", savedToken.getToken());
    }

    // Test Case ID: UT_AM_055
    // Kiểm tra reset password với user không tồn tại
    @Test
    void testRequestPasswordReset_UserNotFound() throws MessagingException {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        boolean result = userService.requestPasswordReset("missing@example.com");

        assertFalse(result);

        log.info("[UT_AM_055] result={}", result);
    }

    // Test Case ID: UT_AM_056
    // Kiểm tra reset password thành công
    @Test
    void testResetPassword_Success() {
        String token = new JwtUtils().generatePasswordResetToken(16L);
        User user = createUser(16L, "reset-password-user", "reset-password@example.com");
        PasswordResetToken passwordResetToken = new PasswordResetToken(1L, token, user);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(passwordResetToken);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userRepository.save(user)).thenReturn(userWithPassword(user, "encoded-new-password"));

        boolean result = userService.resetPassword(token, "new-password");

        assertTrue(result);
        assertEquals("encoded-new-password", user.getPassword());

        log.info("[UT_AM_056] result={}", result);
    }

    // Test Case ID: UT_AM_057
    // Kiểm tra reset pasword với token hết hạn
    @Test
    void testResetPassword_ExpiredToken() {
        String expiredToken = Jwts.builder()
                .setSubject("16")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(SignatureAlgorithm.HS512, Constants.SIGNING_KEY)
                .compact();

        boolean result = userService.resetPassword(expiredToken, "new-password");

        assertFalse(result);

        log.info("[UT_AM_057] result={}", result);
    }

    // Test Case ID: UT_AM_058
    // Kiểm tra reset password khi không có token
    @Test
    void testResetPassword_TokenNotFound() {
        String token = new JwtUtils().generatePasswordResetToken(17L);
        when(passwordResetTokenRepository.findByToken(token)).thenReturn(null);

        boolean result = userService.resetPassword(token, "new-password");

        assertFalse(result);

        log.info("[UT_AM_058] result={}", result);
    }

    // Test Case ID: UT_AM_059
    // Kiểm tra reset password với user vừa lưu trả về null
    @Test
    void testResetPassword_SaveReturnsNull() {
        String token = new JwtUtils().generatePasswordResetToken(18L);
        User user = createUser(18L, "null-save-user", "null-save@example.com");
        PasswordResetToken passwordResetToken = new PasswordResetToken(1L, token, user);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(passwordResetToken);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userRepository.save(user)).thenReturn(null);

        boolean result = userService.resetPassword(token, "new-password");

        assertFalse(result);

        log.info("[UT_AM_059] result={}", result);
    }

    // Test Case ID: UT_AM_060
    // Kiểm tra reset password với user sau khi lưu có password khác với password được encoded
    @Test
    void testResetPassword_SaveReturnsUserWithDifferentPassword() {
        String token = new JwtUtils().generatePasswordResetToken(19L);
        User user = createUser(19L, "different-password-user", "different@example.com");
        PasswordResetToken passwordResetToken = new PasswordResetToken(1L, token, user);

        when(passwordResetTokenRepository.findByToken(token)).thenReturn(passwordResetToken);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userRepository.save(user)).thenReturn(userWithPassword(new User(), "another-password"));

        boolean result = userService.resetPassword(token, "new-password");

        assertFalse(result);

        log.info("[UT_AM_060] result={}", result);
    }

    // Test Case ID: UT_AM_061
    // Kiểm tra tìm kiếm user theo username hoặc email
    @Test
    void testFindAllByUsernameContainsOrEmailContains_Success() {
        Pageable pageable = PageRequest.of(0, 10);

        User user = createUser(13L, "keyword-user", "keyword@example.com");

        Page<User> page = new PageImpl<>(
                Collections.singletonList(user), pageable, 1
        );

        when(userRepository.findAllByUsernameContainsOrEmailContains("keyword", "keyword", pageable))
                .thenReturn(page);

        Page<User> result = userService
                .findAllByUsernameContainsOrEmailContains("keyword", "keyword", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        User res = result.getContent().get(0);
        assertEquals(13L, res.getId());
        assertEquals("keyword-user", res.getUsername());
        assertEquals("keyword@example.com", res.getEmail());

        assertTrue(res.getUsername().contains("keyword")
                || res.getEmail().contains("keyword"));

        log.info("[UT_AM_061] result={}", result.getContent());
    }

    // Test Case ID: UT_AM_062
    // Kiểm tra thêm role thành công
    @Test
    void testAddRoles_Success() {
        Role lecturerRole = new Role(2L, ERole.ROLE_LECTURER);
        Set<Role> roles = new HashSet<>();
        when(roleService.findByName(ERole.ROLE_LECTURER)).thenReturn(Optional.of(lecturerRole));

        userService.addRoles(ERole.ROLE_LECTURER, roles);

        assertEquals(1, roles.size());
        assertTrue(roles.contains(lecturerRole));

        log.info("[UT_AM_062] roles={}", roles);
    }

    // Test Case ID: UT_AM_063
    // Kiểm tra không tìm thấy role
    @Test
    void testAddRoles_RoleNotFound() {
        Set<Role> roles = new HashSet<>();
        when(roleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.addRoles(ERole.ROLE_ADMIN, roles));

        assertEquals("Error: Role is not found", ex.getMessage());
        assertTrue(roles.isEmpty());

        log.info("[UT_AM_063] role={} -> throws RuntimeException", ERole.ROLE_ADMIN);
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setProfile(new Profile(id, "First" + id, "Last" + id, null));
        return user;
    }

    private User createDeletedUser(Long id, boolean deleted) {
        User user = createUser(id, "deleted-user-" + id, "deleted" + id + "@example.com");
        user.setDeleted(deleted);
        return user;
    }

    private User userWithPassword(User user, String password) {
        user.setPassword(password);
        return user;
    }

}
