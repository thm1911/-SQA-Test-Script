package com.thanhtam.backend;

import com.thanhtam.backend.controller.UserController;
import com.thanhtam.backend.dto.EmailUpdate;
import com.thanhtam.backend.dto.PageResult;
import com.thanhtam.backend.dto.PasswordUpdate;
import com.thanhtam.backend.dto.ServiceResult;
import com.thanhtam.backend.dto.UserUpdate;
import com.thanhtam.backend.entity.Profile;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.entity.Role;
import com.thanhtam.backend.service.ExcelService;
import com.thanhtam.backend.service.FilesStorageService;
import com.thanhtam.backend.service.RoleService;
import com.thanhtam.backend.service.UserService;
import com.thanhtam.backend.ultilities.ERole;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class UserControllerTest {

    private UserService mockUserService;
    private RoleService mockRoleService;
    private PasswordEncoder mockPasswordEncoder;
    private ExcelService mockExcelService;
    private FilesStorageService mockFilesStorageService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        mockUserService = mock(UserService.class);
        mockRoleService = mock(RoleService.class);
        mockPasswordEncoder = mock(PasswordEncoder.class);
        mockExcelService = mock(ExcelService.class);
        mockFilesStorageService = mock(FilesStorageService.class);

        userController = new UserController(
                mockUserService,
                mockRoleService,
                mockPasswordEncoder,
                mockExcelService,
                mockFilesStorageService
        );
    }

    // Test Case ID: UT_AM_009
    // Kiểm tra lấy thông tin người dùng thành công theo username
    @Test
    void testGetUserProfile_Success() {
        String username = "testuser";

        Profile profile = new Profile(10L, "Test", "User", null);
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setProfile(profile);

        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = userController.getUser(username);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);

        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Lấy thông tin user " + username + " thành công!", body.getMessage());
        assertTrue(body.getData() instanceof Optional);

        Optional<User> userOpt = (Optional<User>) body.getData();
        assertTrue(userOpt.isPresent());
        assertEquals(username, userOpt.get().getUsername());

        verify(mockUserService).getUserByUsername(username);

        log.info("[UT_AM_009] response={}", response);
    }

    // Test Case ID: UT_AM_010
    // Kiểm tra lấy thông tin user mà truyền vào username rỗng
    @Test
    void testGetUserProfile_EmptyUsername() {
        //Mock data
        String currentUsername = "current.user";

        User user = new User();
        user.setId(1L);
        user.setUsername(currentUsername);
        user.setEmail("current@example.com");

        when(mockUserService.getUserName()).thenReturn(currentUsername);
        when(mockUserService.getUserByUsername(currentUsername)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = userController.getUser("");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);

        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Lấy thông tin user thành công!", body.getMessage());

        verify(mockUserService).getUserName();
        verify(mockUserService).getUserByUsername(currentUsername);

        log.info("[UT_AM_010] response={}", response);
    }

    // Test Case ID: UT_AM_011
    // Lấy thông tin của user không tồn tại trong hệ thống
    @Test
    void testGetUserProfile_UserNotFound() {
        String username = "not-exists";

        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.getUser(username);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);

        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatusCode());
        assertEquals("Tên đăng nhâp " + username + " không tìm thấy!", body.getMessage());
        assertNull(body.getData());

        verify(mockUserService).getUserByUsername(username);

        log.info("[UT_AM_011] response={}", response);
    }

    // Test Case ID: UT_AM_012
    // Kiểm tra user có tồn tại hay không
    @Test
    void testCheckUsername_Success() {
        String username = "testuser";
        when(mockUserService.existsByUsername(username)).thenReturn(true);

        boolean result = userController.checkUsername(username);

        assertTrue(result);
        verify(mockUserService).existsByUsername(username);

        log.info("[UT_AM_012] result={}", result);
    }

    // Test Case ID: UT_AM_013
    // Kiểm tra email tồn tại
    @Test
    void testCheckEmail_Success() {
        String email = "test@example.com";
        when(mockUserService.existsByEmail(email)).thenReturn(true);

        boolean result = userController.checkEmail(email);

        assertTrue(result);
        verify(mockUserService).existsByEmail(email);

        log.info("[UT_AM_013] result={}", result);
    }

    // Test Case ID: UT_AM_014
    // Kiểm tra update email thành công
    @Test
    void testUpdateEmail_Success() {
        long userId = 1L;

        User existing = new User();
        existing.setId(userId);
        existing.setPassword("hashed");
        existing.setEmail("old@example.com");

        EmailUpdate req = new EmailUpdate("new@example.com", "password");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));
        when(mockPasswordEncoder.matches(req.getPassword(), existing.getPassword())).thenReturn(true);

        ResponseEntity response = userController.updateEmail(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);

        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Update email successfully", body.getMessage());
        assertEquals("new@example.com", body.getData());

        assertEquals("new@example.com", existing.getEmail());
        verify(mockUserService).updateUser(existing);

        log.info("[UT_AM_014] response={}", response);
    }

    // Test Case ID: UT_AM_015
    // Kiểm tra update email khi password nhập vào sai
    @Test
    void testUpdateEmail_WrongPassword() {
        long userId = 1L;

        User existing = new User();
        existing.setId(userId);
        existing.setPassword("hashed");

        EmailUpdate req = new EmailUpdate("new@example.com", "wrong");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));
        when(mockPasswordEncoder.matches(req.getPassword(), existing.getPassword())).thenReturn(false);

        ResponseEntity response = userController.updateEmail(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);

        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.EXPECTATION_FAILED.value(), body.getStatusCode());
        assertEquals("Password is wrong", body.getMessage());
        assertNull(body.getData());

        verify(mockUserService, never()).updateUser(any(User.class));

        log.info("[UT_AM_015] response={}", response);
    }

    // Test Case ID: UT_AM_016
    // Kiểm tra cập nhật email với user không tồn tại
    @Test
    void testUpdateEmail_UserNotFound() {
        long userId = 999L;
        EmailUpdate req = new EmailUpdate("new@example.com", "password");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userController.updateEmail(req, userId));

        log.info("[UT_AM_016] userId={} -> throws NoSuchElementException", userId);
    }

    // Test Case ID: UT_AM_017
    // Cập nhật password thành công
    @Test
    void testUpdatePassword_Success() {
        long userId = 1L;

        User existing = new User();
        existing.setId(userId);
        existing.setPassword("hashed");

        PasswordUpdate req = new PasswordUpdate("hashed", "newpass");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));
        when(mockPasswordEncoder.matches(req.getCurrentPassword(), existing.getPassword())).thenReturn(true);
        when(mockPasswordEncoder.encode(req.getNewPassword())).thenReturn("encoded-newpass");

        ResponseEntity response = userController.updatePass(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);

        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Update password successfully", body.getMessage());

        assertEquals("encoded-newpass", existing.getPassword());
        verify(mockUserService).updateUser(existing);

        log.info("[UT_AM_017] response={}", response);
    }

    // Test Case ID: UT_AM_018
    // Kiểm tra update password với password nhập vào sai
    @Test
    void testUpdatePassword_WrongCurrentPassword() {
        long userId = 1L;

        User existing = new User();
        existing.setId(userId);
        existing.setPassword("hashed");

        PasswordUpdate req = new PasswordUpdate("wrong", "newpass");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));
        when(mockPasswordEncoder.matches(req.getCurrentPassword(), existing.getPassword())).thenReturn(false);

        ResponseEntity response = userController.updatePass(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatusCode());
        assertEquals("Wrong password, please check again!", body.getMessage());
        assertNull(body.getData());

        verify(mockUserService, never()).updateUser(any(User.class));

        log.info("[UT_AM_018] response={}", response);
    }

    // Test Case ID: UT_AM_019
    // Kiểm tra update password giống password cũ
    @Test
    void testUpdatePassword_NewPasswordSameAsCurrent() {
        long userId = 1L;

        User existing = new User();
        existing.setId(userId);
        existing.setPassword("hashed");

        PasswordUpdate req = new PasswordUpdate("hashed", "hashed");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));
        when(mockPasswordEncoder.matches(req.getCurrentPassword(), existing.getPassword())).thenReturn(true);

        ResponseEntity response = userController.updatePass(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatusCode());
        assertEquals("This is old password", body.getMessage());

        verify(mockUserService, never()).updateUser(any(User.class));

        log.info("[UT_AM_019] response={}", response);
    }

    // Test Case ID: UT_AM_020
    // Kiểm tra update password vơi user không tồn tại
    @Test
    void testUpdatePassword_UserNotFound() {
        long userId = 999L;
        PasswordUpdate req = new PasswordUpdate("current", "newpass");

        when(mockUserService.findUserById(userId)).thenReturn(Optional.empty());

        ResponseEntity response = userController.updatePass(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatusCode());
        assertNotNull(body.getMessage());
        assertNull(body.getData());

        log.info("[UT_AM_020] response={}", response);
    }

    // Test Case ID: UT_AM_021
    // Kiểm tra lấy ra user theo pageable
    @Test
    void testGetUsersByPage_Success() {
        Pageable pageable = PageRequest.of(0, 10);

        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("u1");
        u1.setEmail("u1@example.com");

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setEmail("u2@example.com");

        Page<User> page = new PageImpl<>(Arrays.asList(u1, u2), pageable, 2);
        when(mockUserService.findUsersByPage(pageable)).thenReturn(page);

        PageResult result = userController.getUsersByPage(pageable);

        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertNotNull(result.getPaginationDetails());
        verify(mockUserService).findUsersByPage(pageable);

        log.info("[UT_AM_021] pageResult={}", result.getData());
    }

    // Test Case ID: UT_AM_022
    // Kiểm thử chức năng tạo user thành công
    @Test
    void testCreateUser_Success() {
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("new@example.com");

        when(mockUserService.existsByUsername(user.getUsername())).thenReturn(false);
        when(mockUserService.existsByEmail(user.getEmail())).thenReturn(false);

        ResponseEntity<?> response = userController.createUser(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("User created successfully!", body.getMessage());

        verify(mockUserService).createUser(user);

        log.info("[UT_AM_022] response={}", response);
    }

    // Test Case ID: UT_AM_023
    // Kiểm thử chức năng tạo user với username đã tồn tại
    @Test
    void testCreateUser_UsernameExists() {
        User user = new User();
        user.setUsername("exists");
        user.setEmail("new@example.com");

        when(mockUserService.existsByUsername(user.getUsername())).thenReturn(true);

        ResponseEntity<?> response = userController.createUser(user);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatusCode());
        assertEquals("Tên đăng nhập đã có người sử dụng!", body.getMessage());

        verify(mockUserService, never()).createUser(any(User.class));

        log.info("[UT_AM_023] response={}", response);
    }

    // Test Case ID: UT_AM_024
    // Kiểm thử tạo user với email đã tồn tại
    @Test
    void testCreateUser_EmailExists() {
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("exists@example.com");

        when(mockUserService.existsByUsername(user.getUsername())).thenReturn(false);
        when(mockUserService.existsByEmail(user.getEmail())).thenReturn(true);

        ResponseEntity<?> response = userController.createUser(user);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatusCode());
        assertEquals("Email đã có người sử dụng!", body.getMessage());

        verify(mockUserService, never()).createUser(any(User.class));

        log.info("[UT_AM_024] response={}", response);
    }

    // Test Case ID: UT_AM_025
    // Kiểm thử tìm kiếm user theo username hoặc email
    @Test
    void testSearchUsers_Success() {
        String keyword = "abc";
        Pageable pageable = PageRequest.of(0, 10);

        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("abc1");
        u1.setEmail("abc1@example.com");

        Page<User> page = new PageImpl<>(Collections.singletonList(u1), pageable, 1);
        when(mockUserService.findAllByUsernameContainsOrEmailContains(keyword, keyword, pageable)).thenReturn(page);

        PageResult result = userController.searchUsersByUsernameOrEmail(keyword, pageable);

        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(mockUserService).findAllByUsernameContainsOrEmailContains(keyword, keyword, pageable);

        log.info("[UT_AM_025] pageResult{}", result.getData());
    }

    // Test Case ID: UT_AM_026
    // Kiểm tra update email tồn tại với user không tồn tại
    @Test
    void testCheckExistsEmailUpdate_UserNotFound() {
        long userId = 999L;
        String email = "exists@example.com";

        when(mockUserService.existsByEmail(email)).thenReturn(true);
        when(mockUserService.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userController.checkExistsEmailUpdate(email, userId));

        log.info("[UT_AM_026] userId={} -> throws NoSuchElementException", userId);
    }

    // Test Case ID: UT_AM_027
    // Kiểm tra email cập nhật không tồn tại với email đang có của user
    @Test
    void testCheckExistsEmailUpdate_EmailNotExists() {
        long userId = 1L;
        String email = "new@example.com";

        when(mockUserService.existsByEmail(email)).thenReturn(false);

        boolean result = userController.checkExistsEmailUpdate(email, userId);

        assertFalse(result);
        verify(mockUserService).existsByEmail(email);
        verify(mockUserService, never()).findUserById(anyLong());

        log.info("[UT_AM_027] result={}", result);
    }

    // Test Case ID: UT_AM_028
    // Kiểm tra email cập nhật giống với email đang có của người dùng
    @Test
    void testCheckExistsEmailUpdate_SameEmail() {
        long userId = 1L;
        String email = "same@example.com";

        User existing = new User();
        existing.setId(userId);
        existing.setEmail(email);

        when(mockUserService.existsByEmail(email)).thenReturn(true);
        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));

        boolean result = userController.checkExistsEmailUpdate(email, userId);

        assertFalse(result);
        verify(mockUserService).existsByEmail(email);
        verify(mockUserService).findUserById(userId);

        log.info("[UT_AM_028] result={}", result);
    }

    // Test Case ID: UT_AM_029
    // Kiểm tra email cập nhật khác với email đang có của người dùng
    @Test
    void testCheckExistsEmailUpdate_DifferentExistingEmail() {
        long userId = 1L;
        String email = "exists@example.com";

        User existing = new User();
        existing.setId(userId);
        existing.setEmail("current@example.com");

        when(mockUserService.existsByEmail(email)).thenReturn(true);
        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));

        boolean result = userController.checkExistsEmailUpdate(email, userId);

        assertTrue(result);
        verify(mockUserService).existsByEmail(email);
        verify(mockUserService).findUserById(userId);

        log.info("[UT_AM_029] result={}", result);
    }

    // Test Case ID: UT_AM_030
    // Kiểm tra xóa tạm thời user
    @Test
    void testDeleteTempUser_Success() {
        long userId = 1L;
        boolean deleted = true;

        User existing = new User();
        existing.setId(userId);
        existing.setDeleted(false);

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = userController.deleteTempUser(userId, deleted);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(existing.isDeleted());
        verify(mockUserService).updateUser(existing);

        log.info("[UT_AM_030] response={}", response);
    }

    // Test Case ID: UT_AM_031
    // Kiểm tra update user với password không truyền vào
    @Test
    void testUpdateUser_NullPassword() {
        long userId = 1L;

        Profile existingProfile = new Profile(10L, "Old", "Name", null);
        User existing = new User();
        existing.setId(userId);
        existing.setPassword("hashed");
        existing.setEmail("old@example.com");
        existing.setProfile(existingProfile);

        Profile reqProfile = new Profile(null, "New", "Name", null);
        UserUpdate req = new UserUpdate("ignored", "new@example.com", null, reqProfile);

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = userController.updateUser(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Cập nhật thành công!", body.getMessage());

        assertEquals("hashed", existing.getPassword());
        assertEquals("new@example.com", existing.getEmail());
        assertNotNull(existing.getProfile());
        assertEquals(10L, existing.getProfile().getId());
        assertEquals("New", existing.getProfile().getFirstName());
        assertEquals("Name", existing.getProfile().getLastName());

        verify(mockPasswordEncoder, never()).encode(anyString());
        verify(mockUserService).updateUser(existing);

        log.info("[UT_AM_031] response={}", response);
    }

    // Test Case ID: UT_AM_032
    // Cập nhật user với password mới
    @Test
    void testUpdateUser_WithPassword() {
        long userId = 1L;

        Profile existingProfile = new Profile(10L, "Old", "Name", null);
        User existing = new User();
        existing.setId(userId);
        existing.setPassword("old-hashed");
        existing.setEmail("old@example.com");
        existing.setProfile(existingProfile);

        Profile reqProfile = new Profile(null, "New", "Name", null);
        UserUpdate req = new UserUpdate("ignored", "new@example.com", "plain-pass", reqProfile);

        when(mockUserService.findUserById(userId)).thenReturn(Optional.of(existing));
        when(mockPasswordEncoder.encode("plain-pass")).thenReturn("encoded-pass");

        ResponseEntity<?> response = userController.updateUser(req, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Cập nhật thành công!", body.getMessage());

        assertEquals("encoded-pass", existing.getPassword());
        assertEquals("new@example.com", existing.getEmail());
        assertNotNull(existing.getProfile());
        assertEquals(10L, existing.getProfile().getId());
        assertEquals("New", existing.getProfile().getFirstName());
        assertEquals("Name", existing.getProfile().getLastName());

        verify(mockPasswordEncoder).encode("plain-pass");
        verify(mockUserService).updateUser(existing);

        log.info("[UT_AM_032] response={}", response);
    }

    // Test Case ID: UT_AM_033
    // Kiểm tra update user với user không tồn tại
    @Test
    void testUpdateUser_UserNotFound() {
        long userId = 999L;
        UserUpdate req = new UserUpdate();

        when(mockUserService.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userController.updateUser(req, userId));

        log.info("[UT_AM_033] userId={} -> throws NoSuchElementException", userId);
    }

    // Test Case ID: UT_AM_034
    // Kiểm tra xuất file csv user
    @Test
    void testExportUsersToCSV_Success() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter writer = mock(PrintWriter.class);

        when(response.getWriter()).thenReturn(writer);
        when(mockUserService.findAllByDeletedToExport(false)).thenReturn(Collections.emptyList());

        userController.exportUsersToCSV(response);

        verify(response).setContentType("text/csv");
        verify(response).setHeader("Content-Disposition", "attachment; filename=\"users.csv\"");
        verify(response).getWriter();
        verify(mockUserService).findAllByDeletedToExport(false);

        log.info("[UT_AM_034] export invoked successfully");
    }

    // Test Case ID: UT_AM_035
    // Kiểm tra thêm role thành công
    @Test
    void testAddRoles_Success() {
        Role role = new Role();
        role.setName(ERole.ROLE_ADMIN);
        Set<Role> roles = new HashSet<>();

        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(role));

        userController.addRoles(ERole.ROLE_ADMIN, roles);

        assertEquals(1, roles.size());
        assertTrue(roles.contains(role));
        verify(mockRoleService).findByName(ERole.ROLE_ADMIN);

        log.info("[UT_AM_035] rolesSize={}", roles.size());
    }
}

