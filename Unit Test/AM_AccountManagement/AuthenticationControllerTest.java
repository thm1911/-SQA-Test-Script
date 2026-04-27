package com.thanhtam.backend.controller;

import com.thanhtam.backend.config.JwtUtils;
import com.thanhtam.backend.dto.LoginUser;
import com.thanhtam.backend.dto.OperationStatusDto;
import com.thanhtam.backend.dto.PasswordResetDto;
import com.thanhtam.backend.dto.PasswordResetRequest;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.payload.response.JwtResponse;
import com.thanhtam.backend.service.UserDetailsImpl;
import com.thanhtam.backend.service.UserService;
import com.thanhtam.backend.ultilities.RequestOperationName;
import com.thanhtam.backend.ultilities.RequestOperationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationControllerTest.class);

    private JwtUtils jwtUtils;
    private AuthenticationManager authenticationManager;
    private UserService userService;
    private AuthenticationController controller;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        authenticationManager = mock(AuthenticationManager.class);
        userService = mock(UserService.class);

        controller = new AuthenticationController(jwtUtils, authenticationManager, userService);
    }

    // Test Case ID: UT_AM_001
    // Returns 200 OK; JwtResponse with JWT token and user details; userService.updateUser called once
    @Test
    void testAuthenticateUserSuccess() {
        String username = "testuser";
        String password = "123";
        String jwt = "jwt-token";

        LoginUser login = new LoginUser();
        login.setUsername(username);
        login.setPassword(password);

        User user = new User();
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setDeleted(false);

        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        UserDetailsImpl principal = new UserDetailsImpl(
                1L,
                username,
                "test@example.com",
                password,
                Collections.<SimpleGrantedAuthority>singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn(jwt);

        ResponseEntity<?> response = controller.authenticateUser(login);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof JwtResponse);

        JwtResponse body = (JwtResponse) response.getBody();
        assertNotNull(body.getAccessToken());
        assertEquals(1L, body.getId());
        assertEquals(username, body.getUsername());
        assertEquals("test@example.com", body.getEmail());
        assertEquals(Collections.singletonList("ROLE_STUDENT"), body.getRoles());

        verify(userService, times(2)).getUserByUsername(username);
        verify(userService).updateUser(any(User.class));
        verify(authenticationManager).authenticate(any());
        verify(jwtUtils).generateJwtToken(authentication);

        logger.info("[UT_AUTH_001] HTTP Status: {}", response.getStatusCodeValue());
        logger.info("[UT_AUTH_001] JwtResponse: token={}, id={}, username={}, email={}, roles={}",
                body.getAccessToken(),
                body.getId(),
                body.getUsername(),
                body.getEmail(),
                body.getRoles()
        );
    }

    // Test Case ID: UT_AM_002
    // Returns 400 Bad Request
    @Test
    void testAuthenticateUserNotFound() {
        String username = "notfound";

        LoginUser login = new LoginUser();
        login.setUsername(username);
        login.setPassword("123");

        when(userService.getUserByUsername(username)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.authenticateUser(login);

        assertEquals(400, response.getStatusCodeValue());

        verify(userService).getUserByUsername(username);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtUtils);

        logger.info("[UT_AM_002] HTTP Status: {}", response.getStatusCodeValue());
    }

    // Test Case ID: UT_AM_003
    // Returns 400 Bad Request
    @Test
    void testAuthenticateUserDeleted() {
        String username = "deleted";

        LoginUser login = new LoginUser();
        login.setUsername(username);
        login.setPassword("123");

        User user = new User();
        user.setUsername(username);
        user.setDeleted(true);

        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.authenticateUser(login);

        assertEquals(400, response.getStatusCodeValue());

        verify(userService).getUserByUsername(username);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtUtils);

        logger.info("[UT_AM_003] HTTP Status: {}", response.getStatusCodeValue());
    }

    // Test Case ID: UT_AM_004
    // Returns 400 Bad Request when user becomes deleted in the post-authentication lookup
    @Test
    void testAuthenticateUserDeletedAfterAuthentication() {
        String username = "testuser";
        String password = "123";
        String jwt = "jwt-token";

        LoginUser login = new LoginUser();
        login.setUsername(username);
        login.setPassword(password);

        User activeUser = new User();
        activeUser.setUsername(username);
        activeUser.setEmail("test@example.com");
        activeUser.setDeleted(false);

        User deletedUserLog = new User();
        deletedUserLog.setUsername(username);
        deletedUserLog.setEmail("test@example.com");
        deletedUserLog.setDeleted(true);

        when(userService.getUserByUsername(username))
                .thenReturn(Optional.of(activeUser))
                .thenReturn(Optional.of(deletedUserLog));

        UserDetailsImpl principal = new UserDetailsImpl(
                1L,
                username,
                "test@example.com",
                password,
                Collections.<SimpleGrantedAuthority>singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn(jwt);

        ResponseEntity<?> response = controller.authenticateUser(login);

        assertEquals(400, response.getStatusCodeValue());
        assertNull(response.getBody());

        verify(userService, times(2)).getUserByUsername(username);
        verify(userService).updateUser(deletedUserLog);
        verify(authenticationManager).authenticate(any());
        verify(jwtUtils).generateJwtToken(authentication);

        logger.info("[UT_AM_008] HTTP Status: {}", response.getStatusCodeValue());
    }

    // Test Case ID: UT_AM_005
    // Returns OperationStatusDto with operationName=REQUEST_PASSWORD_RESET, operationResult=SUCCESS; service called once
    @Test
    void testPasswordResetRequestSuccess() throws Exception {
        String email = "test@gmail.com";

        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail(email);

        when(userService.requestPasswordReset(email)).thenReturn(true);

        OperationStatusDto response = controller.resetPasswordRequest(request);

        assertEquals(RequestOperationName.REQUEST_PASSWORD_RESET.name(), response.getOperationName());
        assertEquals(RequestOperationStatus.SUCCESS.name(), response.getOperationResult());

        verify(userService).requestPasswordReset(email);

        logger.info("[UT_AM_004] response={}", response);
    }

    // Test Case ID: UT_AM_006
    // Returns OperationStatusDto with operationName=PASSWORD_RESET, operationResult=SUCCESS; service called once
    @Test
    void testPasswordResetSuccess() {
        String token = "valid-token";
        String password = "newpass";

        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken(token);
        dto.setPassword(password);

        when(userService.resetPassword(token, password)).thenReturn(true);

        OperationStatusDto response = controller.resetPassword(dto);

        assertEquals(RequestOperationName.PASSWORD_RESET.name(), response.getOperationName());
        assertEquals(RequestOperationStatus.SUCCESS.name(), response.getOperationResult());

        verify(userService).resetPassword(token, password);

        logger.info("[UT_AM_005] response={}", response);
    }

    // Test Case ID: UT_AM_007
    // Returns OperationStatusDto with operationName=REQUEST_PASSWORD_RESET, operationResult=ERROR; service called once
    @Test
    void testPasswordResetRequestFail() throws Exception {
        String email = "unknown@gmail.com";

        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail(email);

        when(userService.requestPasswordReset(email)).thenReturn(false);

        OperationStatusDto response = controller.resetPasswordRequest(request);

        assertEquals(RequestOperationName.REQUEST_PASSWORD_RESET.name(), response.getOperationName());
        assertEquals(RequestOperationStatus.ERROR.name(), response.getOperationResult());

        verify(userService).requestPasswordReset(email);

        logger.info("[UT_AM_006] response={}", response);
    }

    // Test Case ID: UT_AM_008
    // Returns OperationStatusDto with operationName=PASSWORD_RESET, operationResult=ERROR; service called once
    @Test
    void testPasswordResetFail() {
        String token = "invalid";
        String password = "123";

        PasswordResetDto dto = new PasswordResetDto();
        dto.setToken(token);
        dto.setPassword(password);

        when(userService.resetPassword(token, password)).thenReturn(false);

        OperationStatusDto response = controller.resetPassword(dto);

        assertEquals(RequestOperationName.PASSWORD_RESET.name(), response.getOperationName());
        assertEquals(RequestOperationStatus.ERROR.name(), response.getOperationResult());

        verify(userService).resetPassword(token, password);

        logger.info("[UT_AM_007] response={}", response);
    }
}
