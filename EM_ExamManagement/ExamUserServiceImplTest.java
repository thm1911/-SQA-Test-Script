package com.thanhtam.backend.exam;

import com.thanhtam.backend.entity.*;
import com.thanhtam.backend.repository.ExamRepository;
import com.thanhtam.backend.repository.ExamUserRepository;
import com.thanhtam.backend.service.ExamUserServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================================
 * Unit Test cho ExamUserServiceImpl - Tầng Service quản lý quan hệ Exam-User
 * ============================================================================
 * Mô tả: Test các phương thức quản lý bài thi của người dùng:
 * tạo exam user, cập nhật trạng thái, lấy danh sách, lọc theo điều kiện
 * Phương pháp: Sử dụng Mockito để mock ExamUserRepository
 * Rollback: Mock-based - không tương tác DB thật.
 * 
 * @Before/@After đảm bảo reset trạng thái giữa các test.
 *                ============================================================================
 */
@RunWith(MockitoJUnitRunner.class)
public class ExamUserServiceImplTest {

    private static final Logger logger = LoggerFactory.getLogger(ExamUserServiceImplTest.class);

    @Mock
    private ExamUserRepository examUserRepository;

    @Mock
    private ExamRepository examRepository;

    @InjectMocks
    private ExamUserServiceImpl examUserService;

    @Captor
    private ArgumentCaptor<List<ExamUser>> examUserListCaptor;

    // ============ Test Data ============
    private Exam sampleExam;
    private User sampleUser1;
    private User sampleUser2;
    private User sampleUser3;
    private ExamUser sampleExamUser;
    private Intake sampleIntake;
    private Profile sampleProfile1;
    private Profile sampleProfile2;

    @Before
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test ExamUserService...");

        // Intake
        sampleIntake = new Intake();
        sampleIntake.setId(1L);
        sampleIntake.setName("Khóa 2024");
        sampleIntake.setIntakeCode("K2024");

        // Profiles
        sampleProfile1 = new Profile();
        sampleProfile1.setId(1L);
        sampleProfile1.setFirstName("Nguyen");
        sampleProfile1.setLastName("Van A");

        sampleProfile2 = new Profile();
        sampleProfile2.setId(2L);
        sampleProfile2.setFirstName("Tran");
        sampleProfile2.setLastName("Thi B");

        // Users
        sampleUser1 = new User();
        sampleUser1.setId(1L);
        sampleUser1.setUsername("student01");
        sampleUser1.setEmail("student01@ptit.edu.vn");
        sampleUser1.setIntake(sampleIntake);
        sampleUser1.setProfile(sampleProfile1);

        sampleUser2 = new User();
        sampleUser2.setId(2L);
        sampleUser2.setUsername("student02");
        sampleUser2.setEmail("student02@ptit.edu.vn");
        sampleUser2.setIntake(sampleIntake);
        sampleUser2.setProfile(sampleProfile2);

        sampleUser3 = new User();
        sampleUser3.setId(3L);
        sampleUser3.setUsername("student03");
        sampleUser3.setEmail("student03@ptit.edu.vn");
        sampleUser3.setIntake(sampleIntake);

