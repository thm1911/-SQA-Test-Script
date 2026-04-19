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
     * UT_EM_039: Lưu bài kiểm tra hợp lệ thành công
     * Mô tả: Kiểm tra lưu một bài kiểm tra có đầy đủ thông tin hợp lệ
     * Input: Exam object với title, intake, part, duration, begin/finish date
     * Expected: Exam được lưu thành công và trả về đúng object
     */
    @Test
    public void UT_EM_039_saveExam_withValidData_shouldReturnSavedExam() {
        logger.info("[UT_EM_039] BẮT ĐẦU: Lưu bài kiểm tra hợp lệ thành công");
        logger.info("[UT_EM_039] Input: Exam(title='{}', duration={}, intake='{}', part='{}')",
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
        logger.info("[UT_EM_039] KẾT QUẢ: PASSED - Exam đã được lưu thành công với ID={}", result.getId());
    }

    /**
     * UT_EM_040: Lưu bài kiểm tra với title null
     * Mô tả: Kiểm tra hành vi khi lưu exam có title null
     * Input: Exam object với title = null
     * Expected: Exam vẫn được lưu (validation ở tầng controller)
     */
    @Test
    public void UT_EM_040_saveExam_withNullTitle_shouldStillSave() {
        logger.info("[UT_EM_040] BẮT ĐẦU: Lưu bài kiểm tra với title null");

        // Arrange
        sampleExam.setTitle(null);
        logger.info("[UT_EM_040] Input: Exam(title=null)");
        when(examRepository.save(any(Exam.class))).thenReturn(sampleExam);

        // Act
        Exam result = examService.saveExam(sampleExam);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertNull("Title phải là null", result.getTitle());
        verify(examRepository, times(1)).save(sampleExam);
        logger.info("[UT_EM_040] KẾT QUẢ: PASSED - Exam với title null đã được lưu");
    }

    /**
     * UT_EM_041: Lưu bài kiểm tra với chế độ xáo trộn câu hỏi
     * Mô tả: Kiểm tra lưu exam có shuffle = true
     * Input: Exam object với isShuffle = true
     * Expected: Exam được lưu với trạng thái shuffle = true
     */
    @Test
    public void UT_EM_041_saveExam_withShuffleEnabled_shouldSaveWithShuffle() {
        logger.info("[UT_EM_041] BẮT ĐẦU: Lưu bài kiểm tra với chế độ xáo trộn");

        // Arrange
        sampleExam.setShuffle(true);
        logger.info("[UT_EM_041] Input: Exam(isShuffle=true)");
        when(examRepository.save(any(Exam.class))).thenReturn(sampleExam);

        // Act
        Exam result = examService.saveExam(sampleExam);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("Shuffle phải là true", result.isShuffle());
        verify(examRepository, times(1)).save(sampleExam);
        logger.info("[UT_EM_041] KẾT QUẢ: PASSED - Exam shuffle=true đã được lưu");
    }

    // ========================================================================================
    // TEST CASES CHO findAll()
    // ========================================================================================

    /**
     * UT_EM_042: Lấy danh sách bài kiểm tra phân trang - có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exam phân trang khi có dữ liệu
     * Input: Pageable(page=0, size=10)
     * Expected: Page chứa danh sách exam, totalElements > 0
     */
    @Test
    public void UT_EM_042_findAll_withExistingData_shouldReturnPagedExams() {
        logger.info("[UT_EM_042] BẮT ĐẦU: Lấy danh sách bài kiểm tra phân trang - có dữ liệu");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Exam> exams = Arrays.asList(sampleExam);
        Page<Exam> examPage = new PageImpl<>(exams, pageable, 1);
        when(examRepository.findAll(pageable)).thenReturn(examPage);
        logger.info("[UT_EM_042] Input: Pageable(page=0, size=10)");

        // Act
        Page<Exam> result = examService.findAll(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 phần tử", 1, result.getTotalElements());
        assertEquals("Phải có 1 trang", 1, result.getTotalPages());
        assertEquals("Exam title phải khớp", sampleExam.getTitle(), result.getContent().get(0).getTitle());

        verify(examRepository, times(1)).findAll(pageable);
        logger.info("[UT_EM_042] KẾT QUẢ: PASSED - Trả về {} exam, {} trang",
                result.getTotalElements(), result.getTotalPages());
    }

    /**
     * UT_EM_043: Lấy danh sách bài kiểm tra phân trang - không có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exam phân trang khi DB rỗng
     * Input: Pageable(page=0, size=10)
     * Expected: Page rỗng, totalElements = 0
     */
    @Test
    public void UT_EM_043_findAll_withNoData_shouldReturnEmptyPage() {
        logger.info("[UT_EM_043] BẮT ĐẦU: Lấy danh sách bài kiểm tra phân trang - không có dữ liệu");

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Exam> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(examRepository.findAll(pageable)).thenReturn(emptyPage);
        logger.info("[UT_EM_043] Input: Pageable(page=0, size=10), DB rỗng");

        // Act
        Page<Exam> result = examService.findAll(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 0 phần tử", 0, result.getTotalElements());
        assertTrue("Content phải rỗng", result.getContent().isEmpty());

        verify(examRepository, times(1)).findAll(pageable);
        logger.info("[UT_EM_043] KẾT QUẢ: PASSED - Page rỗng được trả về khi không có dữ liệu");
    }

    /**
     * UT_EM_044: Lấy danh sách bài kiểm tra - nhiều trang
     * Mô tả: Kiểm tra phân trang khi có nhiều exam hơn page size
     * Input: 3 exams, Pageable(page=0, size=2)
     * Expected: Page chứa 2 phần tử, totalElements = 3, totalPages = 2
     */
    @Test
    public void UT_EM_044_findAll_withMultiplePages_shouldReturnCorrectPagination() {
        logger.info("[UT_EM_044] BẮT ĐẦU: Lấy danh sách bài kiểm tra - nhiều trang");

        // Arrange
        Exam exam2 = new Exam();
        exam2.setId(2L);
        exam2.setTitle("Kiểm tra cuối kỳ");

        Pageable pageable = PageRequest.of(0, 2);
        List<Exam> exams = Arrays.asList(sampleExam, exam2);
        Page<Exam> examPage = new PageImpl<>(exams, pageable, 3);
        when(examRepository.findAll(pageable)).thenReturn(examPage);
        logger.info("[UT_EM_044] Input: 3 exams, Pageable(page=0, size=2)");

        // Act
        Page<Exam> result = examService.findAll(pageable);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 3 phần tử tổng", 3, result.getTotalElements());
        assertEquals("Content trang hiện tại phải có 2 phần tử", 2, result.getContent().size());
        assertEquals("Phải có 2 trang", 2, result.getTotalPages());

        logger.info("[UT_EM_044] KẾT QUẢ: PASSED - Phân trang đúng: {} elements, {} pages",
                result.getTotalElements(), result.getTotalPages());
    }

    // ========================================================================================
    // TEST CASES CHO cancelExam()
    // ========================================================================================

    /**
     * UT_EM_045: Hủy bài kiểm tra thành công
     * Mô tả: Kiểm tra hủy exam bằng ID hợp lệ
     * Input: examId = 1
     * Expected: Repository.cancelExam() được gọi đúng 1 lần
     */
    @Test
    public void UT_EM_045_cancelExam_withValidId_shouldCallRepository() {
        logger.info("[UT_EM_045] BẮT ĐẦU: Hủy bài kiểm tra thành công");
        logger.info("[UT_EM_045] Input: examId={}", 1L);

        // Arrange
        doNothing().when(examRepository).cancelExam(1L);

        // Act
        examService.cancelExam(1L);

        // Assert
        verify(examRepository, times(1)).cancelExam(1L);
        logger.info("[UT_EM_045] KẾT QUẢ: PASSED - cancelExam() đã được gọi đúng 1 lần với ID=1");
    }

    /**
     * UT_EM_046: Hủy bài kiểm tra với ID không tồn tại
     * Mô tả: Kiểm tra hành vi khi hủy exam với ID không có trong DB
     * Input: examId = 999
     * Expected: Service phải phát hiện không tồn tại và ném lỗi
     */
    @Test
    public void UT_EM_046_cancelExam_withNonExistingId_shouldThrowAndNotCallRepositoryCancel() {
        logger.info("[UT_EM_046] BẮT ĐẦU: Hủy bài kiểm tra với ID không tồn tại");
        logger.info("[UT_EM_046] Input: examId={}", 999L);

        // Arrange
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        try {
            examService.cancelExam(999L);
            fail("Phải ném NoSuchElementException khi examId không tồn tại");
        } catch (NoSuchElementException expected) {
            assertNotNull(expected);
        }

        verify(examRepository, never()).cancelExam(anyLong());
        logger.info("[UT_EM_046] KẾT QUẢ: PASSED - Service chặn ID không tồn tại trước khi gọi repository.cancelExam()");
    }

    // ========================================================================================
    // TEST CASES CHO getAll()
    // ========================================================================================

    /**
     * UT_EM_047: Lấy tất cả bài kiểm tra - có dữ liệu
     * Mô tả: Kiểm tra lấy toàn bộ danh sách exam không phân trang
     * Input: Không có
     * Expected: List chứa exam, size > 0
     */
    @Test
    public void UT_EM_047_getAll_withExistingData_shouldReturnAllExams() {
        logger.info("[UT_EM_047] BẮT ĐẦU: Lấy tất cả bài kiểm tra - có dữ liệu");

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
        logger.info("[UT_EM_047] KẾT QUẢ: PASSED - Trả về {} exams", result.size());
    }

    /**
     * UT_EM_048: Lấy tất cả bài kiểm tra - không có dữ liệu
     * Mô tả: Kiểm tra lấy toàn bộ danh sách exam khi DB rỗng
     * Input: Không có
     * Expected: List rỗng
     */
    @Test
    public void UT_EM_048_getAll_withNoData_shouldReturnEmptyList() {
        logger.info("[UT_EM_048] BẮT ĐẦU: Lấy tất cả bài kiểm tra - không có dữ liệu");

        // Arrange
        when(examRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Exam> result = examService.getAll();

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        verify(examRepository, times(1)).findAll();
        logger.info("[UT_EM_048] KẾT QUẢ: PASSED - List rỗng được trả về");
    }

    // ========================================================================================
    // TEST CASES CHO getExamById()
    // ========================================================================================

    /**
     * UT_EM_049: Lấy bài kiểm tra theo ID - tồn tại
     * Mô tả: Kiểm tra tìm exam bằng ID hợp lệ, exam tồn tại trong DB
     * Input: examId = 1
     * Expected: Optional chứa Exam với ID = 1
     */
    @Test
    public void UT_EM_049_getExamById_withExistingId_shouldReturnExam() {
        logger.info("[UT_EM_049] BẮT ĐẦU: Lấy bài kiểm tra theo ID - tồn tại");
        logger.info("[UT_EM_049] Input: examId={}", 1L);

        // Arrange
        when(examRepository.findById(1L)).thenReturn(Optional.of(sampleExam));

        // Act
        Optional<Exam> result = examService.getExamById(1L);

        // Assert
        assertTrue("Kết quả phải có giá trị", result.isPresent());
        assertEquals("ID phải khớp", Long.valueOf(1L), result.get().getId());
        assertEquals("Title phải khớp", sampleExam.getTitle(), result.get().getTitle());

        verify(examRepository, times(1)).findById(1L);
        logger.info("[UT_EM_049] KẾT QUẢ: PASSED - Tìm thấy exam ID={}, title='{}'",
                result.get().getId(), result.get().getTitle());
    }

    /**
     * UT_EM_050: Lấy bài kiểm tra theo ID - không tồn tại
     * Mô tả: Kiểm tra tìm exam bằng ID không tồn tại trong DB
     * Input: examId = 999
     * Expected: Optional.empty()
     */
    @Test
    public void UT_EM_050_getExamById_withNonExistingId_shouldReturnEmpty() {
        logger.info("[UT_EM_050] BẮT ĐẦU: Lấy bài kiểm tra theo ID - không tồn tại");
        logger.info("[UT_EM_050] Input: examId={}", 999L);

        // Arrange
        when(examRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Exam> result = examService.getExamById(999L);

        // Assert
        assertFalse("Kết quả phải rỗng", result.isPresent());

        verify(examRepository, times(1)).findById(999L);
        logger.info("[UT_EM_050] KẾT QUẢ: PASSED - Không tìm thấy exam với ID=999");
    }

    // ========================================================================================
    // TEST CASES CHO findAllByCreatedBy_Username()
    // ========================================================================================

    /**
     * UT_EM_051: Lấy exam theo username người tạo - có dữ liệu
     * Mô tả: Kiểm tra lấy danh sách exams theo username người tạo
     * Input: username = "lecturer01", Pageable(page=0, size=10)
     * Expected: Page chứa exams tạo bởi lecturer01
     */
    @Test
    public void UT_EM_051_findAllByCreatedBy_Username_withExistingData_shouldReturnExams() {
        logger.info("[UT_EM_051] BẮT ĐẦU: Lấy exam theo username người tạo - có dữ liệu");
        String username = "lecturer01";
        Pageable pageable = PageRequest.of(0, 10);
        logger.info("[UT_EM_051] Input: username='{}', Pageable(page=0, size=10)", username);

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
        logger.info("[UT_EM_051] KẾT QUẢ: PASSED - Tìm thấy {} exams cho user '{}'",
                result.getTotalElements(), username);
    }

    /**
     * UT_EM_052: Lấy exam theo username - không có exam
     * Mô tả: Kiểm tra khi user không tạo exam nào
     * Input: username = "newlecturer", Pageable(page=0, size=10)
     * Expected: Page rỗng
     */
    @Test
    public void UT_EM_052_findAllByCreatedBy_Username_withNoExams_shouldReturnEmptyPage() {
        logger.info("[UT_EM_052] BẮT ĐẦU: Lấy exam theo username - không có exam");
        String username = "newlecturer";
        Pageable pageable = PageRequest.of(0, 10);
        logger.info("[UT_EM_052] Input: username='{}', Pageable(page=0, size=10)", username);

        // Arrange
        Page<Exam> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(examRepository.findAllByCreatedBy_Username(pageable, username)).thenReturn(emptyPage);

        // Act
        Page<Exam> result = examService.findAllByCreatedBy_Username(pageable, username);

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 0 exams", 0, result.getTotalElements());
        assertTrue("Content phải rỗng", result.getContent().isEmpty());

        logger.info("[UT_EM_052] KẾT QUẢ: PASSED - Không có exam nào cho user '{}'", username);
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - TRUE/FALSE
    // ========================================================================================

    /**
     * UT_EM_053: Chấm bài kiểm tra - câu hỏi True/False - trả lời đúng
     * Mô tả: Kiểm tra logic chấm điểm khi user trả lời đúng câu True/False
     * Input: UserChoice với choiceText = "True", đáp án đúng là "True"
     * Expected: isSelectedCorrected = true
     */
    @Test
    public void UT_EM_053_getChoiceList_TFQuestion_correctAnswer_shouldMarkCorrect() {
        logger.info("[UT_EM_053] BẮT ĐẦU: Chấm bài - TF question - trả lời đúng");

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
        logger.info("[UT_EM_053] Input: TF Question, user chọn 'True', đáp án đúng 'True'");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertEquals("Phải có 1 choice list", 1, result.size());
        assertTrue("Câu trả lời phải đúng", result.get(0).getIsSelectedCorrected());

        logger.info("[UT_EM_053] KẾT QUẢ: PASSED - Câu TF trả lời đúng, isSelectedCorrected=true");
    }

    /**
     * UT_EM_054: Chấm bài kiểm tra - câu hỏi True/False - trả lời sai
     * Mô tả: Kiểm tra logic chấm điểm khi user trả lời sai câu True/False
     * Input: UserChoice với choiceText = "False", đáp án đúng là "True"
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_054_getChoiceList_TFQuestion_wrongAnswer_shouldMarkIncorrect() {
        logger.info("[UT_EM_054] BẮT ĐẦU: Chấm bài - TF question - trả lời sai");

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
        logger.info("[UT_EM_054] Input: TF Question, user chọn 'False', đáp án đúng 'True'");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Câu trả lời phải sai", result.get(0).getIsSelectedCorrected());

        logger.info("[UT_EM_054] KẾT QUẢ: PASSED - Câu TF trả lời sai, isSelectedCorrected=false");
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - MULTIPLE CHOICE
    // ========================================================================================

    /**
     * UT_EM_055: Chấm bài - câu hỏi Multiple Choice - chọn cả đúng và sai
     * Mô tả: Theo chuẩn MC, chỉ được chọn 1 đáp án đúng duy nhất.
     *        Nếu chọn thêm đáp án sai thì kết quả phải sai.
     * Input: User chọn cả đáp án đúng và đáp án sai
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_055_getChoiceList_MCQuestion_selectCorrectAndWrong_shouldMarkIncorrect() {
        logger.info("[UT_EM_055] BẮT ĐẦU: Chấm bài - MC question - chọn cả đúng và sai");

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
        logger.info("[UT_EM_055] Input: MC Question, user chọn cả 'int'(đúng) và 'String'(sai)");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Theo chuẩn MC, chọn dư đáp án sai phải bị chấm sai", result.get(0).getIsSelectedCorrected());

        logger.info("[UT_EM_055] KẾT QUẢ: PASSED - Chọn dư đáp án sai bị chấm sai");
    }

    /**
     * UT_EM_056: Chấm bài - câu hỏi Multiple Choice - trả lời sai
     * Mô tả: Kiểm tra logic chấm điểm câu MC khi user chọn sai đáp án
     * Input: UserChoice chọn đáp án sai (isCorrected=1 nhưng realCorrect=0)
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_056_getChoiceList_MCQuestion_wrongAnswer_shouldMarkIncorrect() {
        logger.info("[UT_EM_056] BẮT ĐẦU: Chấm bài - MC question - trả lời sai");

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
        logger.info("[UT_EM_056] Input: MC Question, user không chọn đáp án đúng");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Câu trả lời MC phải sai", result.get(0).getIsSelectedCorrected());

        logger.info("[UT_EM_056] KẾT QUẢ: PASSED - Câu MC trả lời sai");
    }

    // ========================================================================================
    // TEST CASES CHO getChoiceList() - MULTIPLE SELECT
    // ========================================================================================

    /**
     * UT_EM_057: Chấm bài - câu hỏi Multiple Select - trả lời đúng hết
     * Mô tả: Kiểm tra chấm điểm câu MS khi user chọn đúng tất cả đáp án
     * Input: User chọn tất cả đáp án đúng, không chọn đáp án sai
     * Expected: isSelectedCorrected = true
     */
    @Test
    public void UT_EM_057_getChoiceList_MSQuestion_allCorrect_shouldMarkCorrect() {
        logger.info("[UT_EM_057] BẮT ĐẦU: Chấm bài - MS question - trả lời đúng hết");

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
        logger.info("[UT_EM_057] Input: MS Question, user chọn đúng 'public', 'private'");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("Câu trả lời MS phải đúng khi chọn đúng tất cả", result.get(0).getIsSelectedCorrected());

        logger.info("[UT_EM_057] KẾT QUẢ: PASSED - Câu MS đúng tất cả");
    }

    /**
     * UT_EM_058: Chấm bài - câu hỏi Multiple Select - bỏ sót đáp án đúng
     * Mô tả: Kiểm tra chấm điểm câu MS khi user bỏ sót 1 đáp án đúng
     * Input: User chọn 1 đáp án đúng, bỏ sót 1 đáp án đúng khác
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_058_getChoiceList_MSQuestion_missedCorrectAnswer_shouldMarkIncorrect() {
        logger.info("[UT_EM_058] BẮT ĐẦU: Chấm bài - MS question - bỏ sót đáp án đúng");

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
        logger.info("[UT_EM_058] Input: MS Question, user bỏ sót 'private'(đúng)");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertFalse("Câu trả lời MS phải sai khi bỏ sót đáp án", result.get(0).getIsSelectedCorrected());

        logger.info("[UT_EM_058] KẾT QUẢ: PASSED - Câu MS sai do bỏ sót đáp án");
    }


    /**
     * UT_EM_059: Chấm bài với danh sách câu trả lời rỗng
     * Mô tả: Kiểm tra khi user không trả lời câu nào
     * Input: userChoices = empty list
     * Expected: choiceLists rỗng
     */
    @Test
    public void UT_EM_059_getChoiceList_emptyAnswerSheet_shouldReturnEmptyList() {
        logger.info("[UT_EM_059] BẮT ĐẦU: Chấm bài với danh sách câu trả lời rỗng");

        // Act
        List<ChoiceList> result = examService.getChoiceList(
                Collections.emptyList(), Collections.emptyList());

        // Assert
        assertNotNull("Kết quả không được null", result);
        assertTrue("List phải rỗng", result.isEmpty());

        logger.info("[UT_EM_059] KẾT QUẢ: PASSED - Trả về list rỗng khi không có câu trả lời");
    }

    /**
     * UT_EM_060: Chấm bài với nhiều câu hỏi hỗn hợp (TF + MC + MS)
     * Mô tả: Kiểm tra chấm điểm khi bài thi có cả 3 loại câu hỏi
     * Input: 1 câu TF (đúng), 1 câu MC (sai), 1 câu MS (đúng)
     * Expected: 3 ChoiceList, correct = [true, false, true]
     */
    @Test
    public void UT_EM_060_getChoiceList_mixedQuestionTypes_shouldGradeCorrectly() {
        logger.info("[UT_EM_060] BẮT ĐẦU: Chấm bài hỗn hợp (TF + MC + MS)");

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

        logger.info("[UT_EM_060] Input: TF(đúng) + MC(sai) + MS(đúng)");

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

        logger.info("[UT_EM_060] KẾT QUẢ: PASSED - TF=đúng, MC=sai, MS=đúng");
    }

    /**
     * UT_EM_061: Chấm bài - câu hỏi Multiple Choice - chọn đúng duy nhất
     * Mô tả: Theo chuẩn MC, user chỉ chọn 1 đáp án đúng.
     * Input: User chọn duy nhất đáp án đúng
     * Expected: isSelectedCorrected = true
     */
    @Test
    public void UT_EM_061_getChoiceList_MCQuestion_singleCorrectSelection_shouldMarkCorrect() {
        logger.info("[UT_EM_061] BẮT ĐẦU: Chấm bài - MC question - chọn đúng duy nhất");

        Choice correct = new Choice(1L, "int", 1);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(2L);
        answerSheet.setChoices(Arrays.asList(correct));
        answerSheet.setPoint(20);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(2L);
        eqp.setPoint(20);

        when(questionService.getQuestionById(2L)).thenReturn(Optional.of(sampleQuestionMC));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(1);

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue("Chọn đúng duy nhất phải được chấm đúng", result.get(0).getIsSelectedCorrected());
    }

    /**
     * UT_EM_062: Chấm bài - câu hỏi Multiple Select - chọn dư đáp án sai
     * Mô tả: Theo chuẩn MS, nếu user tick thêm đáp án sai thì phải chấm sai.
     * Input: User chọn đúng các đáp án đúng nhưng tick thêm 1 đáp án sai
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_062_getChoiceList_MSQuestion_selectExtraWrong_shouldMarkIncorrect() {
        logger.info("[UT_EM_062] BẮT ĐẦU: Chấm bài - MS question - chọn dư đáp án sai");

        Choice choiceA = new Choice(1L, "public", 1);
        Choice choiceB = new Choice(2L, "private", 1);
        Choice choiceC = new Choice(3L, "internal", 1); // user tick sai

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

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse("Theo chuẩn MS, chọn dư đáp án sai phải bị chấm sai", result.get(0).getIsSelectedCorrected());
    }

    /**
     * UT_EM_063: Chấm bài - câu hỏi Multiple Select - không chọn đáp án nào
     * Mô tả: Theo chuẩn MS, nếu không chọn đáp án nào thì phải bị chấm sai
     * Input: choices = empty list
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_063_getChoiceList_MSQuestion_emptySelection_shouldMarkIncorrect() {
        logger.info("[UT_EM_063] BẮT ĐẦU: Chấm bài - MS question - không chọn đáp án nào");

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(Collections.emptyList());
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse("Theo chuẩn MS, không chọn đáp án nào phải bị chấm sai", result.get(0).getIsSelectedCorrected());
    }

    /**
     * UT_EM_064: Chấm bài - câu hỏi Multiple Select - chỉ chọn đáp án sai
     * Mô tả: Theo chuẩn MS, chọn đáp án sai mà không chọn đúng phải bị chấm sai
     * Input: User tick một đáp án sai duy nhất
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_064_getChoiceList_MSQuestion_onlyWrongSelection_shouldMarkIncorrect() {
        logger.info("[UT_EM_064] BẮT ĐẦU: Chấm bài - MS question - chỉ chọn đáp án sai");

        Choice wrongChoice = new Choice(3L, "internal", 1);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(Arrays.asList(wrongChoice));
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));
        when(choiceService.findIsCorrectedById(3L)).thenReturn(0);

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse("Theo chuẩn MS, chỉ chọn đáp án sai phải bị chấm sai", result.get(0).getIsSelectedCorrected());
    }

    /**
     * UT_EM_065: Chấm bài - câu hỏi Multiple Select - payload chỉ gửi lựa chọn đã tick
     * Mô tả: Nhiều client chỉ gửi các choice đã chọn. Nếu user bỏ sót đáp án đúng thì vẫn phải bị chấm sai.
     * Input: User chỉ gửi 1 đáp án đúng đã tick, không gửi đáp án đúng còn lại
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_065_getChoiceList_MSQuestion_selectedOnlyPayload_missedCorrect_shouldMarkIncorrect() {
        logger.info("[UT_EM_065] BẮT ĐẦU: Chấm bài - MS question - payload chỉ có lựa chọn đã tick");

        Choice selectedCorrect = new Choice(1L, "public", 1);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(Arrays.asList(selectedCorrect));
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(1);

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse("Theo chuẩn MS, bỏ sót đáp án đúng phải bị chấm sai dù payload chỉ gửi choice đã chọn",
                result.get(0).getIsSelectedCorrected());
    }

    /**
     * UT_EM_066: Chấm bài - câu hỏi Multiple Select - choices null
     * Mô tả: Theo chuẩn nghiệp vụ, payload thiếu choices không được làm crash hệ thống,
     *       cần được xử lý như câu trả lời sai.
     * Input: answerSheet.choices = null
     * Expected: Không throw exception và isSelectedCorrected = false
     */
    @Test
    public void UT_EM_066_getChoiceList_MSQuestion_nullChoices_shouldNotCrashAndMarkIncorrect() {
        logger.info("[UT_EM_066] BẮT ĐẦU: Chấm bài - MS question - choices null");

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(null);
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));

        try {
            List<ChoiceList> result = examService.getChoiceList(
                    Arrays.asList(answerSheet), Arrays.asList(eqp));
            assertNotNull(result);
            assertEquals(1, result.size());
            assertFalse("choices null phải được xử lý như câu sai", result.get(0).getIsSelectedCorrected());
        } catch (Exception ex) {
            fail("Không được throw exception khi choices null: " + ex.getClass().getSimpleName());
        }
    }

    /**
     * UT_EM_067: Chấm bài - câu hỏi True/False - chọn nhiều đáp án
     * Mô tả: Theo chuẩn TF, user chỉ được chọn 1 đáp án. Nếu chọn nhiều đáp án phải bị chấm sai.
     * Input: User chọn cả "False" và "True"
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_067_getChoiceList_TFQuestion_multipleSelection_shouldMarkIncorrect() {
        logger.info("[UT_EM_067] BẮT ĐẦU: Chấm bài - TF question - chọn nhiều đáp án");

        Choice wrongThenCorrect1 = new Choice(2L, "False", 0);
        Choice wrongThenCorrect2 = new Choice(1L, "True", 1);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(1L);
        answerSheet.setChoices(Arrays.asList(wrongThenCorrect1, wrongThenCorrect2));
        answerSheet.setPoint(10);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(1L);
        eqp.setPoint(10);

        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(sampleQuestionTF));
        when(choiceService.findChoiceTextById(1L)).thenReturn("True");
        when(choiceService.findChoiceTextById(2L)).thenReturn("False");

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse("Theo chuẩn TF, chọn nhiều đáp án phải bị chấm sai", result.get(0).getIsSelectedCorrected());
    }

    /**
     * UT_EM_068: Chấm bài - câu hỏi Multiple Select - duplicate cùng choice ID
     * Mô tả: Payload có thể bị duplicate cùng một choice. Hệ thống không được chấm đúng sai phụ thuộc thứ tự duplicate.
     * Input: Cùng choiceId=2 xuất hiện 2 lần với isCorrected mâu thuẫn
     * Expected: isSelectedCorrected = false
     */
    @Test
    public void UT_EM_068_getChoiceList_MSQuestion_duplicateChoicePayload_shouldMarkIncorrect() {
        logger.info("[UT_EM_068] BẮT ĐẦU: Chấm bài - MS question - duplicate choice payload");

        Choice choiceCorrect = new Choice(1L, "public", 1);
        Choice duplicateWrong1 = new Choice(2L, "private", 0);
        Choice duplicateWrong2 = new Choice(2L, "private", 1);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(3L);
        answerSheet.setChoices(Arrays.asList(choiceCorrect, duplicateWrong1, duplicateWrong2));
        answerSheet.setPoint(30);

        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(3L);
        eqp.setPoint(30);

        when(questionService.getQuestionById(3L)).thenReturn(Optional.of(sampleQuestionMS));
        when(choiceService.findIsCorrectedById(1L)).thenReturn(1);
        when(choiceService.findIsCorrectedById(2L)).thenReturn(1);

        List<ChoiceList> result = examService.getChoiceList(
                Arrays.asList(answerSheet), Arrays.asList(eqp));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse("Duplicate payload mâu thuẫn phải bị chấm sai", result.get(0).getIsSelectedCorrected());
    }
}

