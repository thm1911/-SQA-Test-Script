package com.thanhtam.backend.exam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhtam.backend.controller.ExamController;
import com.thanhtam.backend.dto.*;
import com.thanhtam.backend.entity.*;
import com.thanhtam.backend.service.*;
import com.thanhtam.backend.ultilities.DifficultyLevel;
import com.thanhtam.backend.ultilities.EQTypeCode;
import com.thanhtam.backend.ultilities.ERole;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================================
 * Unit Test cho ExamController - Tầng Controller quản lý bài kiểm tra
 * ============================================================================
 * Mô tả: Test các API endpoint của ExamController bao gồm:
 *         - CRUD bài kiểm tra (tạo, xem, hủy)
 *         - Quản lý câu hỏi trong bài thi
 *         - Lấy kết quả bài thi
 *         - Lưu câu trả lời của user
 *         - Lịch thi
 * Phương pháp: Sử dụng Mockito để mock Service layer
 * Rollback: Mock-based. @Before/@After reset state giữa các test.
 * ============================================================================
 */
@RunWith(MockitoJUnitRunner.class)
public class ExamControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(ExamControllerTest.class);

    @Mock
    private ExamService examService;

    @Mock
    private QuestionService questionService;

    @Mock
    private UserService userService;

    @Mock
    private IntakeService intakeService;

    @Mock
    private PartService partService;

    @Mock
    private ExamUserService examUserService;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private ExamController examController;

    // ============ Test Data ============
    private Exam sampleExam;
    private User adminUser;
    private User lecturerUser;
    private User studentUser;
    private Intake sampleIntake;
    private Part samplePart;
    private Course sampleCourse;
    private ExamUser sampleExamUser;
    private Question sampleQuestion;
    private QuestionType questionTypeMC;
    private Role adminRole;
    private Role lecturerRole;
    private Role studentRole;

    @Before
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test ExamController...");

        // Roles
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        lecturerRole = new Role();
        lecturerRole.setId(2L);
        lecturerRole.setName(ERole.ROLE_LECTURER);

        studentRole = new Role();
        studentRole.setId(3L);
        studentRole.setName(ERole.ROLE_STUDENT);

        // Course & Part & Intake
        sampleCourse = new Course();
        sampleCourse.setId(1L);
        sampleCourse.setCourseCode("CS101");
        sampleCourse.setName("Lập trình Java");

        samplePart = new Part();
        samplePart.setId(1L);
        samplePart.setName("Chương 1 - Tổng quan");
        samplePart.setCourse(sampleCourse);

        sampleIntake = new Intake();
        sampleIntake.setId(1L);
        sampleIntake.setName("Khóa 2024");
        sampleIntake.setIntakeCode("K2024");

        // Users
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@ptit.edu.vn");
        adminUser.setRoles(new HashSet<>(Collections.singletonList(adminRole)));

        lecturerUser = new User();
        lecturerUser.setId(2L);
        lecturerUser.setUsername("lecturer01");
        lecturerUser.setEmail("lecturer01@ptit.edu.vn");
        lecturerUser.setRoles(new HashSet<>(Collections.singletonList(lecturerRole)));

        studentUser = new User();
        studentUser.setId(3L);
        studentUser.setUsername("student01");
        studentUser.setEmail("student01@ptit.edu.vn");
        studentUser.setRoles(new HashSet<>(Collections.singletonList(studentRole)));
        studentUser.setIntake(sampleIntake);

        // Question Type
        questionTypeMC = new QuestionType();
        questionTypeMC.setId(1L);
        questionTypeMC.setTypeCode(EQTypeCode.MC);
        questionTypeMC.setDescription("Multiple Choice");

        // Question
        sampleQuestion = new Question();
        sampleQuestion.setId(1L);
        sampleQuestion.setQuestionText("Kiểu dữ liệu nào sau đây là kiểu nguyên thủy?");
        sampleQuestion.setQuestionType(questionTypeMC);
        sampleQuestion.setDifficultyLevel(DifficultyLevel.EASY);
        sampleQuestion.setPoint(10);
        sampleQuestion.setPart(samplePart);

        Choice c1 = new Choice(1L, "int", 1);
        Choice c2 = new Choice(2L, "String", 0);
        sampleQuestion.setChoices(Arrays.asList(c1, c2));

        // Exam
        sampleExam = new Exam();
        sampleExam.setId(1L);
        sampleExam.setTitle("Kiểm tra giữa kỳ Java - K2024");
        sampleExam.setIntake(sampleIntake);
        sampleExam.setPart(samplePart);
        sampleExam.setShuffle(false);
        sampleExam.setCanceled(false);
        sampleExam.setDurationExam(60);
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() - 1800000)); // 30 phút trước
        sampleExam.setFinishExam(new Date(System.currentTimeMillis() + 1800000)); // 30 phút sau
        sampleExam.setQuestionData("[{\"questionId\":1,\"point\":10}]");

        // ExamUser
        sampleExamUser = new ExamUser();
        sampleExamUser.setId(1L);
        sampleExamUser.setExam(sampleExam);
        sampleExamUser.setUser(studentUser);
        sampleExamUser.setIsStarted(false);
        sampleExamUser.setIsFinished(false);
        sampleExamUser.setRemainingTime(3600);
        sampleExamUser.setTotalPoint(-1.0);

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    @After
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test ExamController...");
        reset(examService, questionService, userService, intakeService, partService, examUserService);
        sampleExam = null;
        adminUser = null;
        lecturerUser = null;
        studentUser = null;
        sampleIntake = null;
        samplePart = null;
        sampleCourse = null;
        sampleExamUser = null;
        sampleQuestion = null;
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES CHO getExamsByPage() - API: GET /api/exams
    // ========================================================================================

    /**
     * TC_EC_001: Admin lấy danh sách tất cả exam phân trang
     * Mô tả: Admin có quyền xem tất cả exam, không bị giới hạn bởi người tạo
     * Input: Admin user, Pageable(page=0, size=10)
     * Expected: Trả về PageResult chứa tất cả exams
     */
    @Test
    public void TC_EC_001_getExamsByPage_asAdmin_shouldReturnAllExams() {
        logger.info("[TC_EC_001] BẮT ĐẦU: Admin lấy danh sách tất cả exam");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Exam> examPage = new PageImpl<>(Arrays.asList(sampleExam), pageable, 1);

        when(userService.getUserName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(examService.findAll(pageable)).thenReturn(examPage);
        logger.info("[TC_EC_001] Input: user=admin, Pageable(page=0, size=10)");

        // Act
        PageResult result = examController.getExamsByPage(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertNotNull("Data không được null", result.getData());
        assertEquals("Phải có 1 exam", 1, result.getData().size());

        verify(examService, times(1)).findAll(pageable);
        verify(examService, never()).findAllByCreatedBy_Username(any(), any());
        logger.info("[TC_EC_001] KẾT QUẢ: PASSED - Admin thấy tất cả {} exams", result.getData().size());
    }

    /**
     * TC_EC_002: Lecturer chỉ thấy exam do mình tạo
     * Mô tả: Lecturer bị giới hạn chỉ thấy các exam do chính mình tạo
     * Input: Lecturer user, Pageable(page=0, size=10)
     * Expected: Chỉ trả về exams tạo bởi lecturer đó
     */
    @Test
    public void TC_EC_002_getExamsByPage_asLecturer_shouldReturnOwnExams() {
        logger.info("[TC_EC_002] BẮT ĐẦU: Lecturer chỉ thấy exam do mình tạo");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Exam> examPage = new PageImpl<>(Arrays.asList(sampleExam), pageable, 1);

        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(examService.findAllByCreatedBy_Username(pageable, "lecturer01")).thenReturn(examPage);
        logger.info("[TC_EC_002] Input: user=lecturer01, Pageable(page=0, size=10)");

        // Act
        PageResult result = examController.getExamsByPage(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 exam", 1, result.getData().size());

        verify(examService, never()).findAll(pageable);
        verify(examService, times(1)).findAllByCreatedBy_Username(pageable, "lecturer01");
        logger.info("[TC_EC_002] KẾT QUẢ: PASSED - Lecturer chỉ thấy exam của mình");
    }

    // ========================================================================================
    // TEST CASES CHO getExamById() - API: GET /api/exams/{id}
    // ========================================================================================

    /**
     * TC_EC_003: Lấy chi tiết exam theo ID - tồn tại
     * Mô tả: Kiểm tra lấy thông tin chi tiết của 1 exam bằng ID
     * Input: examId = 1
     * Expected: ResponseEntity(200) chứa Exam
     */
    @Test
    public void TC_EC_003_getExamById_existingId_shouldReturn200() {
        logger.info("[TC_EC_003] BẮT ĐẦU: Lấy chi tiết exam theo ID - tồn tại");
        logger.info("[TC_EC_003] Input: examId={}", 1L);

        // Arrange
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));

        // Act
        ResponseEntity<Exam> response = examController.getExamById(1L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Body không được null", response.getBody());
        assertEquals("Exam title phải khớp", sampleExam.getTitle(), response.getBody().getTitle());

        logger.info("[TC_EC_003] KẾT QUẢ: PASSED - Trả về Exam ID={}", response.getBody().getId());
    }

    /**
     * TC_EC_004: Lấy chi tiết exam theo ID - không tồn tại
     * Mô tả: Kiểm tra khi ID không có trong DB
     * Input: examId = 999
     * Expected: ResponseEntity(204 NO_CONTENT) hoặc exception
     */
    @Test
    public void TC_EC_004_getExamById_nonExistingId_shouldReturnNoContent() {
        logger.info("[TC_EC_004] BẮT ĐẦU: Lấy chi tiết exam theo ID - không tồn tại");
        logger.info("[TC_EC_004] Input: examId={}", 999L);

        // Arrange
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        try {
            ResponseEntity<Exam> response = examController.getExamById(999L);
            // Nếu không throw exception, status phải là NO_CONTENT
            assertEquals("Status phải là 204 NO_CONTENT", HttpStatus.NO_CONTENT, response.getStatusCode());
            logger.info("[TC_EC_004] KẾT QUẢ: PASSED - Trả về 204 NO_CONTENT");
        } catch (NoSuchElementException e) {
            // Expected: Optional.get() throws NoSuchElementException khi empty
            logger.info("[TC_EC_004] KẾT QUẢ: PASSED - Exception NoSuchElementException khi ID không tồn tại");
        }
    }

    // ========================================================================================
    // TEST CASES CHO createExam() - API: POST /api/exams
    // ========================================================================================

    /**
     * TC_EC_005: Tạo bài kiểm tra mới thành công
     * Mô tả: Kiểm tra tạo exam với đầy đủ thông tin hợp lệ
     * Input: Exam object, intakeId=1, partId=1, isShuffle=false
     * Expected: ResponseEntity(200) chứa Exam đã tạo
     */
    @Test
    public void TC_EC_005_createExam_withValidData_shouldReturn200() throws Exception {
        logger.info("[TC_EC_005] BẮT ĐẦU: Tạo bài kiểm tra mới thành công");

        // Arrange
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(1L)).thenReturn(Optional.of(sampleIntake));
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));
        when(examService.saveExam(any(Exam.class))).thenReturn(sampleExam);
        when(userService.findAllByIntakeId(1L)).thenReturn(Arrays.asList(studentUser));

        logger.info("[TC_EC_005] Input: Exam(title='{}'), intakeId=1, partId=1, shuffle=false",
                sampleExam.getTitle());

        // Act
        ResponseEntity<?> response = examController.createExam(sampleExam, 1L, 1L, false, false);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());

        verify(examService, times(1)).saveExam(any(Exam.class));
        verify(examUserService, times(1)).create(any(Exam.class), anyList());
        logger.info("[TC_EC_005] KẾT QUẢ: PASSED - Exam được tạo thành công");
    }

    /**
     * TC_EC_006: Tạo bài kiểm tra với shuffle = true
     * Mô tả: Kiểm tra tạo exam có bật chế độ xáo trộn câu hỏi
     * Input: Exam object, isShuffle = true
     * Expected: ResponseEntity(200), exam.isShuffle = true
     */
    @Test
    public void TC_EC_006_createExam_withShuffle_shouldCreateWithShuffle() throws Exception {
        logger.info("[TC_EC_006] BẮT ĐẦU: Tạo bài kiểm tra với shuffle = true");

        // Arrange
        sampleExam.setShuffle(true);
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(1L)).thenReturn(Optional.of(sampleIntake));
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));
        when(examService.saveExam(any(Exam.class))).thenReturn(sampleExam);
        when(userService.findAllByIntakeId(1L)).thenReturn(Arrays.asList(studentUser));

        logger.info("[TC_EC_006] Input: isShuffle=true");

        // Act
        ResponseEntity<?> response = examController.createExam(sampleExam, 1L, 1L, true, false);

        // Assert
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());

        verify(examService, times(1)).saveExam(any(Exam.class));
        logger.info("[TC_EC_006] KẾT QUẢ: PASSED - Exam shuffle được tạo");
    }

    /**
     * TC_EC_007: Tạo bài kiểm tra khi intake không tồn tại
     * Mô tả: Kiểm tra khi intakeId không hợp lệ (intake không tìm thấy)
     * Input: intakeId = 999 (không tồn tại)
     * Expected: Exam vẫn được tạo nhưng intake = null
     */
    @Test
    public void TC_EC_007_createExam_withInvalidIntake_shouldStillCreate() throws Exception {
        logger.info("[TC_EC_007] BẮT ĐẦU: Tạo exam với intake không tồn tại");

        // Arrange
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(999L)).thenReturn(Optional.empty());
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));
        when(examService.saveExam(any(Exam.class))).thenReturn(sampleExam);
        when(userService.findAllByIntakeId(999L)).thenReturn(Collections.emptyList());

        logger.info("[TC_EC_007] Input: intakeId=999 (không tồn tại)");

        // Act
        ResponseEntity<?> response = examController.createExam(sampleExam, 999L, 1L, false, false);

        // Assert
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());
        verify(examService, times(1)).saveExam(any(Exam.class));

        logger.info("[TC_EC_007] KẾT QUẢ: PASSED - Exam được tạo dù intake không tồn tại");
    }

    // ========================================================================================
    // TEST CASES CHO getExamUserById() - API: GET /api/exams/exam-user/{examId}
    // ========================================================================================

    /**
     * TC_EC_008: Lấy exam-user bằng examId - tồn tại
     * Mô tả: Kiểm tra lấy quan hệ Exam-User theo examId cho user hiện tại
     * Input: examId=1, current user=student01
     * Expected: ResponseEntity(200) chứa ExamUser
     */
    @Test
    public void TC_EC_008_getExamUserById_exists_shouldReturn200() throws ParseException {
        logger.info("[TC_EC_008] BẮT ĐẦU: Lấy exam-user bằng examId - tồn tại");
        logger.info("[TC_EC_008] Input: examId={}, user='{}'", 1L, "student01");

        // Arrange
        when(userService.getUserName()).thenReturn("student01");
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(sampleExamUser);

        // Act
        ResponseEntity<ExamUser> response = examController.getExamUserById(1L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Body chứa ExamUser", response.getBody());
        assertEquals("ExamUser ID khớp", Long.valueOf(1L), response.getBody().getId());

        logger.info("[TC_EC_008] KẾT QUẢ: PASSED - ExamUser tìm thấy");
    }

    /**
     * TC_EC_009: Lấy exam-user bằng examId - không tồn tại
     * Mô tả: Kiểm tra khi không tìm thấy quan hệ Exam-User
     * Input: examId=999, current user=student01
     * Expected: ResponseEntity(404 NOT_FOUND)
     */
    @Test
    public void TC_EC_009_getExamUserById_notExists_shouldReturn404() throws ParseException {
        logger.info("[TC_EC_009] BẮT ĐẦU: Lấy exam-user - không tồn tại");
        logger.info("[TC_EC_009] Input: examId={}, user='{}'", 999L, "student01");

        // Arrange
        when(userService.getUserName()).thenReturn("student01");
        when(examUserService.findByExamAndUser(999L, "student01")).thenReturn(null);

        // Act
        ResponseEntity<ExamUser> response = examController.getExamUserById(999L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 404 NOT_FOUND", HttpStatus.NOT_FOUND, response.getStatusCode());

        logger.info("[TC_EC_009] KẾT QUẢ: PASSED - Trả về 404 NOT_FOUND");
    }

    // ========================================================================================
    // TEST CASES CHO cancelExam() - API: GET /api/exams/{id}/cancel
    // ========================================================================================

    /**
     * TC_EC_010: Hủy bài kiểm tra - exam chưa bắt đầu
     * Mô tả: Kiểm tra hủy exam khi thời gian bắt đầu chưa tới
     * Input: examId=1, exam.beginExam > now
     * Expected: examService.cancelExam() được gọi
     */
    @Test
    public void TC_EC_010_cancelExam_beforeBegin_shouldCancel() {
        logger.info("[TC_EC_010] BẮT ĐẦU: Hủy exam - chưa bắt đầu");

        // Arrange - exam bắt đầu trong tương lai
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() + 86400000)); // 1 ngày sau
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        logger.info("[TC_EC_010] Input: examId=1, beginExam > now");

        // Act
        examController.cancelExam(1L);

        // Assert
        verify(examService, times(1)).cancelExam(1L);
        logger.info("[TC_EC_010] KẾT QUẢ: PASSED - Exam đã bị hủy");
    }

    /**
     * TC_EC_011: Hủy bài kiểm tra - exam đã bắt đầu
     * Mô tả: Kiểm tra hành vi khi hủy exam đã bắt đầu
     * Input: examId=1, exam.beginExam < now
     * Expected: examService.cancelExam() KHÔNG được gọi
     */
    @Test
    public void TC_EC_011_cancelExam_afterBegin_shouldNotCancel() {
        logger.info("[TC_EC_011] BẮT ĐẦU: Hủy exam - đã bắt đầu");

        // Arrange - exam đã bắt đầu
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() - 3600000)); // 1h trước
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        logger.info("[TC_EC_011] Input: examId=1, beginExam < now (đã bắt đầu)");

        // Act
        examController.cancelExam(1L);

        // Assert
        verify(examService, never()).cancelExam(anyLong());
        logger.info("[TC_EC_011] KẾT QUẢ: PASSED - Exam đã bắt đầu không bị hủy");
    }

    // ========================================================================================
    // TEST CASES CHO getResultExam() - API: GET /api/exams/{examId}/result
    // ========================================================================================

    /**
     * TC_EC_012: Lấy kết quả bài thi - exam tồn tại
     * Mô tả: Kiểm tra lấy kết quả bài thi khi exam tồn tại trong DB
     * Input: examId=1, current user=student01
     * Expected: ResponseEntity(200) chứa ExamResult
     */
    @Test
    public void TC_EC_012_getResultExam_examExists_shouldReturn200() throws IOException {
        logger.info("[TC_EC_012] BẮT ĐẦU: Lấy kết quả bài thi - exam tồn tại");
        logger.info("[TC_EC_012] Input: examId={}", 1L);

        // Arrange
        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));

        ExamUser finishedExamUser = new ExamUser();
        finishedExamUser.setExam(sampleExam);
        finishedExamUser.setUser(studentUser);
        finishedExamUser.setIsFinished(true);
        finishedExamUser.setTotalPoint(-1.0);

        // Giả lập answerSheet đã có
        Choice userChoice = new Choice(1L, "int", 1);
        AnswerSheet as = new AnswerSheet(1L, Arrays.asList(userChoice), 10);
        String answerJson = mapper.writeValueAsString(Arrays.asList(as));
        finishedExamUser.setAnswerSheet(answerJson);

        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(finishedExamUser);

        // Mock choice list
        ChoiceList choiceList = new ChoiceList();
        choiceList.setQuestion(sampleQuestion);
        choiceList.setPoint(10);
        choiceList.setIsSelectedCorrected(true);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(Arrays.asList(choiceList));

        // Act
        ResponseEntity response = examController.getResultExam(1L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Body chứa ExamResult", response.getBody());
        assertTrue("Body phải là ExamResult", response.getBody() instanceof ExamResult);

        ExamResult result = (ExamResult) response.getBody();
        assertEquals("TotalPoint phải = 10.0", Double.valueOf(10.0), result.getTotalPoint());

        logger.info("[TC_EC_012] KẾT QUẢ: PASSED - Kết quả bài thi: totalPoint={}", result.getTotalPoint());
    }

    /**
     * TC_EC_013: Lấy kết quả bài thi - exam không tồn tại
     * Mô tả: Kiểm tra khi examId không hợp lệ
     * Input: examId=999
     * Expected: ResponseEntity(404 NOT_FOUND)
     */
    @Test
    public void TC_EC_013_getResultExam_examNotExists_shouldReturn404() throws IOException {
        logger.info("[TC_EC_013] BẮT ĐẦU: Lấy kết quả bài thi - exam không tồn tại");
        logger.info("[TC_EC_013] Input: examId={}", 999L);

        // Arrange
        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity response = examController.getResultExam(999L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 404 NOT_FOUND", HttpStatus.NOT_FOUND, response.getStatusCode());

        logger.info("[TC_EC_013] KẾT QUẢ: PASSED - Trả về 404 NOT_FOUND");
    }

    // ========================================================================================
    // TEST CASES CHO getResultExamAll() - API: GET /api/exams/{examId}/result/all
    // ========================================================================================

    /**
     * TC_EC_014: Lấy kết quả tất cả user cho 1 exam - exam không tồn tại
     * Mô tả: Kiểm tra khi exam không có trong DB
     * Input: examId=999
     * Expected: ResponseEntity(404 NOT_FOUND)
     */
    @Test
    public void TC_EC_014_getResultExamAll_examNotExists_shouldReturn404() throws IOException {
        logger.info("[TC_EC_014] BẮT ĐẦU: Lấy kết quả tất cả user - exam không tồn tại");
        logger.info("[TC_EC_014] Input: examId={}", 999L);

        // Arrange
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity response = examController.getResultExamAll(999L);

        // Assert
        assertEquals("Status phải là 404 NOT_FOUND", HttpStatus.NOT_FOUND, response.getStatusCode());
        logger.info("[TC_EC_014] KẾT QUẢ: PASSED - 404 NOT_FOUND");
    }

    /**
     * TC_EC_015: Lấy kết quả tất cả user - exam tồn tại, có users
     * Mô tả: Kiểm tra kết quả toàn bộ khi có 1 user chưa làm bài
     * Input: examId=1, 1 user chưa làm bài (answerSheet rỗng)
     * Expected: ResponseEntity(200), examResult.totalPoint = null, examStatus phù hợp
     */
    @Test
    public void TC_EC_015_getResultExamAll_withUsersNoAnswer_shouldReturn200() throws IOException {
        logger.info("[TC_EC_015] BẮT ĐẦU: Lấy kết quả tất cả user - user chưa làm bài");

        // Arrange
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));

        ExamUser noAnswerUser = new ExamUser();
        noAnswerUser.setExam(sampleExam);
        noAnswerUser.setUser(studentUser);
        noAnswerUser.setIsStarted(false);
        noAnswerUser.setIsFinished(false);
        noAnswerUser.setTotalPoint(-1.0);
        noAnswerUser.setAnswerSheet(null);

        when(examUserService.findAllByExam_Id(1L)).thenReturn(Arrays.asList(noAnswerUser));

        // Act
        ResponseEntity response = examController.getResultExamAll(1L);

        // Assert
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Body không được null", response.getBody());

        logger.info("[TC_EC_015] KẾT QUẢ: PASSED - Trả về kết quả cho user chưa làm bài");
    }

    // ========================================================================================
    // TEST CASES CHO convertAnswerJsonToObject()
    // ========================================================================================

    /**
     * TC_EC_016: Convert JSON answer sheet hợp lệ
     * Mô tả: Parse JSON answer sheet string thành List<AnswerSheet>
     * Input: Valid JSON string
     * Expected: List<AnswerSheet> có dữ liệu đúng
     */
    @Test
    public void TC_EC_016_convertAnswerJsonToObject_validJson_shouldReturnList() throws IOException {
        logger.info("[TC_EC_016] BẮT ĐẦU: Convert JSON answer sheet hợp lệ");

        // Arrange
        String json = "[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"True\",\"isCorrected\":1}],\"point\":10}]";
        sampleExamUser.setAnswerSheet(json);
        logger.info("[TC_EC_016] Input: JSON = '{}'", json);

        // Act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(sampleExamUser);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 answer sheet", 1, result.size());
        assertEquals("QuestionId phải = 1", Long.valueOf(1L), result.get(0).getQuestionId());
        assertEquals("Point phải = 10", Integer.valueOf(10), result.get(0).getPoint());

        logger.info("[TC_EC_016] KẾT QUẢ: PASSED - JSON parsed thành công");
    }

    /**
     * TC_EC_017: Convert answer sheet khi answerSheet null/rỗng
     * Mô tả: Kiểm tra khi user chưa có answer sheet
     * Input: ExamUser với answerSheet = null
     * Expected: List rỗng (emptyList)
     */
    @Test
    public void TC_EC_017_convertAnswerJsonToObject_nullAnswerSheet_shouldReturnEmptyList() throws IOException {
        logger.info("[TC_EC_017] BẮT ĐẦU: Convert answer sheet null");

        // Arrange
        sampleExamUser.setAnswerSheet(null);
        logger.info("[TC_EC_017] Input: answerSheet=null");

        // Act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(sampleExamUser);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[TC_EC_017] KẾT QUẢ: PASSED - Trả về empty list cho null answerSheet");
    }

    /**
     * TC_EC_018: Convert answer sheet khi answerSheet rỗng ""
     * Mô tả: Kiểm tra khi answerSheet là chuỗi rỗng
     * Input: ExamUser với answerSheet = ""
     * Expected: List rỗng (emptyList)
     */
    @Test
    public void TC_EC_018_convertAnswerJsonToObject_emptyString_shouldReturnEmptyList() throws IOException {
        logger.info("[TC_EC_018] BẮT ĐẦU: Convert answer sheet rỗng");

        // Arrange
        sampleExamUser.setAnswerSheet("");
        logger.info("[TC_EC_018] Input: answerSheet=''");

        // Act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(sampleExamUser);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[TC_EC_018] KẾT QUẢ: PASSED - Trả về empty list cho empty string");
    }

    // ========================================================================================
    // TEST CASES CHO convertQuestionJsonToObject()
    // ========================================================================================

    /**
     * TC_EC_019: Convert JSON question data hợp lệ
     * Mô tả: Parse question data JSON từ Exam thành List<ExamQuestionPoint>
     * Input: Exam với questionData JSON hợp lệ
     * Expected: List<ExamQuestionPoint> đúng
     */
    @Test
    public void TC_EC_019_convertQuestionJsonToObject_validJson_shouldReturnList() throws IOException {
        logger.info("[TC_EC_019] BẮT ĐẦU: Convert question data JSON hợp lệ");
        logger.info("[TC_EC_019] Input: questionData='{}'", sampleExam.getQuestionData());

        // Act
        List<ExamQuestionPoint> result = examController.convertQuestionJsonToObject(Optional.of(sampleExam));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 question point", 1, result.size());
        assertEquals("QuestionId phải = 1", Long.valueOf(1L), result.get(0).getQuestionId());
        assertEquals("Point phải = 10", Integer.valueOf(10), result.get(0).getPoint());

        logger.info("[TC_EC_019] KẾT QUẢ: PASSED - Question data parsed thành công");
    }

    /**
     * TC_EC_020: Convert question JSON với nhiều câu hỏi
     * Mô tả: Parse question data có nhiều câu hỏi
     * Input: questionData với 3 câu hỏi
     * Expected: List có 3 phần tử
     */
    @Test
    public void TC_EC_020_convertQuestionJsonToObject_multipleQuestions_shouldReturnAll() throws IOException {
        logger.info("[TC_EC_020] BẮT ĐẦU: Convert question JSON với nhiều câu hỏi");

        // Arrange
        sampleExam.setQuestionData("[{\"questionId\":1,\"point\":10},{\"questionId\":2,\"point\":20},{\"questionId\":3,\"point\":30}]");
        logger.info("[TC_EC_020] Input: 3 câu hỏi");

        // Act
        List<ExamQuestionPoint> result = examController.convertQuestionJsonToObject(Optional.of(sampleExam));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 3 question points", 3, result.size());
        assertEquals("Tổng điểm phải = 60",
                Integer.valueOf(60),
                Integer.valueOf(result.stream().mapToInt(ExamQuestionPoint::getPoint).sum()));

        logger.info("[TC_EC_020] KẾT QUẢ: PASSED - 3 câu hỏi, tổng {} điểm",
                result.stream().mapToInt(ExamQuestionPoint::getPoint).sum());
    }
}