        // Exam
        sampleExam = new Exam();
        sampleExam.setId(1L);
        sampleExam.setTitle("Kiểm tra giữa kỳ Java");
        sampleExam.setDurationExam(60);
        sampleExam.setIntake(sampleIntake);
        sampleExam.setCanceled(false);
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() + 3600000));
        sampleExam.setFinishExam(new Date(System.currentTimeMillis() + 7200000));

        // ExamUser
        sampleExamUser = new ExamUser();
        sampleExamUser.setId(1L);
        sampleExamUser.setExam(sampleExam);
        sampleExamUser.setUser(sampleUser1);
        sampleExamUser.setIsStarted(false);
        sampleExamUser.setIsFinished(false);
        sampleExamUser.setRemainingTime(3600);
        sampleExamUser.setTotalPoint(-1.0);

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    @After
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test ExamUserService...");
        reset(examUserRepository, examRepository);
        sampleExam = null;
        sampleUser1 = null;
        sampleUser2 = null;
        sampleUser3 = null;
        sampleExamUser = null;
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES CHO create()
    // ========================================================================================

    /**
     * TC_EU_001: Tạo examUser cho 1 user
     * Mô tả: Kiểm tra tạo ExamUser cho 1 sinh viên khi tạo bài kiểm tra mới
     * Input: Exam(duration=60), List<User> chứa 1 user
     * Expected: examUserRepository.saveAll() được gọi với list chứa 1 ExamUser đúng
     */
    @Test
    public void TC_EU_001_create_singleUser_shouldCreateOneExamUser() {
        logger.info("[TC_EU_001] BẮT ĐẦU: Tạo examUser cho 1 user");
        logger.info("[TC_EU_001] Input: Exam(id={}, duration={}), 1 user(username='{}')",
                sampleExam.getId(), sampleExam.getDurationExam(), sampleUser1.getUsername());

        // Arrange
        List<User> users = Arrays.asList(sampleUser1);

        // Act
        examUserService.create(sampleExam, users);

        // Assert
        verify(examUserRepository, times(1)).saveAll(examUserListCaptor.capture());
        List<ExamUser> savedList = examUserListCaptor.getValue();

        assertEquals("Phải tạo 1 ExamUser", 1, savedList.size());
        assertEquals("User phải khớp", sampleUser1, savedList.get(0).getUser());
        assertEquals("Exam phải khớp", sampleExam, savedList.get(0).getExam());
        assertEquals("RemainingTime phải = duration * 60", 3600, savedList.get(0).getRemainingTime());
        assertEquals("TotalPoint phải = -1.0", Double.valueOf(-1.0), savedList.get(0).getTotalPoint());

        logger.info("[TC_EU_001] KẾT QUẢ: PASSED - Tạo thành công 1 ExamUser");
    }

    /**
     * TC_EU_002: Tạo examUser cho nhiều users
     * Mô tả: Kiểm tra tạo ExamUser cho nhiều sinh viên cùng lúc
     * Input: Exam(duration=60), List<User> chứa 3 users
     * Expected: examUserRepository.saveAll() được gọi với list chứa 3 ExamUsers
     */
    @Test
    public void TC_EU_002_create_multipleUsers_shouldCreateMultipleExamUsers() {
        logger.info("[TC_EU_002] BẮT ĐẦU: Tạo examUser cho nhiều users");
        logger.info("[TC_EU_002] Input: Exam(id={}), 3 users", sampleExam.getId());

        // Arrange
        List<User> users = Arrays.asList(sampleUser1, sampleUser2, sampleUser3);

        // Act
        examUserService.create(sampleExam, users);

        // Assert
        verify(examUserRepository, times(1)).saveAll(examUserListCaptor.capture());
        List<ExamUser> savedList = examUserListCaptor.getValue();

        assertEquals("Phải tạo 3 ExamUsers", 3, savedList.size());
        for (ExamUser eu : savedList) {
            assertEquals("Exam phải khớp cho tất cả", sampleExam, eu.getExam());
            assertEquals("RemainingTime phải = 3600 cho tất cả", 3600, eu.getRemainingTime());
            assertEquals("TotalPoint phải = -1.0 cho tất cả", Double.valueOf(-1.0), eu.getTotalPoint());
        }

        logger.info("[TC_EU_002] KẾT QUẢ: PASSED - Tạo thành công {} ExamUsers", savedList.size());
    }

    /**
     * TC_EU_003: Tạo examUser cho danh sách user rỗng
     * Mô tả: Kiểm tra tạo ExamUser khi không có sinh viên nào
     * Input: Exam, List<User> rỗng
     * Expected: examUserRepository.saveAll() được gọi với list rỗng
     */
    @Test
    public void TC_EU_003_create_emptyUserList_shouldSaveEmptyList() {
        logger.info("[TC_EU_003] BẮT ĐẦU: Tạo examUser cho danh sách user rỗng");

        // Arrange
        List<User> users = Collections.emptyList();

        // Act
        examUserService.create(sampleExam, users);

        // Assert
        verify(examUserRepository, times(1)).saveAll(examUserListCaptor.capture());
        List<ExamUser> savedList = examUserListCaptor.getValue();
        assertTrue("List phải rỗng", savedList.isEmpty());

        logger.info("[TC_EU_003] KẾT QUẢ: PASSED - SaveAll gọi với list rỗng");
    }

    /**
     * TC_EU_004: Kiểm tra remainingTime được tính đúng
     * Mô tả: Kiểm tra remainingTime = durationExam * 60 (giây)
     * Input: Exam(duration=45 phút)
     * Expected: remainingTime = 2700 giây
     */
    @Test
    public void TC_EU_004_create_shouldSetRemainingTimeCorrectly() {
        logger.info("[TC_EU_004] BẮT ĐẦU: Kiểm tra remainingTime được tính đúng");

        // Arrange
        sampleExam.setDurationExam(45); // 45 minutes
        logger.info("[TC_EU_004] Input: Exam(duration=45 phút)");

        // Act
        examUserService.create(sampleExam, Arrays.asList(sampleUser1));

        // Assert
        verify(examUserRepository, times(1)).saveAll(examUserListCaptor.capture());
        List<ExamUser> savedList = examUserListCaptor.getValue();
        assertEquals("RemainingTime phải = 45*60 = 2700", 2700, savedList.get(0).getRemainingTime());

        logger.info("[TC_EU_004] KẾT QUẢ: PASSED - RemainingTime = 2700 giây (45 phút)");
    }

    // ========================================================================================
    // TEST CASES CHO getExamListByUsername()
    // ========================================================================================

    /**
     * TC_EU_005: Lấy danh sách bài thi theo username - có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exam chưa bị hủy của user
     * Input: username = "student01"
     * Expected: List chứa ExamUsers chưa bị canceled
     */
    @Test
    public void TC_EU_005_getExamListByUsername_withExistingData_shouldReturnList() {
        logger.info("[TC_EU_005] BẮT ĐẦU: Lấy danh sách bài thi theo username - có dữ liệu");
        String username = "student01";
        logger.info("[TC_EU_005] Input: username='{}'", username);

        // Arrange
        when(examUserRepository.findAllByUser_UsernameAndExam_Canceled(username, false))
                .thenReturn(Arrays.asList(sampleExamUser));

        // Act
        List<ExamUser> result = examUserService.getExamListByUsername(username);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 ExamUser", 1, result.size());
        assertFalse("Exam không bị canceled", result.get(0).getExam().isCanceled());

        verify(examUserRepository, times(1))
                .findAllByUser_UsernameAndExam_Canceled(username, false);
        logger.info("[TC_EU_005] KẾT QUẢ: PASSED - Trả về {} ExamUsers", result.size());
    }

    /**
     * TC_EU_006: Lấy danh sách bài thi theo username - không có dữ liệu
     * Mô tả: Kiểm tra khi user chưa được gán bất kỳ bài thi nào
     * Input: username = "newstudent"
     * Expected: List rỗng
     */
    @Test
    public void TC_EU_006_getExamListByUsername_withNoData_shouldReturnEmptyList() {
        logger.info("[TC_EU_006] BẮT ĐẦU: Lấy danh sách bài thi theo username - không có dữ liệu");
        String username = "newstudent";
        logger.info("[TC_EU_006] Input: username='{}'", username);

        // Arrange
        when(examUserRepository.findAllByUser_UsernameAndExam_Canceled(username, false))
                .thenReturn(Collections.emptyList());

        // Act
        List<ExamUser> result = examUserService.getExamListByUsername(username);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[TC_EU_006] KẾT QUẢ: PASSED - List rỗng cho user mới");
    }

    // ========================================================================================
    // TEST CASES CHO findByExamAndUser()
    // ========================================================================================

    /**
     * TC_EU_007: Tìm ExamUser theo examId và username - tồn tại
     * Mô tả: Kiểm tra tìm quan hệ Exam-User khi cả 2 tồn tại
     * Input: examId=1, username="student01"
     * Expected: Trả về ExamUser đúng
     */
    @Test
    public void TC_EU_007_findByExamAndUser_exists_shouldReturnExamUser() {
        logger.info("[TC_EU_007] BẮT ĐẦU: Tìm ExamUser theo examId và username - tồn tại");
        logger.info("[TC_EU_007] Input: examId={}, username='{}'", 1L, "student01");

        // Arrange
        when(examUserRepository.findByExam_IdAndUser_Username(1L, "student01"))
                .thenReturn(sampleExamUser);

        // Act
        ExamUser result = examUserService.findByExamAndUser(1L, "student01");

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("ExamUser ID phải khớp", Long.valueOf(1L), result.getId());
        assertEquals("Exam phải khớp", sampleExam, result.getExam());
        assertEquals("User phải khớp", sampleUser1, result.getUser());

        logger.info("[TC_EU_007] KẾT QUẢ: PASSED - Tìm thấy ExamUser ID={}", result.getId());
    }

    /**
     * TC_EU_008: Tìm ExamUser theo examId và username - không tồn tại
     * Mô tả: Kiểm tra tìm quan hệ Exam-User khi không có trong DB
     * Input: examId=999, username="nonexistent"
     * Expected: Trả về null
     */
    @Test
    public void TC_EU_008_findByExamAndUser_notExists_shouldReturnNull() {
        logger.info("[TC_EU_008] BẮT ĐẦU: Tìm ExamUser - không tồn tại");
        logger.info("[TC_EU_008] Input: examId={}, username='{}'", 999L, "nonexistent");

        // Arrange
        when(examUserRepository.findByExam_IdAndUser_Username(999L, "nonexistent"))
                .thenReturn(null);

        // Act
        ExamUser result = examUserService.findByExamAndUser(999L, "nonexistent");

        // Assert
        assertNull("Kết quả phải null", result);

        logger.info("[TC_EU_008] KẾT QUẢ: PASSED - Trả về null khi không tìm thấy");
    }

    // ========================================================================================
    // TEST CASES CHO update()
    // ========================================================================================

    /**
     * TC_EU_009: Cập nhật ExamUser - bắt đầu làm bài
     * Mô tả: Kiểm tra cập nhật trạng thái ExamUser khi user bắt đầu làm bài
     * Input: ExamUser với isStarted=true, timeStart=now
     * Expected: examUserRepository.save() được gọi với dữ liệu đúng
     */
    @Test
    public void TC_EU_009_update_startExam_shouldSaveStartedState() {
        logger.info("[TC_EU_009] BẮT ĐẦU: Cập nhật ExamUser - bắt đầu làm bài");

        // Arrange
        sampleExamUser.setIsStarted(true);
        sampleExamUser.setTimeStart(new Date());
        logger.info("[TC_EU_009] Input: ExamUser(isStarted=true, timeStart=now)");

        // Act
        examUserService.update(sampleExamUser);

        // Assert
        verify(examUserRepository, times(1)).save(sampleExamUser);

        logger.info("[TC_EU_009] KẾT QUẢ: PASSED - ExamUser đã được cập nhật với trạng thái started");
    }

    /**
     * TC_EU_010: Cập nhật ExamUser - nộp bài
     * Mô tả: Kiểm tra cập nhật khi user hoàn thành và nộp bài
     * Input: ExamUser với isFinished=true, timeFinish=now, totalPoint=85.0
     * Expected: examUserRepository.save() được gọi chính xác
     */
    @Test
    public void TC_EU_010_update_finishExam_shouldSaveFinishedState() {
        logger.info("[TC_EU_010] BẮT ĐẦU: Cập nhật ExamUser - nộp bài");

        // Arrange
        sampleExamUser.setIsStarted(true);
        sampleExamUser.setIsFinished(true);
        sampleExamUser.setTimeStart(new Date(System.currentTimeMillis() - 1800000));
        sampleExamUser.setTimeFinish(new Date());
        sampleExamUser.setTotalPoint(85.0);
        sampleExamUser.setRemainingTime(1800);
        logger.info("[TC_EU_010] Input: ExamUser(isFinished=true, totalPoint=85.0, remainingTime=1800)");

        // Act
        examUserService.update(sampleExamUser);

        // Assert
        verify(examUserRepository, times(1)).save(sampleExamUser);

        logger.info("[TC_EU_010] KẾT QUẢ: PASSED - ExamUser đã được cập nhật trạng thái finished");
    }

    /**
     * TC_EU_011: Cập nhật ExamUser - lưu bài giữa chừng (auto-save)
     * Mô tả: Kiểm tra cập nhật khi user lưu bài giữa chừng (isFinished=false)
     * Input: ExamUser với answerSheet updated, isFinished=false, remainingTime giảm
     * Expected: examUserRepository.save() được gọi
     */
    @Test
    public void TC_EU_011_update_autoSave_shouldSaveInterimState() {
        logger.info("[TC_EU_011] BẮT ĐẦU: Cập nhật ExamUser - auto-save giữa chừng");

        // Arrange
        sampleExamUser.setIsStarted(true);
        sampleExamUser.setIsFinished(false);
        sampleExamUser.setRemainingTime(2400); // còn 40 phút
        sampleExamUser.setAnswerSheet("[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"True\"}]}]");
        logger.info("[TC_EU_011] Input: ExamUser(isFinished=false, remainingTime=2400, answerSheet=updated)");

        // Act
        examUserService.update(sampleExamUser);

        // Assert
        verify(examUserRepository, times(1)).save(sampleExamUser);

        logger.info("[TC_EU_011] KẾT QUẢ: PASSED - Auto-save thành công");
    }

    // ========================================================================================
    // TEST CASES CHO findExamUserById()
    // ========================================================================================

    /**
     * TC_EU_012: Tìm ExamUser theo ID - tồn tại
     * Mô tả: Kiểm tra tìm ExamUser bằng primary key
     * Input: examUserId = 1
     * Expected: Optional chứa ExamUser
     */
    @Test
    public void TC_EU_012_findExamUserById_exists_shouldReturnOptional() {
        logger.info("[TC_EU_012] BẮT ĐẦU: Tìm ExamUser theo ID - tồn tại");
        logger.info("[TC_EU_012] Input: examUserId={}", 1L);

        // Arrange
        when(examUserRepository.findById(1L)).thenReturn(Optional.of(sampleExamUser));

        // Act
        Optional<ExamUser> result = examUserService.findExamUserById(1L);

        // Assert
        assertTrue("Kết quả phải có giá trị", result.isPresent());
        assertEquals("ExamUser ID phải khớp", Long.valueOf(1L), result.get().getId());

        logger.info("[TC_EU_012] KẾT QUẢ: PASSED - Tìm thấy ExamUser ID={}", result.get().getId());
    }

    /**
     * TC_EU_013: Tìm ExamUser theo ID - không tồn tại
     * Mô tả: Kiểm tra tìm ExamUser bằng ID không có trong DB
     * Input: examUserId = 999
     * Expected: Optional.empty()
     */
    @Test
    public void TC_EU_013_findExamUserById_notExists_shouldReturnEmpty() {
        logger.info("[TC_EU_013] BẮT ĐẦU: Tìm ExamUser theo ID - không tồn tại");
        logger.info("[TC_EU_013] Input: examUserId={}", 999L);

        // Arrange
        when(examUserRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<ExamUser> result = examUserService.findExamUserById(999L);

        // Assert
        assertFalse("Kết quả phải rỗng", result.isPresent());

        logger.info("[TC_EU_013] KẾT QUẢ: PASSED - Không tìm thấy ExamUser ID=999");
    }

    // ========================================================================================
    // TEST CASES CHO getCompleteExams()
    // ========================================================================================

    /**
     * TC_EU_014: Lấy danh sách bài thi hoàn thành theo khóa học
     * Mô tả: Kiểm tra lấy các ExamUser có totalPoint > -1 (đã hoàn thành)
     * Input: courseId=1, username="student01"
     * Expected: List chứa ExamUsers đã hoàn thành
     */
    @Test
    public void TC_EU_014_getCompleteExams_withData_shouldReturnCompletedExams() {
        logger.info("[TC_EU_014] BẮT ĐẦU: Lấy danh sách bài thi hoàn thành");
        logger.info("[TC_EU_014] Input: courseId={}, username='{}'", 1L, "student01");

        // Arrange
        ExamUser completedExamUser = new ExamUser();
        completedExamUser.setId(10L);
        completedExamUser.setTotalPoint(85.0);
        completedExamUser.setIsFinished(true);

        when(examUserRepository.findAllByExam_Part_Course_IdAndUser_UsernameAndTotalPointIsGreaterThan(
                1L, "student01", -1.0)).thenReturn(Arrays.asList(completedExamUser));

        // Act
        List<ExamUser> result = examUserService.getCompleteExams(1L, "student01");

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 exam hoàn thành", 1, result.size());
        assertTrue("TotalPoint phải > -1", result.get(0).getTotalPoint() > -1.0);

        logger.info("[TC_EU_014] KẾT QUẢ: PASSED - Tìm thấy {} bài thi hoàn thành", result.size());
    }

    /**
     * TC_EU_015: Lấy danh sách bài thi hoàn thành - không có kết quả
     * Mô tả: Kiểm tra khi user chưa hoàn thành bài thi nào
     * Input: courseId=1, username="student01"
     * Expected: List rỗng
     */
    @Test
    public void TC_EU_015_getCompleteExams_noData_shouldReturnEmptyList() {
        logger.info("[TC_EU_015] BẮT ĐẦU: Lấy bài thi hoàn thành - không có kết quả");
        logger.info("[TC_EU_015] Input: courseId={}, username='{}'", 1L, "student01");

        // Arrange
        when(examUserRepository.findAllByExam_Part_Course_IdAndUser_UsernameAndTotalPointIsGreaterThan(
                1L, "student01", -1.0)).thenReturn(Collections.emptyList());

        // Act
        List<ExamUser> result = examUserService.getCompleteExams(1L, "student01");

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[TC_EU_015] KẾT QUẢ: PASSED - Chưa có bài thi hoàn thành");
    }

    // ========================================================================================
    // TEST CASES CHO findAllByExam_Id()
    // ========================================================================================

    /**
     * TC_EU_016: Lấy danh sách tất cả ExamUser theo examId
     * Mô tả: Kiểm tra lấy tất cả user được gán cho 1 bài thi
     * Input: examId = 1
     * Expected: List chứa tất cả ExamUsers của exam đó
     */
    @Test
    public void TC_EU_016_findAllByExam_Id_shouldReturnAllExamUsers() {
        logger.info("[TC_EU_016] BẮT ĐẦU: Lấy tất cả ExamUser theo examId");
        logger.info("[TC_EU_016] Input: examId={}", 1L);

        // Arrange
        ExamUser eu1 = new ExamUser();
        eu1.setId(1L);
        eu1.setExam(sampleExam);
        eu1.setUser(sampleUser1);

        ExamUser eu2 = new ExamUser();
        eu2.setId(2L);
        eu2.setExam(sampleExam);
        eu2.setUser(sampleUser2);

        when(examUserRepository.findAllByExam_Id(1L)).thenReturn(Arrays.asList(eu1, eu2));

        // Act
        List<ExamUser> result = examUserService.findAllByExam_Id(1L);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 2 ExamUsers", 2, result.size());

        logger.info("[TC_EU_016] KẾT QUẢ: PASSED - Tìm thấy {} ExamUsers cho exam ID=1", result.size());
    }

    // ========================================================================================
    // TEST CASES CHO findExamUsersByIsFinishedIsTrueAndExam_Id()
    // ========================================================================================

    /**
     * TC_EU_017: Lấy danh sách user đã hoàn thành bài thi theo examId
     * Mô tả: Kiểm tra lọc chỉ những ExamUser có isFinished=true
     * Input: examId = 1
     * Expected: Chỉ trả về ExamUsers đã nộp bài
     */
    @Test
    public void TC_EU_017_findFinishedExamUsers_shouldReturnOnlyFinished() {
        logger.info("[TC_EU_017] BẮT ĐẦU: Lấy ExamUsers đã hoàn thành");
        logger.info("[TC_EU_017] Input: examId={}", 1L);

        // Arrange
        ExamUser finishedEU = new ExamUser();
        finishedEU.setId(1L);
        finishedEU.setIsFinished(true);
        finishedEU.setTotalPoint(75.0);

        when(examUserRepository.findExamUsersByIsFinishedIsTrueAndExam_Id(1L))
                .thenReturn(Arrays.asList(finishedEU));

        // Act
        List<ExamUser> result = examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(1L);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 ExamUser hoàn thành", 1, result.size());
        assertTrue("ExamUser phải isFinished=true", result.get(0).getIsFinished());

        logger.info("[TC_EU_017] KẾT QUẢ: PASSED - Tìm thấy {} ExamUsers đã hoàn thành", result.size());
    }

    /**
     * TC_EU_018: Lấy ExamUsers hoàn thành khi chưa ai nộp bài
     * Mô tả: Kiểm tra khi không có user nào hoàn thành bài thi
     * Input: examId = 1
     * Expected: List rỗng
     */
    @Test
    public void TC_EU_018_findFinishedExamUsers_noneFinished_shouldReturnEmpty() {
        logger.info("[TC_EU_018] BẮT ĐẦU: Lấy ExamUsers hoàn thành - chưa ai nộp bài");
        logger.info("[TC_EU_018] Input: examId={}", 1L);

        // Arrange
        when(examUserRepository.findExamUsersByIsFinishedIsTrueAndExam_Id(1L))
                .thenReturn(Collections.emptyList());

        // Act
        List<ExamUser> result = examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(1L);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[TC_EU_018] KẾT QUẢ: PASSED - Chưa có ai nộp bài");
    }
}
