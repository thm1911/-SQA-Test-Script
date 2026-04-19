package com.thanhtam.backend.service;

import com.thanhtam.backend.dto.UserExport;
import com.thanhtam.backend.entity.Intake;
import com.thanhtam.backend.entity.Role;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.repository.UserRepository;
import com.thanhtam.backend.ultilities.ERole;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class ExcelServiceImplTest {

    private FilesStorageService filesStorageService;
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private RoleService roleService;
    private IntakeService intakeService;

    private ExcelServiceImpl excelService;

    @TempDir
    Path tempDir;

    private Path exportedUsersFile;
    private Path invalidExcelFile;

    @BeforeEach
    void setUp() {
        filesStorageService = mock(FilesStorageService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userRepository = mock(UserRepository.class);
        roleService = mock(RoleService.class);
        intakeService = mock(IntakeService.class);

        excelService = new ExcelServiceImpl(
                filesStorageService,
                passwordEncoder,
                userRepository,
                roleService,
                intakeService
        );

        exportedUsersFile = Paths.get("users.xlsx");
        invalidExcelFile = Paths.get("not-excel.txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(exportedUsersFile);
        try {
            Files.deleteIfExists(invalidExcelFile);
        } catch (IOException ignored) {
            // The implementation leaks the file handle on invalid extensions.
        }
    }

    // Test Case ID: UT_AM_080
    // Kiem thu doc file xlsx va gan role STUDENT mac dinh
    @Test
    void testReadUserFromExcelFile_readsRowAndMapsDefaultStudentRole() throws IOException {
        Path excelFile = tempDir.resolve("users.xlsx");
        writeWorkbook(excelFile, workbook -> {
            Sheet sheet = workbook.createSheet("Users");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("alice");
            row.createCell(1).setCellValue("alice@example.com");
            row.createCell(2).setCellValue("Alice");
            row.createCell(3).setCellValue("Nguyen");
            row.createCell(4).setCellValue("INT-01");
        });

        Intake intake = new Intake(1L, "Intake 1", "INT-01");
        Role studentRole = new Role(3L, ERole.ROLE_STUDENT);
        when(passwordEncoder.encode("alice")).thenReturn("encoded-alice");
        when(intakeService.findByCode("INT-01")).thenReturn(intake);
        when(roleService.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));

        List<User> users = excelService.readUserFromExcelFile(excelFile.toString());

        assertEquals(1, users.size());
        User user = users.get(0);
        assertEquals("alice", user.getUsername());
        assertEquals("encoded-alice", user.getPassword());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals(intake, user.getIntake());
        assertNotNull(user.getRoles());
        assertEquals(Collections.singleton(studentRole), user.getRoles());

        verify(passwordEncoder).encode("alice");
        verify(intakeService).findByCode("INT-01");
        verify(roleService).findByName(ERole.ROLE_STUDENT);

        log.info("[UT_AM_080] users={}", users);
    }

    // Test Case ID: UT_AM_081
    // Kiem thu doc file xlsx va gan role ADMIN khi co cot role
    @Test
    void testReadUserFromExcelFile_usesAdminRole() throws IOException {
        Path excelFile = tempDir.resolve("admins.xlsx");
        writeWorkbook(excelFile, workbook -> {
            Sheet sheet = workbook.createSheet("Users");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("admin");
            row.createCell(1).setCellValue("admin@example.com");
            row.createCell(2).setCellValue("System");
            row.createCell(3).setCellValue("Admin");
            row.createCell(5).setCellValue("ADMIN");
        });

        Role adminRole = new Role(1L, ERole.ROLE_ADMIN);
        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");
        when(roleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

        List<User> users = excelService.readUserFromExcelFile(excelFile.toString());

        assertEquals(1, users.size());
        assertEquals(Collections.singleton(adminRole), users.get(0).getRoles());
        verify(roleService).findByName(ERole.ROLE_ADMIN);

        log.info("[UT_AM_081] users={}", users);
    }

    // Test Case ID: UT_AM_082
    // Kiem thu doc file xls va gan role LECTURER
    @Test
    void testReadUserFromExcelFile_readsXlsAndMapsLecturerRole() throws IOException {
        Path excelFile = tempDir.resolve("lecturers.xls");
        writeWorkbook(excelFile, new HSSFWorkbook(), workbook -> {
            Sheet sheet = workbook.createSheet("Users");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("lecturer");
            row.createCell(1).setCellValue("lecturer@example.com");
            row.createCell(2).setCellValue("Le");
            row.createCell(3).setCellValue("Pham");
            row.createCell(5).setCellValue("LECTURER");
        });

        Role lecturerRole = new Role(2L, ERole.ROLE_LECTURER);
        when(passwordEncoder.encode("lecturer")).thenReturn("encoded-lecturer");
        when(roleService.findByName(ERole.ROLE_LECTURER)).thenReturn(Optional.of(lecturerRole));

        List<User> users = excelService.readUserFromExcelFile(excelFile.toString());

        assertEquals(1, users.size());
        assertEquals(Collections.singleton(lecturerRole), users.get(0).getRoles());
        verify(roleService).findByName(ERole.ROLE_LECTURER);

        log.info("[UT_AM_082] users={}", users);
    }

    // Test Case ID: UT_AM_083
    // Kiem thu nem loi khi role khong ton tai
    @Test
    void testReadUserFromExcelFile_RoleIsMissing() throws IOException {
        Path excelFile = tempDir.resolve("missing-role.xlsx");
        writeWorkbook(excelFile, workbook -> {
            Sheet sheet = workbook.createSheet("Users");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("admin");
            row.createCell(5).setCellValue("ADMIN");
        });

        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");
        when(roleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> excelService.readUserFromExcelFile(excelFile.toString())
        );

        assertEquals("Error: Role is not found", ex.getMessage());

        log.info("[UT_AM_083] exception={}", ex.getMessage());
    }

    // Test Case ID: UT_AM_084
    // Kiem thu tu choi file khong dung dinh dang excel
    @Test
    void testReadUserFromExcelFile_rejectsNonExcelExtension() throws IOException {
        Files.write(invalidExcelFile, Collections.singletonList("plain text"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> excelService.readUserFromExcelFile(invalidExcelFile.toString())
        );

        assertEquals("The specified file is not Excel file", ex.getMessage());

        log.info("[UT_AM_084] exception={}", ex.getMessage());
    }

    // Test Case ID: UT_AM_085
    // Kiem thu nem ClassCastException khi username la boolean
    @Test
    void testReadUserFromExcelFile_UsernameIsBoolean() throws IOException {
        Path excelFile = tempDir.resolve("boolean-username.xlsx");
        writeWorkbook(excelFile, workbook -> {
            Sheet sheet = workbook.createSheet("Users");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(true);
        });

        ClassCastException ex = assertThrows(
                ClassCastException.class,
                () -> excelService.readUserFromExcelFile(excelFile.toString())
        );

        log.info("[UT_AM_085] exception={}", ex.getClass().getSimpleName());
    }

    // Test Case ID: UT_AM_086
    // Kiem thu nem ClassCastException khi username la so
    @Test
    void testReadUserFromExcelFile_UsernameIsNumeric() throws IOException {
        Path excelFile = tempDir.resolve("numeric-username.xlsx");
        writeWorkbook(excelFile, workbook -> {
            Sheet sheet = workbook.createSheet("Users");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(123);
        });

        ClassCastException ex = assertThrows(
                ClassCastException.class,
                () -> excelService.readUserFromExcelFile(excelFile.toString())
        );

        log.info("[UT_AM_086] exception={}", ex.getClass().getSimpleName());
    }

    // Test Case ID: UT_AM_087
    // Kiem thu xuat file excel chua danh sach user
    @Test
    void testWriteUserToExcelFile() throws IOException {
        ArrayList<UserExport> exports = new ArrayList<>(Arrays.asList(
                new UserExport("alice", "alice@example.com", "Alice", "Nguyen"),
                new UserExport("bob", "bob@example.com", "Bob", "Tran")
        ));

        excelService.writeUserToExcelFile(exports);

        assertTrue(Files.exists(exportedUsersFile));
        try (InputStream inputStream = Files.newInputStream(exportedUsersFile);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("List of users", sheet.getSheetName());
            assertEquals("Username", sheet.getRow(0).getCell(0).getStringCellValue());
            assertTrue(sheet.getRow(0).getCell(1).getStringCellValue().length() > 0);
            assertEquals("Email", sheet.getRow(0).getCell(2).getStringCellValue());

            assertEquals("alice", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Alice", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Nguyen", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("alice@example.com", sheet.getRow(1).getCell(3).getStringCellValue());

            assertEquals("bob", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("Bob", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("Tran", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("bob@example.com", sheet.getRow(2).getCell(3).getStringCellValue());

            log.info("[UT_AM_087] exportedFile={}, rows={}", exportedUsersFile, sheet.getLastRowNum() + 1);
        }
    }

    // Test Case ID: UT_AM_088
    // Kiem thu chi luu user chua ton tai trong DB
    @Test
    void testInsertUserToDB_savesOnlyUsersNotExist() {
        User existingUser = new User();
        existingUser.setUsername("existing");
        existingUser.setEmail("existing@example.com");

        User newUser = new User();
        newUser.setUsername("new-user");
        newUser.setEmail("new-user@example.com");

        when(userRepository.existsByEmailOrUsername(existingUser.getEmail(), existingUser.getUsername())).thenReturn(true);
        when(userRepository.existsByEmailOrUsername(newUser.getEmail(), newUser.getUsername())).thenReturn(false);

        excelService.InsertUserToDB(Arrays.asList(existingUser, newUser));

        verify(userRepository, never()).save(existingUser);
        verify(userRepository).save(newUser);

        log.info("[UT_AM_088] existingUser={}, newUser={}", existingUser.getUsername(), newUser.getUsername());
    }

    private void writeWorkbook(Path file, WorkbookWriter writer) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            writer.accept(workbook);
            try (java.io.OutputStream outputStream = Files.newOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeWorkbook(Path file, Workbook workbook, WorkbookWriter writer) throws IOException {
        try (Workbook closableWorkbook = workbook) {
            writer.accept(closableWorkbook);
            try (java.io.OutputStream outputStream = Files.newOutputStream(file)) {
                closableWorkbook.write(outputStream);
            }
        }
    }

    @FunctionalInterface
    private interface WorkbookWriter {
        void accept(Workbook workbook) throws IOException;
    }
}
