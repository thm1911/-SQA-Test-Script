package com.thanhtam.backend;

import com.thanhtam.backend.controller.ExcelController;
import com.thanhtam.backend.dto.UserExcel;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.service.ExcelService;
import com.thanhtam.backend.service.FilesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class ExcelControllerTest {

    private FilesStorageService mockFilesStorageService;
    private ExcelService mockExcelService;

    private ExcelController excelController;

    @BeforeEach
    void setUp() {
        mockFilesStorageService = mock(FilesStorageService.class);
        mockExcelService = mock(ExcelService.class);
        excelController = new ExcelController(mockFilesStorageService, mockExcelService);
    }


    // Test Case ID: UT_AM_078
    // Kiểm thử thêm user vào DB thông qua file excel
    @Test
    void testUploadUserToDB_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake-content".getBytes()
        );

        User user1 = new User();
        user1.setUsername("user1");
        User user2 = new User();
        user2.setUsername("user2");
        List<User> users = Arrays.asList(user1, user2);

        when(mockExcelService.readUserFromExcelFile("excel-import-user\\users.xlsx")).thenReturn(users);

        ResponseEntity<UserExcel> response = excelController.uploadUserToDB(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK.value(), response.getBody().getStatusCode());
        assertEquals("Uploaded the user list successfully: users.xlsx", response.getBody().getMessage());
        assertEquals(users, response.getBody().getData());
        assertEquals(2, response.getBody().getUserTotal());

        InOrder inOrder = inOrder(mockFilesStorageService, mockExcelService);
        inOrder.verify(mockFilesStorageService).save(file, "excel-import-user");
        inOrder.verify(mockExcelService).readUserFromExcelFile("excel-import-user\\users.xlsx");
        inOrder.verify(mockExcelService).InsertUserToDB(users);
        verify(mockFilesStorageService).deleteAllUserExcel("users.xlsx");

        log.info("[UT_AM_078] response={}", response);
    }

    // Test Case ID: UT_AM_079
    // Kiểm thử thêm user vào DB thông qua file excel
    @Test
    void testUploadUserToDB_FailureStillDeletesTempFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake-content".getBytes()
        );

        doThrow(new RuntimeException("read error"))
                .when(mockExcelService)
                .readUserFromExcelFile("excel-import-user\\invalid.xlsx");

        ResponseEntity<UserExcel> response = excelController.uploadUserToDB(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.EXPECTATION_FAILED.value(), response.getBody().getStatusCode());
        assertEquals("Could not upload the user list: invalid.xlsx!", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertEquals(0, response.getBody().getUserTotal());

        verify(mockFilesStorageService).save(file, "excel-import-user");
        verify(mockExcelService).readUserFromExcelFile("excel-import-user\\invalid.xlsx");
        verify(mockExcelService, never()).InsertUserToDB(anyList());
        verify(mockFilesStorageService).deleteAllUserExcel("invalid.xlsx");

        log.info("[UT_AM_079] response={}", response);
    }
}
