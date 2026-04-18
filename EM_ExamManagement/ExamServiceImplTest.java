package com.thanhtam.backend.exam;

import com.thanhtam.backend.dto.AnswerSheet;
import com.thanhtam.backend.dto.ChoiceCorrect;
import com.thanhtam.backend.dto.ChoiceList;
import com.thanhtam.backend.dto.ExamQuestionPoint;
import com.thanhtam.backend.entity.*;
import com.thanhtam.backend.repository.ExamRepository;
import com.thanhtam.backend.repository.IntakeRepository;
import com.thanhtam.backend.service.*;
import com.thanhtam.backend.ultilities.DifficultyLevel;
import com.thanhtam.backend.ultilities.EQTypeCode;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================================
 * Unit Test cho ExamServiceImpl - Tầng Service quản lý bài kiểm tra
 * ============================================================================
 * Mô tả: Test các phương thức CRUD và logic nghiệp vụ của ExamService
 * Phương pháp: Sử dụng Mockito để mock các dependency (Repository, Service)
 * Rollback: Sử dụng Mockito (không tương tác DB thật) nên không cần rollback DB.
 *           Mỗi test được reset mock objects qua @Before/@After.
 * ============================================================================
 */
@RunWith(MockitoJUnitRunner.class)
public class ExamServiceImplTest {

    private static final Logger logger = LoggerFactory.getLogger(ExamServiceImplTest.class);

    @Mock
    private ExamRepository examRepository;

    @Mock
    private IntakeRepository intakeRepository;

    @Mock
    private PartService partService;

    @Mock
    private UserService userService;

    @Mock
    private QuestionService questionService;

    @Mock
    private ChoiceService choiceService;

    @InjectMocks
    private ExamServiceImpl examService;

    // ============ Test Data ============
    private Exam sampleExam;
    private Intake sampleIntake;
    private Part samplePart;
    private Course sampleCourse;
    private User sampleUser;
    private Question sampleQuestionTF;
    private Question sampleQuestionMC;
    private Question sampleQuestionMS;
    private QuestionType questionTypeTF;
    private QuestionType questionTypeMC;
    private QuestionType questionTypeMS;

    /**
     * Khởi tạo dữ liệu test trước mỗi test case.
     * Đảm bảo mỗi test case có dữ liệu sạch, không bị ảnh hưởng bởi test khác.
     */
    @Before
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test...");

        // Course
        sampleCourse = new Course();
        sampleCourse.setId(1L);
        sampleCourse.setCourseCode("CS101");
        sampleCourse.setName("Lập trình Java");

        // Intake
        sampleIntake = new Intake();
        sampleIntake.setId(1L);
        sampleIntake.setName("Khóa 2024");
        sampleIntake.setIntakeCode("K2024");

        // Part
        samplePart = new Part();
        samplePart.setId(1L);
        samplePart.setName("Chương 1");
        samplePart.setCourse(sampleCourse);

        // User
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("lecturer01");
        sampleUser.setEmail("lecturer01@ptit.edu.vn");

        // Question Types
        questionTypeTF = new QuestionType();
        questionTypeTF.setId(1L);
        questionTypeTF.setTypeCode(EQTypeCode.TF);
        questionTypeTF.setDescription("True/False");

        questionTypeMC = new QuestionType();
        questionTypeMC.setId(2L);
        questionTypeMC.setTypeCode(EQTypeCode.MC);
        questionTypeMC.setDescription("Multiple Choice");

        questionTypeMS = new QuestionType();
        questionTypeMS.setId(3L);
        questionTypeMS.setTypeCode(EQTypeCode.MS);
        questionTypeMS.setDescription("Multiple Select");

        // Choices for TF question
        Choice choiceTF_True = new Choice();
        choiceTF_True.setId(1L);
        choiceTF_True.setChoiceText("True");
        choiceTF_True.setIsCorrected(1);

        // Questions
        sampleQuestionTF = new Question();
        sampleQuestionTF.setId(1L);
        sampleQuestionTF.setQuestionText("Java là ngôn ngữ hướng đối tượng?");
        sampleQuestionTF.setQuestionType(questionTypeTF);
        sampleQuestionTF.setDifficultyLevel(DifficultyLevel.EASY);
        sampleQuestionTF.setPoint(10);
        sampleQuestionTF.setChoices(Arrays.asList(choiceTF_True));

