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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
        SecurityContextHolder.clearContext();
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
     * UT_EM_001: Admin lấy danh sách tất cả exam phân trang
     * Mô tả: Admin có quyền xem tất cả exam, không bị giới hạn bởi người tạo
     * Input: Admin user, Pageable(page=0, size=10)
     * Expected: Trả về PageResult chứa tất cả exams
     */
    @Test
    public void UT_EM_001_getExamsByPage_asAdmin_shouldReturnAllExams() {
        logger.info("[UT_EM_001] BẮT ĐẦU: Admin lấy danh sách tất cả exam");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Exam> examPage = new PageImpl<>(Arrays.asList(sampleExam), pageable, 1);

        when(userService.getUserName()).thenReturn("admin");
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(examService.findAll(pageable)).thenReturn(examPage);
        when(examService.findAllByCreatedBy_Username(pageable, "admin"))
            .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
        logger.info("[UT_EM_001] Input: user=admin, Pageable(page=0, size=10)");

        // Act
        PageResult result = examController.getExamsByPage(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertNotNull("Data không được null", result.getData());
        assertEquals("Phải có 1 exam", 1, result.getData().size());

        verify(examService, times(1)).findAll(pageable);
        verify(examService, never()).findAllByCreatedBy_Username(any(), any());
        logger.info("[UT_EM_001] KẾT QUẢ: PASSED - Admin thấy tất cả {} exams", result.getData().size());
    }

    /**
     * UT_EM_002: Lecturer chỉ thấy exam do mình tạo
     * Mô tả: Lecturer bị giới hạn chỉ thấy các exam do chính mình tạo
     * Input: Lecturer user, Pageable(page=0, size=10)
     * Expected: Chỉ trả về exams tạo bởi lecturer đó
     */
    @Test
    public void UT_EM_002_getExamsByPage_asLecturer_shouldReturnOwnExams() {
        logger.info("[UT_EM_002] BẮT ĐẦU: Lecturer chỉ thấy exam do mình tạo");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Exam> examPage = new PageImpl<>(Arrays.asList(sampleExam), pageable, 1);

        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(examService.findAllByCreatedBy_Username(pageable, "lecturer01")).thenReturn(examPage);
        logger.info("[UT_EM_002] Input: user=lecturer01, Pageable(page=0, size=10)");

        // Act
        PageResult result = examController.getExamsByPage(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 exam", 1, result.getData().size());

        verify(examService, never()).findAll(pageable);
        verify(examService, times(1)).findAllByCreatedBy_Username(pageable, "lecturer01");
        logger.info("[UT_EM_002] KẾT QUẢ: PASSED - Lecturer chỉ thấy exam của mình");
    }

    // ========================================================================================
    // TEST CASES CHO getExamById() - API: GET /api/exams/{id}
    // ========================================================================================

    /**
     * UT_EM_003: Lấy chi tiết exam theo ID - tồn tại
     * Mô tả: Kiểm tra lấy thông tin chi tiết của 1 exam bằng ID
     * Input: examId = 1
     * Expected: ResponseEntity(200) chứa Exam
     */
    @Test
    public void UT_EM_003_getExamById_existingId_shouldReturn200() {
        logger.info("[UT_EM_003] BẮT ĐẦU: Lấy chi tiết exam theo ID - tồn tại");
        logger.info("[UT_EM_003] Input: examId={}", 1L);

        // Arrange
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));

        // Act
        ResponseEntity<Exam> response = examController.getExamById(1L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());
        assertNotNull("Body không được null", response.getBody());
        assertEquals("Exam title phải khớp", sampleExam.getTitle(), response.getBody().getTitle());

        logger.info("[UT_EM_003] KẾT QUẢ: PASSED - Trả về Exam ID={}", response.getBody().getId());
    }

    /**
     * UT_EM_004: Lấy chi tiết exam theo ID - không tồn tại
     * Mô tả: Kiểm tra khi ID không có trong DB
     * Input: examId = 999
     * Expected: ResponseEntity(204 NO_CONTENT), không ném exception
     */
    @Test
    public void UT_EM_004_getExamById_nonExistingId_shouldReturnNoContentWithoutThrowing() {
        logger.info("[UT_EM_004] BẮT ĐẦU: Lấy chi tiết exam theo ID - không tồn tại");
        logger.info("[UT_EM_004] Input: examId={}", 999L);

        // Arrange
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Exam> response = null;
        try {
            response = examController.getExamById(999L);
        } catch (NoSuchElementException ex) {
            fail("Controller không được ném NoSuchElementException khi exam không tồn tại");
        }

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 204 NO_CONTENT", HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull("Body phải null khi không tồn tại", response.getBody());

        logger.info("[UT_EM_004] KẾT QUẢ: PASSED - Trả về 204 NO_CONTENT");
    }

    // ========================================================================================
    // TEST CASES CHO createExam() - API: POST /api/exams
    // ========================================================================================

    /**
     * UT_EM_005: Tạo bài kiểm tra mới thành công
     * Mô tả: Kiểm tra tạo exam với đầy đủ thông tin hợp lệ
     * Input: Exam object, intakeId=1, partId=1, isShuffle=false
     * Expected: ResponseEntity(200) chứa Exam đã tạo
     */
    @Test
    public void UT_EM_005_createExam_withValidData_shouldReturn200() throws Exception {
        logger.info("[UT_EM_005] BẮT ĐẦU: Tạo bài kiểm tra mới thành công");

        // Arrange
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(1L)).thenReturn(Optional.of(sampleIntake));
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));
        when(examService.saveExam(any(Exam.class))).thenReturn(sampleExam);
        when(userService.findAllByIntakeId(1L)).thenReturn(Arrays.asList(studentUser));

        logger.info("[UT_EM_005] Input: Exam(title='{}'), intakeId=1, partId=1, shuffle=false",
                sampleExam.getTitle());

        // Act
        ResponseEntity<?> response = examController.createExam(sampleExam, 1L, 1L, false, false);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());

        verify(examService, times(1)).saveExam(any(Exam.class));
        verify(examUserService, times(1)).create(any(Exam.class), anyList());
        logger.info("[UT_EM_005] KẾT QUẢ: PASSED - Exam được tạo thành công");
    }

    /**
     * UT_EM_006: Tạo bài kiểm tra với shuffle = true
     * Mô tả: Kiểm tra tạo exam có bật chế độ xáo trộn câu hỏi
     * Input: Exam object, isShuffle = true
     * Expected: ResponseEntity(200), exam.isShuffle = true
     */
    @Test
    public void UT_EM_006_createExam_withShuffle_shouldCreateWithShuffle() throws Exception {
        logger.info("[UT_EM_006] BẮT ĐẦU: Tạo bài kiểm tra với shuffle = true");

        // Arrange
        sampleExam.setShuffle(true);
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(1L)).thenReturn(Optional.of(sampleIntake));
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));
        when(examService.saveExam(any(Exam.class))).thenReturn(sampleExam);
        when(userService.findAllByIntakeId(1L)).thenReturn(Arrays.asList(studentUser));

        logger.info("[UT_EM_006] Input: isShuffle=true");

        // Act
        ResponseEntity<?> response = examController.createExam(sampleExam, 1L, 1L, true, false);

        // Assert
        assertEquals("Status phải là 200 OK", HttpStatus.OK, response.getStatusCode());

        verify(examService, times(1)).saveExam(any(Exam.class));
        logger.info("[UT_EM_006] KẾT QUẢ: PASSED - Exam shuffle được tạo");
    }

    /**
     * UT_EM_007: Tạo bài kiểm tra khi intake không tồn tại
     * Mô tả: Kiểm tra khi intakeId không hợp lệ (intake không tìm thấy)
     * Input: intakeId = 999 (không tồn tại)
     * Expected: Service phải từ chối tạo exam và không được persist dữ liệu
     */
    @Test
    public void UT_EM_007_createExam_withInvalidIntake_shouldRejectAndNotCreate() throws Exception {
        logger.info("[UT_EM_007] BẮT ĐẦU: Tạo exam với intake không tồn tại");

        // Arrange
        sampleExam.setIntake(null);
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(999L)).thenReturn(Optional.empty());
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));

        logger.info("[UT_EM_007] Input: intakeId=999 (không tồn tại)");

        // Act
        ResponseEntity<?> response = examController.createExam(sampleExam, 999L, 1L, false, false);

        // Assert
        assertTrue("Status phải là mã lỗi (4xx/5xx) khi intake không tồn tại",
                response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
        verify(examService, never()).saveExam(any(Exam.class));
        verify(examUserService, never()).create(any(Exam.class), anyList());

        logger.info("[UT_EM_007] KẾT QUẢ: PASSED - Intake không hợp lệ bị từ chối, exam không được tạo");
    }

    // ========================================================================================
    // TEST CASES CHO getExamUserById() - API: GET /api/exams/exam-user/{examId}
    // ========================================================================================

    /**
     * UT_EM_008: Lấy exam-user bằng examId - tồn tại
     * Mô tả: Kiểm tra lấy quan hệ Exam-User theo examId cho user hiện tại
     * Input: examId=1, current user=student01
     * Expected: ResponseEntity(200) chứa ExamUser
     */
    @Test
    public void UT_EM_008_getExamUserById_exists_shouldReturn200() throws ParseException {
        logger.info("[UT_EM_008] BẮT ĐẦU: Lấy exam-user bằng examId - tồn tại");
        logger.info("[UT_EM_008] Input: examId={}, user='{}'", 1L, "student01");

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

        logger.info("[UT_EM_008] KẾT QUẢ: PASSED - ExamUser tìm thấy");
    }

    /**
     * UT_EM_009: Lấy exam-user bằng examId - không tồn tại
     * Mô tả: Kiểm tra khi không tìm thấy quan hệ Exam-User
     * Input: examId=999, current user=student01
     * Expected: ResponseEntity(404 NOT_FOUND)
     */
    @Test
    public void UT_EM_009_getExamUserById_notExists_shouldReturn404() throws ParseException {
        logger.info("[UT_EM_009] BẮT ĐẦU: Lấy exam-user - không tồn tại");
        logger.info("[UT_EM_009] Input: examId={}, user='{}'", 999L, "student01");

        // Arrange
        when(userService.getUserName()).thenReturn("student01");
        when(examUserService.findByExamAndUser(999L, "student01")).thenReturn(null);

        // Act
        ResponseEntity<ExamUser> response = examController.getExamUserById(999L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 404 NOT_FOUND", HttpStatus.NOT_FOUND, response.getStatusCode());

        logger.info("[UT_EM_009] KẾT QUẢ: PASSED - Trả về 404 NOT_FOUND");
    }

    // ========================================================================================
    // TEST CASES CHO cancelExam() - API: GET /api/exams/{id}/cancel
    // ========================================================================================

    /**
     * UT_EM_010: Hủy bài kiểm tra - exam chưa bắt đầu
     * Mô tả: Kiểm tra hủy exam khi thời gian bắt đầu chưa tới
     * Input: examId=1, exam.beginExam > now
     * Expected: examService.cancelExam() được gọi
     */
    @Test
    public void UT_EM_010_cancelExam_beforeBegin_shouldCancel() {
        logger.info("[UT_EM_010] BẮT ĐẦU: Hủy exam - chưa bắt đầu");

        // Arrange - exam bắt đầu trong tương lai
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() + 86400000)); // 1 ngày sau
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        logger.info("[UT_EM_010] Input: examId=1, beginExam > now");

        // Act
        examController.cancelExam(1L);

        // Assert
        verify(examService, times(1)).cancelExam(1L);
        logger.info("[UT_EM_010] KẾT QUẢ: PASSED - Exam đã bị hủy");
    }

    /**
     * UT_EM_011: Hủy bài kiểm tra - exam đã bắt đầu
     * Mô tả: Kiểm tra hành vi khi hủy exam đã bắt đầu
     * Input: examId=1, exam.beginExam < now
     * Expected: examService.cancelExam() KHÔNG được gọi
     */
    @Test
    public void UT_EM_011_cancelExam_afterBegin_shouldNotCancel() {
        logger.info("[UT_EM_011] BẮT ĐẦU: Hủy exam - đã bắt đầu");

        // Arrange - exam đã bắt đầu
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() - 3600000)); // 1h trước
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        logger.info("[UT_EM_011] Input: examId=1, beginExam < now (đã bắt đầu)");

        // Act
        examController.cancelExam(1L);

        // Assert
        verify(examService, never()).cancelExam(anyLong());
        logger.info("[UT_EM_011] KẾT QUẢ: PASSED - Exam đã bắt đầu không bị hủy");
    }

    // ========================================================================================
    // TEST CASES CHO getResultExam() - API: GET /api/exams/{examId}/result
    // ========================================================================================

    /**
     * UT_EM_012: Lấy kết quả bài thi - exam tồn tại
     * Mô tả: Kiểm tra lấy kết quả bài thi khi exam tồn tại trong DB
     * Input: examId=1, current user=student01
     * Expected: ResponseEntity(200) chứa ExamResult
     */
    @Test
    public void UT_EM_012_getResultExam_examExists_shouldReturn200() throws IOException {
        logger.info("[UT_EM_012] BẮT ĐẦU: Lấy kết quả bài thi - exam tồn tại");
        logger.info("[UT_EM_012] Input: examId={}", 1L);

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

        logger.info("[UT_EM_012] KẾT QUẢ: PASSED - Kết quả bài thi: totalPoint={}", result.getTotalPoint());
    }

    /**
     * UT_EM_013: Lấy kết quả bài thi - exam không tồn tại
     * Mô tả: Kiểm tra khi examId không hợp lệ
     * Input: examId=999
     * Expected: ResponseEntity(404 NOT_FOUND)
     */
    @Test
    public void UT_EM_013_getResultExam_examNotExists_shouldReturn404() throws IOException {
        logger.info("[UT_EM_013] BẮT ĐẦU: Lấy kết quả bài thi - exam không tồn tại");
        logger.info("[UT_EM_013] Input: examId={}", 999L);

        // Arrange
        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity response = examController.getResultExam(999L);

        // Assert
        assertNotNull("Response không được null", response);
        assertEquals("Status phải là 404 NOT_FOUND", HttpStatus.NOT_FOUND, response.getStatusCode());

        logger.info("[UT_EM_013] KẾT QUẢ: PASSED - Trả về 404 NOT_FOUND");
    }

    // ========================================================================================
    // TEST CASES CHO getResultExamAll() - API: GET /api/exams/{examId}/result/all
    // ========================================================================================

    /**
     * UT_EM_014: Lấy kết quả tất cả user cho 1 exam - exam không tồn tại
     * Mô tả: Kiểm tra khi exam không có trong DB
     * Input: examId=999
     * Expected: ResponseEntity(404 NOT_FOUND)
     */
    @Test
    public void UT_EM_014_getResultExamAll_examNotExists_shouldReturn404() throws IOException {
        logger.info("[UT_EM_014] BẮT ĐẦU: Lấy kết quả tất cả user - exam không tồn tại");
        logger.info("[UT_EM_014] Input: examId={}", 999L);

        // Arrange
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity response = examController.getResultExamAll(999L);

        // Assert
        assertEquals("Status phải là 404 NOT_FOUND", HttpStatus.NOT_FOUND, response.getStatusCode());
        logger.info("[UT_EM_014] KẾT QUẢ: PASSED - 404 NOT_FOUND");
    }

    /**
     * UT_EM_015: Lấy kết quả tất cả user - exam tồn tại, có users
     * Mô tả: Kiểm tra kết quả toàn bộ khi có 1 user chưa làm bài
     * Input: examId=1, 1 user chưa làm bài (answerSheet rỗng)
        * Expected: ResponseEntity(200), examResult.totalPoint = null, examStatus = 0
     */
    @Test
    public void UT_EM_015_getResultExamAll_withUsersNoAnswer_shouldReturn200() throws IOException {
        logger.info("[UT_EM_015] BẮT ĐẦU: Lấy kết quả tất cả user - user chưa làm bài");

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
        @SuppressWarnings("unchecked")
        List<ExamResult> results = (List<ExamResult>) response.getBody();
        assertEquals("Phải có đúng 1 kết quả", 1, results.size());
        assertNull("User chưa làm bài phải có totalPoint = null", results.get(0).getTotalPoint());
        assertEquals("User chưa làm bài phải có examStatus = 0", 0, results.get(0).getExamStatus());

        logger.info("[UT_EM_015] KẾT QUẢ: PASSED - Trả về kết quả cho user chưa làm bài");
    }

    // ========================================================================================
    // TEST CASES CHO convertAnswerJsonToObject()
    // ========================================================================================

    /**
     * UT_EM_016: Convert JSON answer sheet hợp lệ
     * Mô tả: Parse JSON answer sheet string thành List<AnswerSheet>
     * Input: Valid JSON string
     * Expected: List<AnswerSheet> có dữ liệu đúng
     */
    @Test
    public void UT_EM_016_convertAnswerJsonToObject_validJson_shouldReturnList() throws IOException {
        logger.info("[UT_EM_016] BẮT ĐẦU: Convert JSON answer sheet hợp lệ");

        // Arrange
        String json = "[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"True\",\"isCorrected\":1}],\"point\":10}]";
        sampleExamUser.setAnswerSheet(json);
        logger.info("[UT_EM_016] Input: JSON = '{}'", json);

        // Act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(sampleExamUser);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 answer sheet", 1, result.size());
        assertEquals("QuestionId phải = 1", Long.valueOf(1L), result.get(0).getQuestionId());
        assertEquals("Point phải = 10", Integer.valueOf(10), result.get(0).getPoint());

        logger.info("[UT_EM_016] KẾT QUẢ: PASSED - JSON parsed thành công");
    }

    /**
     * UT_EM_017: Convert answer sheet khi answerSheet null/rỗng
     * Mô tả: Kiểm tra khi user chưa có answer sheet
     * Input: ExamUser với answerSheet = null
     * Expected: List rỗng (emptyList)
     */
    @Test
    public void UT_EM_017_convertAnswerJsonToObject_nullAnswerSheet_shouldReturnEmptyList() throws IOException {
        logger.info("[UT_EM_017] BẮT ĐẦU: Convert answer sheet null");

        // Arrange
        sampleExamUser.setAnswerSheet(null);
        logger.info("[UT_EM_017] Input: answerSheet=null");

        // Act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(sampleExamUser);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[UT_EM_017] KẾT QUẢ: PASSED - Trả về empty list cho null answerSheet");
    }

    /**
     * UT_EM_018: Convert answer sheet khi answerSheet rỗng ""
     * Mô tả: Kiểm tra khi answerSheet là chuỗi rỗng
     * Input: ExamUser với answerSheet = ""
     * Expected: List rỗng (emptyList)
     */
    @Test
    public void UT_EM_018_convertAnswerJsonToObject_emptyString_shouldReturnEmptyList() throws IOException {
        logger.info("[UT_EM_018] BẮT ĐẦU: Convert answer sheet rỗng");

        // Arrange
        sampleExamUser.setAnswerSheet("");
        logger.info("[UT_EM_018] Input: answerSheet=''");

        // Act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(sampleExamUser);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[UT_EM_018] KẾT QUẢ: PASSED - Trả về empty list cho empty string");
    }

    // ========================================================================================
    // TEST CASES CHO convertQuestionJsonToObject()
    // ========================================================================================

    /**
     * UT_EM_019: Convert JSON question data hợp lệ
     * Mô tả: Parse question data JSON từ Exam thành List<ExamQuestionPoint>
     * Input: Exam với questionData JSON hợp lệ
     * Expected: List<ExamQuestionPoint> đúng
     */
    @Test
    public void UT_EM_019_convertQuestionJsonToObject_validJson_shouldReturnList() throws IOException {
        logger.info("[UT_EM_019] BẮT ĐẦU: Convert question data JSON hợp lệ");
        logger.info("[UT_EM_019] Input: questionData='{}'", sampleExam.getQuestionData());

        // Act
        List<ExamQuestionPoint> result = examController.convertQuestionJsonToObject(Optional.of(sampleExam));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 question point", 1, result.size());
        assertEquals("QuestionId phải = 1", Long.valueOf(1L), result.get(0).getQuestionId());
        assertEquals("Point phải = 10", Integer.valueOf(10), result.get(0).getPoint());

        logger.info("[UT_EM_019] KẾT QUẢ: PASSED - Question data parsed thành công");
    }

    /**
     * UT_EM_020: Convert question JSON với nhiều câu hỏi
     * Mô tả: Parse question data có nhiều câu hỏi
     * Input: questionData với 3 câu hỏi
     * Expected: List có 3 phần tử
     */
    @Test
    public void UT_EM_020_convertQuestionJsonToObject_multipleQuestions_shouldReturnAll() throws IOException {
        logger.info("[UT_EM_020] BẮT ĐẦU: Convert question JSON với nhiều câu hỏi");

        // Arrange
        sampleExam.setQuestionData("[{\"questionId\":1,\"point\":10},{\"questionId\":2,\"point\":20},{\"questionId\":3,\"point\":30}]");
        logger.info("[UT_EM_020] Input: 3 câu hỏi");

        // Act
        List<ExamQuestionPoint> result = examController.convertQuestionJsonToObject(Optional.of(sampleExam));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 3 question points", 3, result.size());
        assertEquals("Tổng điểm phải = 60",
                Integer.valueOf(60),
                Integer.valueOf(result.stream().mapToInt(ExamQuestionPoint::getPoint).sum()));

        logger.info("[UT_EM_020] KẾT QUẢ: PASSED - 3 câu hỏi, tổng {} điểm",
                result.stream().mapToInt(ExamQuestionPoint::getPoint).sum());
    }

    // ========================================================================================
    // TEST CASES BỔ SUNG CHO CÁC ENDPOINT CHƯA ĐƯỢC BAO PHỦ
    // ========================================================================================

    /**
     * UT_EM_021: Lấy danh sách exam theo user hiện tại
     * Mô tả: API phải trả về danh sách exam-user và gán trạng thái locked theo thời gian bắt đầu
     * Input: username=student01, 1 exam chưa bắt đầu + 1 exam đã bắt đầu
     * Expected: Response 200, exam chưa bắt đầu locked=false, exam đã bắt đầu locked=true
     */
    @Test
    public void UT_EM_021_getAllByUser_shouldReturnListAndSetLockedState() {
        logger.info("[UT_EM_021] BẮT ĐẦU: getAllByUser phải trả đúng danh sách và trạng thái khóa");

        setAuthentication("student01");

        Exam upcomingExam = new Exam();
        upcomingExam.setId(11L);
        upcomingExam.setTitle("Bài thi sắp diễn ra");
        upcomingExam.setBeginExam(new Date(System.currentTimeMillis() + 600000));

        Exam startedExam = new Exam();
        startedExam.setId(12L);
        startedExam.setTitle("Bài thi đã bắt đầu");
        startedExam.setBeginExam(new Date(System.currentTimeMillis() - 600000));

        ExamUser euUpcoming = new ExamUser();
        euUpcoming.setExam(upcomingExam);
        ExamUser euStarted = new ExamUser();
        euStarted.setExam(startedExam);

        when(examUserService.getExamListByUsername("student01"))
                .thenReturn(Arrays.asList(euUpcoming, euStarted));

        ResponseEntity<List<ExamUser>> response = examController.getAllByUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertFalse("Bài chưa tới giờ phải unlocked", response.getBody().get(0).getExam().isLocked());
        assertTrue("Bài đã tới giờ phải locked", response.getBody().get(1).getExam().isLocked());
    }

    /**
     * UT_EM_022: Lấy danh sách câu hỏi khi exam không tồn tại
     * Mô tả: Kiểm tra endpoint trả về NOT_FOUND khi examId không có
     * Input: examId=999
     * Expected: Response 404
     */
    @Test
    public void UT_EM_022_getAllQuestions_examNotFound_shouldReturn404() throws IOException {
        logger.info("[UT_EM_022] BẮT ĐẦU: getAllQuestions khi exam không tồn tại");

        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * UT_EM_023: Lấy danh sách câu hỏi khi exam đang khóa
     * Mô tả: Khi exam bị khóa thì không cho phép vào đề
     * Input: exam.locked=true
     * Expected: Response 400 BAD_REQUEST
     */
    @Test
    public void UT_EM_023_getAllQuestions_lockedExam_shouldReturnBadRequest() throws IOException {
        logger.info("[UT_EM_023] BẮT ĐẦU: getAllQuestions khi exam bị khóa");

        sampleExam.setLocked(true);
        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));

        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * UT_EM_024: Lấy danh sách câu hỏi khi user đã bắt đầu làm bài
     * Mô tả: Hệ thống phải trả về bộ câu hỏi từ answer sheet đã lưu của user
     * Input: examUser.isStarted=true, answerSheet hợp lệ
     * Expected: Response 200, trả đúng câu hỏi và remainingTime
     */
    @Test
    public void UT_EM_024_getAllQuestions_startedExam_shouldReturnSavedQuestionList() throws IOException {
        logger.info("[UT_EM_024] BẮT ĐẦU: getAllQuestions khi user đã bắt đầu làm bài");

        sampleExam.setLocked(false);
        sampleExamUser.setIsStarted(true);
        sampleExamUser.setRemainingTime(1800);
        sampleExamUser.setAnswerSheet("[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"int\",\"isCorrected\":1}],\"point\":10}]");

        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(sampleExamUser);
        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(sampleQuestion));

        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getQuestions().size());
        assertEquals(1800, response.getBody().getRemainingTime());
    }

    /**
     * UT_EM_025: Lưu câu trả lời khi không tồn tại exam-user
     * Mô tả: Nếu không tìm thấy exam-user theo examId+username thì phải báo lỗi
     * Input: examId=1, username=student01, examUser=null
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_EM_025_saveUserExamAnswer_examUserNotFound_shouldThrowEntityNotFound() throws JsonProcessingException {
        logger.info("[UT_EM_025] BẮT ĐẦU: saveUserExamAnswer khi không tìm thấy exam-user");

        setAuthentication("student01");
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(null);

        try {
            examController.saveUserExamAnswer(Collections.emptyList(), 1L, false, 3500);
            fail("Phải ném EntityNotFoundException");
        } catch (javax.persistence.EntityNotFoundException expected) {
            assertTrue(expected.getMessage().contains("Not found this exam"));
        }
    }

    /**
     * UT_EM_026: Lưu câu trả lời khi bài thi đã kết thúc
     * Mô tả: Không cho phép cập nhật bài thi đã finish
     * Input: examUser.isFinished=true
     * Expected: Ném ExceptionInInitializerError
     */
    @Test
    public void UT_EM_026_saveUserExamAnswer_finishedExam_shouldThrowExceptionInInitializerError() throws JsonProcessingException {
        logger.info("[UT_EM_026] BẮT ĐẦU: saveUserExamAnswer khi bài thi đã kết thúc");

        setAuthentication("student01");
        sampleExamUser.setIsFinished(true);
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(sampleExamUser);

        try {
            examController.saveUserExamAnswer(Collections.emptyList(), 1L, false, 3000);
            fail("Phải ném ExceptionInInitializerError");
        } catch (ExceptionInInitializerError expected) {
            assertTrue(expected.getMessage().contains("This exam was end"));
        }
    }

    /**
     * UT_EM_027: Lưu câu trả lời khi nộp bài thành công
     * Mô tả: Kiểm tra cập nhật isFinished, remainingTime, timeFinish và answerSheet
     * Input: isFinish=true, remainingTime=1200
     * Expected: examUserService.update() được gọi với dữ liệu đã cập nhật
     */
    @Test
    public void UT_EM_027_saveUserExamAnswer_finishExam_shouldUpdateState() throws JsonProcessingException {
        logger.info("[UT_EM_027] BẮT ĐẦU: saveUserExamAnswer cập nhật trạng thái nộp bài");

        setAuthentication("student01");
        sampleExamUser.setIsFinished(false);
        sampleExamUser.setTimeFinish(null);
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(sampleExamUser);

        List<AnswerSheet> answerSheets = Arrays.asList(new AnswerSheet(1L,
                Arrays.asList(new Choice(1L, "int", 1)), 10));

        examController.saveUserExamAnswer(answerSheets, 1L, true, 1200);

        assertTrue(sampleExamUser.getIsFinished());
        assertEquals(1200, sampleExamUser.getRemainingTime());
        assertNotNull(sampleExamUser.getTimeFinish());
        assertNotNull(sampleExamUser.getAnswerSheet());
        verify(examUserService, times(1)).update(sampleExamUser);
    }

    /**
     * UT_EM_028: Lấy báo cáo câu hỏi khi exam không tồn tại
     * Mô tả: Endpoint report phải trả về NOT_FOUND cho examId không hợp lệ
     * Input: examId=999
     * Expected: Response 404
     */
    @Test
    public void UT_EM_028_getResultExamQuestionsReport_examNotFound_shouldReturn404() throws IOException {
        logger.info("[UT_EM_028] BẮT ĐẦU: getResultExamQuestionsReport khi exam không tồn tại");

        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        ResponseEntity response = examController.getResultExamQuestionsReport(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * UT_EM_029: Lấy báo cáo câu hỏi khi chưa có ai nộp bài
     * Mô tả: Hệ thống trả về thông báo chưa có người dùng làm bài
     * Input: finishedExamUsers = empty
     * Expected: Response 200 và body chứa thông báo phù hợp
     */
    @Test
    public void UT_EM_029_getResultExamQuestionsReport_noFinishedUsers_shouldReturnMessage() throws IOException {
        logger.info("[UT_EM_029] BẮT ĐẦU: getResultExamQuestionsReport khi chưa ai nộp bài");

        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(1L)).thenReturn(Collections.emptyList());

        ResponseEntity response = examController.getResultExamQuestionsReport(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Chưa có người dùng thực hiện bài kiểm tra"));
    }

    /**
     * UT_EM_030: Lấy báo cáo câu hỏi phải cộng đúng theo từng user
     * Mô tả: Mỗi user phải được chấm từ answerSheet riêng, không dùng lặp lại answer của user đầu
     * Input: 2 users, user1 đúng, user2 sai
     * Expected: correctTotal = 1
     */
    @Test
    public void UT_EM_030_getResultExamQuestionsReport_shouldAggregatePerUserCorrectly() throws IOException {
        logger.info("[UT_EM_030] BẮT ĐẦU: getResultExamQuestionsReport phải cộng kết quả theo từng user");

        sampleExam.setQuestionData("[{\"questionId\":1,\"point\":10}]");

        ExamUser user1 = new ExamUser();
        user1.setUser(studentUser);
        user1.setIsFinished(true);
        user1.setAnswerSheet("[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"int\",\"isCorrected\":1}],\"point\":10}]");

        ExamUser user2 = new ExamUser();
        user2.setUser(lecturerUser);
        user2.setIsFinished(true);
        user2.setAnswerSheet("[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"int\",\"isCorrected\":0}],\"point\":10}]");

        ChoiceList firstUserChoiceList = new ChoiceList();
        firstUserChoiceList.setQuestion(sampleQuestion);
        firstUserChoiceList.setIsSelectedCorrected(true);

        ChoiceList secondUserChoiceList = new ChoiceList();
        secondUserChoiceList.setQuestion(sampleQuestion);
        secondUserChoiceList.setIsSelectedCorrected(false);

        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(1L)).thenReturn(Arrays.asList(user1, user2));
        when(examService.getChoiceList(anyList(), anyList()))
                .thenReturn(Arrays.asList(firstUserChoiceList), Arrays.asList(secondUserChoiceList));

        ResponseEntity response = examController.getResultExamQuestionsReport(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<QuestionExamReport> reports = (List<QuestionExamReport>) response.getBody();
        assertNotNull(reports);
        assertEquals(1, reports.size());
        assertEquals("Theo chuẩn phải chỉ có 1 user trả lời đúng", 1, reports.get(0).getCorrectTotal());
    }

    /**
     * UT_EM_031: Lấy kết quả theo user khi exam không tồn tại
     * Mô tả: Endpoint phải trả 404 nếu examId không tồn tại
     * Input: examId=999, username=student01
     * Expected: Response 404
     */
    @Test
    public void UT_EM_031_getResultExamByUser_examNotFound_shouldReturn404() throws IOException {
        logger.info("[UT_EM_031] BẮT ĐẦU: getResultExamByUser khi exam không tồn tại");

        when(examService.getExamById(999L)).thenReturn(Optional.empty());
        when(userService.getUserByUsername("student01")).thenReturn(Optional.of(studentUser));

        ResponseEntity response = examController.getResultExamByUser(999L, "student01");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * UT_EM_032: Lấy kết quả theo user thành công
     * Mô tả: Kiểm tra tổng điểm, thời lượng đã làm, thông tin user và cập nhật totalPoint lần đầu
     * Input: exam hợp lệ, examUser có answerSheet đúng
     * Expected: Response 200 chứa ExamResult đúng dữ liệu
     */
    @Test
    public void UT_EM_032_getResultExamByUser_success_shouldReturnComputedResult() throws IOException {
        logger.info("[UT_EM_032] BẮT ĐẦU: getResultExamByUser phải tính điểm và thời lượng đã làm");

        sampleExam.setDurationExam(60);
        sampleExamUser.setTimeStart(new Date(System.currentTimeMillis() - 1200000));
        sampleExamUser.setTimeFinish(new Date());
        sampleExamUser.setRemainingTime(2400);
        sampleExamUser.setTotalPoint(-1.0);
        sampleExamUser.setAnswerSheet("[{\"questionId\":1,\"choices\":[{\"id\":1,\"choiceText\":\"int\",\"isCorrected\":1}],\"point\":10}]");

        ChoiceList choiceList = new ChoiceList();
        choiceList.setQuestion(sampleQuestion);
        choiceList.setPoint(10);
        choiceList.setIsSelectedCorrected(true);

        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        when(userService.getUserByUsername("student01")).thenReturn(Optional.of(studentUser));
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(sampleExamUser);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(Arrays.asList(choiceList));

        ResponseEntity response = examController.getResultExamByUser(1L, "student01");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ExamResult);
        ExamResult result = (ExamResult) response.getBody();
        assertEquals(Double.valueOf(10.0), result.getTotalPoint());
        assertEquals(1200, result.getRemainingTime());
        assertEquals(studentUser, result.getUser());
        verify(examUserService, times(1)).update(sampleExamUser);
    }

    /**
     * UT_EM_033: Lấy danh sách text câu hỏi theo examId
     * Mô tả: Trả về đầy đủ questionText, point, difficulty, questionType
     * Input: exam có questionData hợp lệ
     * Expected: List<ExamDetail> đúng nội dung
     */
    @Test
    public void UT_EM_033_getQuestionTextByExamId_shouldReturnExamDetails() throws IOException {
        logger.info("[UT_EM_033] BẮT ĐẦU: getQuestionTextByExamId trả về chi tiết câu hỏi");

        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(sampleQuestion));

        List<ExamDetail> details = examController.getQuestionTextByExamId(1L);

        assertNotNull(details);
        assertEquals(1, details.size());
        assertEquals(sampleQuestion.getQuestionText(), details.get(0).getQuestionText());
        assertEquals(10, details.get(0).getPoint());
    }

    /**
     * UT_EM_034: Lấy question text khi exam không tồn tại
     * Mô tả: Kiểm tra nhánh lỗi Optional.get khi exam rỗng
     * Input: examId=999
     * Expected: Ném NoSuchElementException
     */
    @Test
    public void UT_EM_034_getQuestionTextByExamId_examNotFound_shouldThrowNoSuchElement() {
        logger.info("[UT_EM_034] BẮT ĐẦU: getQuestionTextByExamId khi exam không tồn tại");

        when(examService.getExamById(999L)).thenReturn(Optional.empty());

        try {
            examController.getQuestionTextByExamId(999L);
            fail("Phải ném NoSuchElementException");
        } catch (NoSuchElementException expected) {
            assertNotNull(expected);
        } catch (IOException ioException) {
            fail("Không mong đợi IOException");
        }
    }

    /**
     * UT_EM_035: Lấy lịch thi và map trạng thái hoàn thành
     * Mô tả: Kiểm tra map trạng thái missed/not started/completed/doing
     * Input: 4 examUsers tương ứng 4 trạng thái
     * Expected: isCompleted lần lượt = -2, 0, -1, 1
     */
    @Test
    public void UT_EM_035_getExamCalendar_shouldMapExamStatuses() {
        logger.info("[UT_EM_035] BẮT ĐẦU: getExamCalendar phải map đúng trạng thái bài thi");

        when(userService.getUserName()).thenReturn("student01");

        Exam missedExam = buildExamForCalendar(101L, new Date(System.currentTimeMillis() - 7200000),
                new Date(System.currentTimeMillis() - 3600000));
        Exam notYetExam = buildExamForCalendar(102L, new Date(System.currentTimeMillis() + 3600000),
                new Date(System.currentTimeMillis() + 7200000));
        Exam completedExam = buildExamForCalendar(103L, new Date(System.currentTimeMillis() - 3600000),
                new Date(System.currentTimeMillis() + 3600000));
        Exam doingExam = buildExamForCalendar(104L, new Date(System.currentTimeMillis() - 1800000),
                new Date(System.currentTimeMillis() + 1800000));

        ExamUser euMissed = new ExamUser();
        euMissed.setExam(missedExam);
        euMissed.setIsStarted(false);
        euMissed.setIsFinished(false);

        ExamUser euNotYet = new ExamUser();
        euNotYet.setExam(notYetExam);
        euNotYet.setIsStarted(false);
        euNotYet.setIsFinished(false);

        ExamUser euCompleted = new ExamUser();
        euCompleted.setExam(completedExam);
        euCompleted.setIsStarted(true);
        euCompleted.setIsFinished(true);

        ExamUser euDoing = new ExamUser();
        euDoing.setExam(doingExam);
        euDoing.setIsStarted(true);
        euDoing.setIsFinished(false);

        when(examUserService.getExamListByUsername("student01"))
                .thenReturn(Arrays.asList(euMissed, euNotYet, euCompleted, euDoing));

        List<ExamCalendar> calendars = examController.getExamCalendar();

        assertEquals(4, calendars.size());
        assertEquals(-2, calendars.get(0).getIsCompleted());
        assertEquals(0, calendars.get(1).getIsCompleted());
        assertEquals(-1, calendars.get(2).getIsCompleted());
        assertEquals(1, calendars.get(3).getIsCompleted());
    }

    /**
     * UT_EM_036: Tạo exam với questionData không hợp lệ phải rollback logic tạo dữ liệu phụ thuộc
     * Mô tả: Nếu questionData parse lỗi thì API phải trả lỗi và không được lưu exam/exam-user.
     * Input: questionData = "invalid-json"
     * Expected: Response 500, không gọi saveExam() và không gọi create(examUser)
     */
    @Test
    public void UT_EM_036_createExam_invalidQuestionData_shouldNotPersistAndReturn500() throws Exception {
        logger.info("[UT_EM_036] BẮT ĐẦU: createExam với questionData không hợp lệ");

        sampleExam.setQuestionData("invalid-json");
        when(userService.getUserName()).thenReturn("lecturer01");
        when(userService.getUserByUsername("lecturer01")).thenReturn(Optional.of(lecturerUser));
        when(intakeService.findById(1L)).thenReturn(Optional.of(sampleIntake));
        when(partService.findPartById(1L)).thenReturn(Optional.of(samplePart));
        when(userService.findAllByIntakeId(1L)).thenReturn(Arrays.asList(studentUser));

        ResponseEntity<?> response = examController.createExam(sampleExam, 1L, 1L, false, false);

        assertEquals("Status phải là 500 khi questionData lỗi", HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(examService, never()).saveExam(any(Exam.class));
        verify(examUserService, never()).create(any(Exam.class), anyList());
    }

    /**
     * UT_EM_037: Lấy câu hỏi khi không tồn tại exam-user cho user hiện tại
     * Mô tả: API cần xử lý an toàn khi user chưa được gán bài thi.
     * Input: exam tồn tại, examUser = null
     * Expected: Response 404, không ném NullPointerException
     */
    @Test
    public void UT_EM_037_getAllQuestions_missingExamUser_shouldReturn404() throws IOException {
        logger.info("[UT_EM_037] BẮT ĐẦU: getAllQuestions khi không tồn tại exam-user");

        sampleExam.setLocked(false);
        when(userService.getUserName()).thenReturn("student01");
        when(examService.getExamById(1L)).thenReturn(Optional.of(sampleExam));
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(null);

        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        assertEquals("Phải trả 404 khi thiếu exam-user", HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * UT_EM_038: Lưu đáp án khi isFinish=false không được set timeFinish
     * Mô tả: Chỉ khi nộp bài (isFinish=true) mới có thời gian kết thúc.
     * Input: isFinish=false
     * Expected: timeFinish giữ nguyên null sau khi update
     */
    @Test
    public void UT_EM_038_saveUserExamAnswer_notFinished_shouldNotSetTimeFinish() throws JsonProcessingException {
        logger.info("[UT_EM_038] BẮT ĐẦU: saveUserExamAnswer khi chưa nộp bài");

        setAuthentication("student01");
        sampleExamUser.setIsFinished(false);
        sampleExamUser.setTimeFinish(null);
        when(examUserService.findByExamAndUser(1L, "student01")).thenReturn(sampleExamUser);

        List<AnswerSheet> answerSheets = Arrays.asList(new AnswerSheet(1L,
                Arrays.asList(new Choice(1L, "int", 1)), 10));

        examController.saveUserExamAnswer(answerSheets, 1L, false, 1500);

        assertNull("Chưa nộp bài thì timeFinish phải null", sampleExamUser.getTimeFinish());
        assertFalse("isFinished phải là false", sampleExamUser.getIsFinished());
        assertEquals(1500, sampleExamUser.getRemainingTime());
        verify(examUserService, times(1)).update(sampleExamUser);
    }

    private void setAuthentication(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "password")
        );
    }

    private Exam buildExamForCalendar(Long id, Date begin, Date finish) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setTitle("Exam-" + id);
        exam.setDurationExam(60);
        exam.setBeginExam(begin);
        exam.setFinishExam(finish);
        exam.setPart(samplePart);
        return exam;
    }
}