        sampleQuestionMC = new Question();
        sampleQuestionMC.setId(2L);
        sampleQuestionMC.setQuestionText("Đâu là kiểu dữ liệu nguyên thủy?");
        sampleQuestionMC.setQuestionType(questionTypeMC);
        sampleQuestionMC.setDifficultyLevel(DifficultyLevel.MEDIUM);
        sampleQuestionMC.setPoint(20);

        sampleQuestionMS = new Question();
        sampleQuestionMS.setId(3L);
        sampleQuestionMS.setQuestionText("Chọn các access modifier hợp lệ?");
        sampleQuestionMS.setQuestionType(questionTypeMS);
        sampleQuestionMS.setDifficultyLevel(DifficultyLevel.HARD);
        sampleQuestionMS.setPoint(30);

        // Exam
        sampleExam = new Exam();
        sampleExam.setId(1L);
        sampleExam.setTitle("Kiểm tra giữa kỳ - Lập trình Java");
        sampleExam.setIntake(sampleIntake);
        sampleExam.setPart(samplePart);
        sampleExam.setShuffle(false);
        sampleExam.setCanceled(false);
        sampleExam.setDurationExam(60);
        sampleExam.setBeginExam(new Date(System.currentTimeMillis() + 3600000)); // 1h sau
        sampleExam.setFinishExam(new Date(System.currentTimeMillis() + 7200000)); // 2h sau
        sampleExam.setQuestionData("[{\"questionId\":1,\"point\":10},{\"questionId\":2,\"point\":20}]");

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    /**
     * Dọn dẹp sau mỗi test case - reset tất cả mock objects.
     * Đảm bảo rollback trạng thái về ban đầu.
     */
    @After
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
        reset(examRepository, intakeRepository, partService, userService, questionService, choiceService);
        sampleExam = null;
        sampleIntake = null;
        samplePart = null;
        sampleCourse = null;
        sampleUser = null;
        sampleQuestionTF = null;
        sampleQuestionMC = null;
        sampleQuestionMS = null;
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES CHO saveExam()
    // ========================================================================================

    /**
     * TC_ES_001: Lưu bài kiểm tra hợp lệ thành công
     * Mô tả: Kiểm tra lưu một bài kiểm tra có đầy đủ thông tin hợp lệ
     * Input: Exam object với title, intake, part, duration, begin/finish date
     * Expected: Exam được lưu thành công và trả về đúng object
     */
    @Test
    public void TC_ES_001_saveExam_withValidData_shouldReturnSavedExam() {
        logger.info("[TC_ES_001] BẮT ĐẦU: Lưu bài kiểm tra hợp lệ thành công");
        logger.info("[TC_ES_001] Input: Exam(title='{}', duration={}, intake='{}', part='{}')",
                sampleExam.getTitle(), sampleExam.getDurationExam(),
                sampleExam.getIntake().getName(), sampleExam.getPart().getName());

        // Arrange
        when(examRepository.save(any(Exam.class))).thenReturn(sampleExam);

        // Act
        Exam result = examService.saveExam(sampleExam);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("ID phải khớp", sampleExam.getId(), result.getId());
        assertEquals("Title phải khớp", sampleExam.getTitle(), result.getTitle());
        assertEquals("Duration phải khớp", sampleExam.getDurationExam(), result.getDurationExam());
        assertFalse("Exam không bị hủy", result.isCanceled());

        verify(examRepository, times(1)).save(sampleExam);
        logger.info("[TC_ES_001] KẾT QUẢ: PASSED - Exam đã được lưu thành công với ID={}", result.getId());
    }

    /**
     * TC_ES_002: Lưu bài kiểm tra với title null
     * Mô tả: Kiểm tra hành vi khi lưu exam có title null
     * Input: Exam object với title = null
     * Expected: Exam vẫn được lưu (validation ở tầng controller)
     */
    @Test
    public void TC_ES_002_saveExam_withNullTitle_shouldStillSave() {
        logger.info("[TC_ES_002] BẮT ĐẦU: Lưu bài kiểm tra với title null");

        // Arrange
        sampleExam.setTitle(null);
        logger.info("[TC_ES_002] Input: Exam(title=null)");
        when(examRepository.save(any(Exam.class))).thenReturn(sampleExam);

        // Act
        Exam result = examService.saveExam(sampleExam);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertNull("Title phải là null", result.getTitle());
        verify(examRepository, times(1)).save(sampleExam);
        logger.info("[TC_ES_002] KẾT QUẢ: PASSED - Exam với title null đã được lưu");
    }

    /**
     * TC_ES_003: Lưu bài kiểm tra với chế độ xáo trộn câu hỏi
     * Mô tả: Kiểm tra lưu exam có shuffle = true
     * Input: Exam object với isShuffle = true
     * Expected: Exam được lưu với trạng thái shuffle = true
     */
    @Test
    public void TC_ES_003_saveExam_withShuffleEnabled_shouldSaveWithShuffle() {
        logger.info("[TC_ES_003] BẮT ĐẦU: Lưu bài kiểm tra với chế độ xáo trộn");

        // Arrange
        sampleExam.setShuffle(true);
        logger.info("[TC_ES_003] Input: Exam(isShuffle=true)");
        when(examRepository.save(any(Exam.class))).thenReturn(sampleExam);

        // Act
        Exam result = examService.saveExam(sampleExam);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("Shuffle phải là true", result.isShuffle());
        verify(examRepository, times(1)).save(sampleExam);
        logger.info("[TC_ES_003] KẾT QUẢ: PASSED - Exam shuffle=true đã được lưu");
    }

    // ========================================================================================
    // TEST CASES CHO findAll()
    // ========================================================================================

    /**
     * TC_ES_004: Lấy danh sách bài kiểm tra phân trang - có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exam phân trang khi có dữ liệu
     * Input: Pageable(page=0, size=10)
     * Expected: Page chứa danh sách exam, totalElements > 0
     */
    @Test
    public void TC_ES_004_findAll_withExistingData_shouldReturnPagedExams() {
        logger.info("[TC_ES_004] BẮT ĐẦU: Lấy danh sách bài kiểm tra phân trang - có dữ liệu");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Exam> exams = Arrays.asList(sampleExam);
        Page<Exam> examPage = new PageImpl<>(exams, pageable, 1);
        when(examRepository.findAll(pageable)).thenReturn(examPage);
        logger.info("[TC_ES_004] Input: Pageable(page=0, size=10)");

        // Act
        Page<Exam> result = examService.findAll(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 phần tử", 1, result.getTotalElements());
        assertEquals("Phải có 1 trang", 1, result.getTotalPages());
        assertEquals("Exam title phải khớp", sampleExam.getTitle(), result.getContent().get(0).getTitle());

        verify(examRepository, times(1)).findAll(pageable);
        logger.info("[TC_ES_004] KẾT QUẢ: PASSED - Trả về {} exam, {} trang",
                result.getTotalElements(), result.getTotalPages());
    }

    /**
     * TC_ES_005: Lấy danh sách bài kiểm tra phân trang - không có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exam phân trang khi DB rỗng
     * Input: Pageable(page=0, size=10)
     * Expected: Page rỗng, totalElements = 0
     */
    @Test
    public void TC_ES_005_findAll_withNoData_shouldReturnEmptyPage() {
        logger.info("[TC_ES_005] BẮT ĐẦU: Lấy danh sách bài kiểm tra phân trang - không có dữ liệu");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Exam> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(examRepository.findAll(pageable)).thenReturn(emptyPage);
        logger.info("[TC_ES_005] Input: Pageable(page=0, size=10), DB rỗng");

        // Act
        Page<Exam> result = examService.findAll(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 0 phần tử", 0, result.getTotalElements());
        assertTrue("Content phải rỗng", result.getContent().isEmpty());

        verify(examRepository, times(1)).findAll(pageable);
        logger.info("[TC_ES_005] KẾT QUẢ: PASSED - Page rỗng được trả về khi không có dữ liệu");
    }

    /**
     * TC_ES_006: Lấy danh sách bài kiểm tra - nhiều trang
     * Mô tả: Kiểm tra phân trang khi có nhiều exam hơn page size
     * Input: 3 exams, Pageable(page=0, size=2)
     * Expected: Page chứa 2 phần tử, totalElements = 3, totalPages = 2
     */
    @Test
    public void TC_ES_006_findAll_withMultiplePages_shouldReturnCorrectPagination() {
        logger.info("[TC_ES_006] BẮT ĐẦU: Lấy danh sách bài kiểm tra - nhiều trang");

        // Arrange
        Exam exam2 = new Exam();
        exam2.setId(2L);
        exam2.setTitle("Kiểm tra cuối kỳ");

        Pageable pageable = PageRequest.of(0, 2);
        List<Exam> exams = Arrays.asList(sampleExam, exam2);
        Page<Exam> examPage = new PageImpl<>(exams, pageable, 3);
        when(examRepository.findAll(pageable)).thenReturn(examPage);
        logger.info("[TC_ES_006] Input: 3 exams, Pageable(page=0, size=2)");

        // Act
        Page<Exam> result = examService.findAll(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 3 phần tử tổng", 3, result.getTotalElements());
        assertEquals("Content trang hiện tại phải có 2 phần tử", 2, result.getContent().size());
        assertEquals("Phải có 2 trang", 2, result.getTotalPages());

        logger.info("[TC_ES_006] KẾT QUẢ: PASSED - Phân trang đúng: {} elements, {} pages",
                result.getTotalElements(), result.getTotalPages());
    }

    // ========================================================================================
    // TEST CASES CHO cancelExam()
    // ========================================================================================

    /**
     * TC_ES_007: Hủy bài kiểm tra thành công
     * Mô tả: Kiểm tra hủy exam bằng ID hợp lệ
     * Input: examId = 1
     * Expected: Repository.cancelExam() được gọi đúng 1 lần
     */
    @Test
    public void TC_ES_007_cancelExam_withValidId_shouldCallRepository() {
        logger.info("[TC_ES_007] BẮT ĐẦU: Hủy bài kiểm tra thành công");
        logger.info("[TC_ES_007] Input: examId={}", 1L);

        // Arrange
        doNothing().when(examRepository).cancelExam(1L);

        // Act
        examService.cancelExam(1L);

        // Assert
        verify(examRepository, times(1)).cancelExam(1L);
        logger.info("[TC_ES_007] KẾT QUẢ: PASSED - cancelExam() đã được gọi đúng 1 lần với ID=1");
    }

    /**
     * TC_ES_008: Hủy bài kiểm tra với ID không tồn tại
     * Mô tả: Kiểm tra hành vi khi hủy exam với ID không có trong DB
     * Input: examId = 999
     * Expected: Repository.cancelExam() vẫn được gọi (không throw exception tại service)
     */
    @Test
    public void TC_ES_008_cancelExam_withNonExistingId_shouldStillCallRepository() {
        logger.info("[TC_ES_008] BẮT ĐẦU: Hủy bài kiểm tra với ID không tồn tại");
        logger.info("[TC_ES_008] Input: examId={}", 999L);

        // Arrange
        doNothing().when(examRepository).cancelExam(999L);

        // Act
        examService.cancelExam(999L);

        // Assert
        verify(examRepository, times(1)).cancelExam(999L);
        logger.info("[TC_ES_008] KẾT QUẢ: PASSED - cancelExam() được gọi mà không có exception");
    }

    // ========================================================================================
    // TEST CASES CHO getAll()
    // ========================================================================================

    /**
     * TC_ES_009: Lấy tất cả bài kiểm tra - có dữ liệu
     * Mô tả: Kiểm tra lấy toàn bộ danh sách exam không phân trang
     * Input: Không có
     * Expected: List chứa exam, size > 0
     */
    @Test
    public void TC_ES_009_getAll_withExistingData_shouldReturnAllExams() {
        logger.info("[TC_ES_009] BẮT ĐẦU: Lấy tất cả bài kiểm tra - có dữ liệu");

        // Arrange
        Exam exam2 = new Exam();
        exam2.setId(2L);
        exam2.setTitle("Kiểm tra 15 phút");
        List<Exam> exams = Arrays.asList(sampleExam, exam2);
        when(examRepository.findAll()).thenReturn(exams);

        // Act
        List<Exam> result = examService.getAll();

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 2 exam", 2, result.size());
        assertEquals("Exam đầu tiên phải khớp", sampleExam.getTitle(), result.get(0).getTitle());

        verify(examRepository, times(1)).findAll();
        logger.info("[TC_ES_009] KẾT QUẢ: PASSED - Trả về {} exams", result.size());
    }

    /**
     * TC_ES_010: Lấy tất cả bài kiểm tra - không có dữ liệu
     * Mô tả: Kiểm tra lấy toàn bộ danh sách exam khi DB rỗng
     * Input: Không có
     * Expected: List rỗng
     */
    @Test
    public void TC_ES_010_getAll_withNoData_shouldReturnEmptyList() {
        logger.info("[TC_ES_010] BẮT ĐẦU: Lấy tất cả bài kiểm tra - không có dữ liệu");

        // Arrange
        when(examRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Exam> result = examService.getAll();

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        verify(examRepository, times(1)).findAll();
        logger.info("[TC_ES_010] KẾT QUẢ: PASSED - List rỗng được trả về");
    }

    // ========================================================================================
    // TEST CASES CHO getExamById()
    // ========================================================================================

    /**
     * TC_ES_011: Lấy bài kiểm tra theo ID - tồn tại
     * Mô tả: Kiểm tra tìm exam bằng ID hợp lệ, exam tồn tại trong DB
     * Input: examId = 1
     * Expected: Optional chứa Exam với ID = 1
     */
    @Test
    public void TC_ES_011_getExamById_withExistingId_shouldReturnExam() {
        logger.info("[TC_ES_011] BẮT ĐẦU: Lấy bài kiểm tra theo ID - tồn tại");
        logger.info("[TC_ES_011] Input: examId={}", 1L);

        // Arrange
        when(examRepository.findById(1L)).thenReturn(Optional.of(sampleExam));

        // Act
        Optional<Exam> result = examService.getExamById(1L);

        // Assert
        assertTrue("Kết quả phải có giá trị", result.isPresent());
        assertEquals("ID phải khớp", Long.valueOf(1L), result.get().getId());
        assertEquals("Title phải khớp", sampleExam.getTitle(), result.get().getTitle());

        verify(examRepository, times(1)).findById(1L);
        logger.info("[TC_ES_011] KẾT QUẢ: PASSED - Tìm thấy exam ID={}, title='{}'",
                result.get().getId(), result.get().getTitle());
    }

    /**
     * TC_ES_012: Lấy bài kiểm tra theo ID - không tồn tại
     * Mô tả: Kiểm tra tìm exam bằng ID không tồn tại trong DB
     * Input: examId = 999
     * Expected: Optional.empty()
     */
    @Test
    public void TC_ES_012_getExamById_withNonExistingId_shouldReturnEmpty() {
        logger.info("[TC_ES_012] BẮT ĐẦU: Lấy bài kiểm tra theo ID - không tồn tại");
        logger.info("[TC_ES_012] Input: examId={}", 999L);

        // Arrange
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Exam> result = examService.getExamById(999L);

        // Assert
        assertFalse("Kết quả phải rỗng", result.isPresent());

        verify(examRepository, times(1)).findById(999L);
        logger.info("[TC_ES_012] KẾT QUẢ: PASSED - Không tìm thấy exam với ID=999");
    }

    // ========================================================================================
    // TEST CASES CHO findAllByCreatedBy_Username()
    // ========================================================================================

    /**
     * TC_ES_013: Lấy exam theo username người tạo - có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exams theo username người tạo
     * Input: username = "lecturer01", Pageable(page=0, size=10)
     * Expected: Page chứa exams tạo bởi lecturer01
     */
    @Test
    public void TC_ES_013_findAllByCreatedBy_Username_withExistingData_shouldReturnExams() {
        logger.info("[TC_ES_013] BẮT ĐẦU: Lấy exam theo username người tạo - có dữ liệu");
        String username = "lecturer01";
        Pageable pageable = PageRequest.of(0, 10);
        logger.info("[TC_ES_013] Input: username='{}', Pageable(page=0, size=10)", username);

        // Arrange
        List<Exam> exams = Arrays.asList(sampleExam);
        Page<Exam> examPage = new PageImpl<>(exams, pageable, 1);
        when(examRepository.findAllByCreatedBy_Username(pageable, username)).thenReturn(examPage);

        // Act
        Page<Exam> result = examService.findAllByCreatedBy_Username(pageable, username);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 exam", 1, result.getTotalElements());

        verify(examRepository, times(1)).findAllByCreatedBy_Username(pageable, username);
        logger.info("[TC_ES_013] KẾT QUẢ: PASSED - Tìm thấy {} exams cho user '{}'",
                result.getTotalElements(), username);
    }

    /**
     * TC_ES_014: Lấy exam theo username - không có exam
     * Mô tả: Kiểm tra khi user không tạo exam nào
     * Input: username = "newlecturer", Pageable(page=0, size=10)
     * Expected: Page rỗng
     */
    @Test
    public void TC_ES_014_findAllByCreatedBy_Username_withNoExams_shouldReturnEmptyPage() {
        logger.info("[TC_ES_014] BẮT ĐẦU: Lấy exam theo username - không có exam");
        String username = "newlecturer";
        Pageable pageable = PageRequest.of(0, 10);
        logger.info("[TC_ES_014] Input: username='{}', Pageable(page=0, size=10)", username);

        // Arrange
        Page<Exam> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(examRepository.findAllByCreatedBy_Username(pageable, username)).thenReturn(emptyPage);

        // Act
        Page<Exam> result = examService.findAllByCreatedBy_Username(pageable, username);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 0 exams", 0, result.getTotalElements());
        assertTrue("Content phải rỗng", result.getContent().isEmpty());

        logger.info("[TC_ES_014] KẾT QUẢ: PASSED - Không có exam nào cho user '{}'", username);
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - TRUE/FALSE
    // ========================================================================================

    /**
     * TC_ES_015: Chấm bài kiểm tra - câu hỏi True/False - trả lời đúng
     * Mô tả: Kiểm tra logic chấm điểm khi user trả lời đúng câu True/False
     * Input: UserChoice với choiceText = "True", đáp án đúng là "True"
     * Expected: isSelectedCorrected = true
     */
    @Test
    public void TC_ES_015_getChoiceList_TFQuestion_correctAnswer_shouldMarkCorrect() {
        logger.info("[TC_ES_015] BẮT ĐẦU: Chấm bài - TF question - trả lời đúng");

        // Arrange
        Choice userChoice = new Choice();
        userChoice.setId(1L);
        userChoice.setChoiceText("True");
        userChoice.setIsCorrected(1);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(1L);
        answerSheet.setChoices(Arrays.asList(userChoice));
        answerSheet.setPoint(10);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(1L);
        eqp.setPoint(10);

        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(sampleQuestionTF));
        when(choiceService.findChoiceTextById(1L)).thenReturn("True");
        logger.info("[TC_ES_015] Input: TF Question, user chọn 'True', đáp án đúng 'True'");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 choice list", 1, result.size());
        assertTrue("Câu trả lời phải đúng", result.get(0).getIsSelectedCorrected());

        logger.info("[TC_ES_015] KẾT QUẢ: PASSED - Câu TF trả lời đúng, isSelectedCorrected=true");
    }

    /**
     * TC_ES_016: Chấm bài kiểm tra - câu hỏi True/False - trả lời sai
     * Mô tả: Kiểm tra logic chấm điểm khi user trả lời sai câu True/False
     * Input: UserChoice với choiceText = "False", đáp án đúng là "True"
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void TC_ES_016_getChoiceList_TFQuestion_wrongAnswer_shouldMarkIncorrect() {
        logger.info("[TC_ES_016] BẮT ĐẦU: Chấm bài - TF question - trả lời sai");

        // Arrange
        Choice userChoice = new Choice();
        userChoice.setId(1L);
        userChoice.setChoiceText("False");
        userChoice.setIsCorrected(0);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(1L);
        answerSheet.setChoices(Arrays.asList(userChoice));
        answerSheet.setPoint(10);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(1L);
        eqp.setPoint(10);

        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(sampleQuestionTF));
        when(choiceService.findChoiceTextById(1L)).thenReturn("True");
        logger.info("[TC_ES_016] Input: TF Question, user chọn 'False', đáp án đúng 'True'");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Câu trả lời phải sai", result.get(0).getIsSelectedCorrected());

        logger.info("[TC_ES_016] KẾT QUẢ: PASSED - Câu TF trả lời sai, isSelectedCorrected=false");
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - MULTIPLE CHOICE
    // ========================================================================================

    /**
     * TC_ES_017: Chấm bài - câu hỏi Multiple Choice - trả lời đúng
     * Mô tả: Kiểm tra logic chấm điểm câu MC khi user chọn đúng đáp án
     * Input: UserChoice với isCorrected=1, đáp án thực tế isRealCorrect=1
     * Expected: isSelectedCorrected = true
     */
    @Test
    public void TC_ES_017_getChoiceList_MCQuestion_correctAnswer_shouldMarkCorrect() {
        logger.info("[TC_ES_017] BẮT ĐẦU: Chấm bài - MC question - trả lời đúng");

        // Arrange
        Choice choiceA = new Choice(1L, "int", 1);
        Choice choiceB = new Choice(2L, "String", 0);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(2L);
        answerSheet.setChoices(Arrays.asList(choiceA, choiceB));
        answerSheet.setPoint(20);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(2L);
        eqp.setPoint(20);

        when(questionService.getQuestionById(2L)).thenReturn(Optional.of(sampleQuestionMC));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(1);
        when(choiceService.findIsCorrectedById(2L)).thenReturn(0);
        logger.info("[TC_ES_017] Input: MC Question, user chọn 'int'(đúng), 'String'(sai)");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("Câu trả lời MC phải đúng", result.get(0).getIsSelectedCorrected());

        logger.info("[TC_ES_017] KẾT QUẢ: PASSED - Câu MC trả lời đúng");
    }

    /**
     * TC_ES_018: Chấm bài - câu hỏi Multiple Choice - trả lời sai
     * Mô tả: Kiểm tra logic chấm điểm câu MC khi user chọn sai đáp án
     * Input: UserChoice chọn đáp án sai (isCorrected=1 nhưng realCorrect=0)
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void TC_ES_018_getChoiceList_MCQuestion_wrongAnswer_shouldMarkIncorrect() {
        logger.info("[TC_ES_018] BẮT ĐẦU: Chấm bài - MC question - trả lời sai");

        // Arrange
        Choice choiceA = new Choice(1L, "int", 0);
        Choice choiceB = new Choice(2L, "String", 0);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(2L);
        answerSheet.setChoices(Arrays.asList(choiceA, choiceB));
        answerSheet.setPoint(20);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(2L);
        eqp.setPoint(20);

        when(questionService.getQuestionById(2L)).thenReturn(Optional.of(sampleQuestionMC));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(0);
        when(choiceService.findIsCorrectedById(2L)).thenReturn(1); // đáp án đúng là B nhưng user không chọn
        logger.info("[TC_ES_018] Input: MC Question, user không chọn đáp án đúng");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Câu trả lời MC phải sai", result.get(0).getIsSelectedCorrected());

        logger.info("[TC_ES_018] KẾT QUẢ: PASSED - Câu MC trả lời sai");
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - MULTIPLE SELECT
    // ========================================================================================

    /**
     * TC_ES_019: Chấm bài - câu hỏi Multiple Select - trả lời đúng hết
     * Mô tả: Kiểm tra chấm điểm câu MS khi user chọn đúng tất cả đáp án
     * Input: User chọn tất cả đáp án đúng, không chọn đáp án sai
     * Expected: isSelectedCorrected = true
     */
    @Test
    public void TC_ES_019_getChoiceList_MSQuestion_allCorrect_shouldMarkCorrect() {
        logger.info("[TC_ES_019] BẮT ĐẦU: Chấm bài - MS question - trả lời đúng hết");

        // Arrange - user chọn đúng cả 2 đáp án: public(1), private(1)
        Choice choiceA = new Choice(1L, "public", 1);
        Choice choiceB = new Choice(2L, "private", 1);
        Choice choiceC = new Choice(3L, "internal", 0);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(Arrays.asList(choiceA, choiceB, choiceC));
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(1);
        when(choiceService.findIsCorrectedById(2L)).thenReturn(1);
        when(choiceService.findIsCorrectedById(3L)).thenReturn(0);
        logger.info("[TC_ES_019] Input: MS Question, user chọn đúng 'public', 'private'");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("Câu trả lời MS phải đúng khi chọn đúng tất cả", result.get(0).getIsSelectedCorrected());

        logger.info("[TC_ES_019] KẾT QUẢ: PASSED - Câu MS đúng tất cả");
    }

    /**
     * TC_ES_020: Chấm bài - câu hỏi Multiple Select - bỏ sót đáp án đúng
     * Mô tả: Kiểm tra chấm điểm câu MS khi user bỏ sót 1 đáp án đúng
     * Input: User chọn 1 đáp án đúng, bỏ sót 1 đáp án đúng khác
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void TC_ES_020_getChoiceList_MSQuestion_missedCorrectAnswer_shouldMarkIncorrect() {
        logger.info("[TC_ES_020] BẮT ĐẦU: Chấm bài - MS question - bỏ sót đáp án đúng");

        // Arrange - user chỉ chọn public(1) nhưng bỏ sót private(0, thực tế là đúng)
        Choice choiceA = new Choice(1L, "public", 1);
        Choice choiceB = new Choice(2L, "private", 0);  // user không chọn, nhưng đáp án đúng

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(Arrays.asList(choiceA, choiceB));
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(1);
        when(choiceService.findIsCorrectedById(2L)).thenReturn(1); // đáp án đúng nhưng user không tick
        logger.info("[TC_ES_020] Input: MS Question, user bỏ sót 'private'(đúng)");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Câu trả lời MS phải sai khi bỏ sót đáp án", result.get(0).getIsSelectedCorrected());

        logger.info("[TC_ES_020] KẾT QUẢ: PASSED - Câu MS sai do bỏ sót đáp án");
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - EDGE CASES
    // ========================================================================================

    /**
     * TC_ES_021: Chấm bài với danh sách câu trả lời rỗng
     * Mô tả: Kiểm tra khi user không trả lời câu nào
     * Input: userChoices = empty list
     * Expected: choiceLists rỗng
     */
    @Test
    public void TC_ES_021_getChoiceList_emptyAnswerSheet_shouldReturnEmptyList() {
        logger.info("[TC_ES_021] BẮT ĐẦU: Chấm bài với danh sách câu trả lời rỗng");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Collections.emptyList(), Collections.emptyList());

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[TC_ES_021] KẾT QUẢ: PASSED - Trả về list rỗng khi không có câu trả lời");
    }

    /**
     * TC_ES_022: Chấm bài với nhiều câu hỏi hỗn hợp (TF + MC + MS)
     * Mô tả: Kiểm tra chấm điểm khi bài thi có cả 3 loại câu hỏi
     * Input: 1 câu TF (đúng), 1 câu MC (sai), 1 câu MS (đúng)
     * Expected: 3 ChoiceList, correct = [true, false, true]
     */
    @Test
    public void TC_ES_022_getChoiceList_mixedQuestionTypes_shouldGradeCorrectly() {
        logger.info("[TC_ES_022] BẮT ĐẦU: Chấm bài hỗn hợp (TF + MC + MS)");

        // ---- TF: đúng ----
        Choice tfChoice = new Choice(1L, "True", 1);
        AnswerSheet asTF = new AnswerSheet(1L, Arrays.asList(tfChoice), 10);

        // ---- MC: sai ----
        Choice mcChoice = new Choice(2L, "Wrong", 0);
        AnswerSheet asMC = new AnswerSheet(2L, Arrays.asList(mcChoice), 20);

        // ---- MS: đúng ----
        Choice msChoice1 = new Choice(3L, "public", 1);
        Choice msChoice2 = new Choice(4L, "private", 1);
        AnswerSheet asMS = new AnswerSheet(3L, Arrays.asList(msChoice1, msChoice2), 30);

        ExamQuestionPoint eqp1 = new ExamQuestionPoint();
        eqp1.setQuestionId(1L);
        eqp1.setPoint(10);
        ExamQuestionPoint eqp2 = new ExamQuestionPoint();
        eqp2.setQuestionId(2L);
        eqp2.setPoint(20);
        ExamQuestionPoint eqp3 = new ExamQuestionPoint();
        eqp3.setQuestionId(3L);
        eqp3.setPoint(30);

        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(sampleQuestionTF));
        when(questionService.getQuestionById(2L)).thenReturn(Optional.of(sampleQuestionMC));
        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));
        when(choiceService.findChoiceTextById(1L)).thenReturn("True");
        when(choiceService.findIsCorrectedById(2L)).thenReturn(1); // đáp án đúng nhưng user chọn sai
        when(choiceService.findIsCorrectedById(3L)).thenReturn(1);
        when(choiceService.findIsCorrectedById(4L)).thenReturn(1);

        logger.info("[TC_ES_022] Input: TF(đúng) + MC(sai) + MS(đúng)");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(asTF, asMC, asMS),
                Arrays.asList(eqp1, eqp2, eqp3));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 3 choice lists", 3, result.size());
        assertTrue("Câu TF phải đúng", result.get(0).getIsSelectedCorrected());
        assertFalse("Câu MC phải sai", result.get(1).getIsSelectedCorrected());
        assertTrue("Câu MS phải đúng", result.get(2).getIsSelectedCorrected());

        logger.info("[TC_ES_022] KẾT QUẢ: PASSED - TF=đúng, MC=sai, MS=đúng");
    }
}
