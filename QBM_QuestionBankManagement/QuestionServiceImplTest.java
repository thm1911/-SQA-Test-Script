package com.thanhtam.backend;

import com.thanhtam.backend.dto.AnswerSheet;
import com.thanhtam.backend.dto.ExamQuestionPoint;
import com.thanhtam.backend.entity.Choice;
import com.thanhtam.backend.entity.Part;
import com.thanhtam.backend.entity.Question;
import com.thanhtam.backend.entity.QuestionType;
import com.thanhtam.backend.repository.QuestionRepository;
import com.thanhtam.backend.service.QuestionServiceImpl;
import com.thanhtam.backend.ultilities.DifficultyLevel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class QuestionServiceImplTest {

    private QuestionRepository mockQuestionRepository = mock(QuestionRepository.class);
    private QuestionServiceImpl questionService = new QuestionServiceImpl(mockQuestionRepository);

    @BeforeEach
    void setUp() {
        reset(mockQuestionRepository);
    }

    // ---getQuestionById()---
    // Test Case ID: UT_QBM_040
    // Kiểm tra tìm thấy câu hỏi theo id
    @Test
    void getQuestionById_Found() {
        Long id = 1L;
        Question expected = new Question();
        expected.setId(id);
        expected.setQuestionText("Nội dung câu hỏi");

        when(mockQuestionRepository.findById(id)).thenReturn(Optional.of(expected));

        Optional<Question> result = questionService.getQuestionById(id);

        verify(mockQuestionRepository).findById(id);
        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        assertEquals(id, result.get().getId());
        assertEquals("Nội dung câu hỏi", result.get().getQuestionText());

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_041
    // Kiểm tra lấy câu hỏi với id không tìm thấy
    @Test
    void getQuestionById_NotFound() {
        Long id = 100L;
        when(mockQuestionRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Question> result = questionService.getQuestionById(id);

        verify(mockQuestionRepository).findById(id);
        assertFalse(result.isPresent());
    }

    // ---getQuestionByPart()---
    // Test Case ID: UT_QBM_042
    // Kiểm tra trả về danh sách câu hỏi theo part
    @Test
    void getQuestionByPart_Success() {
        Part part = new Part();
        part.setId(1L);

        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        List<Question> expected = Arrays.asList(q1, q2);

        when(mockQuestionRepository.findByPart(part)).thenReturn(expected);

        List<Question> result = questionService.getQuestionByPart(part);

        verify(mockQuestionRepository).findByPart(part);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_043
    // Kiểm tra trả về danh sách câu hỏi trống
    @Test
    void getQuestionByPart_EmptyList() {
        Part part = new Part();
        part.setId(1L);
        when(mockQuestionRepository.findByPart(part)).thenReturn(Collections.emptyList());

        List<Question> result = questionService.getQuestionByPart(part);

        verify(mockQuestionRepository).findByPart(part);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info(result.toString());
    }

    // ---getQuestionByQuestionType()---
    // Test Case ID: UT_QBM_044
    // Kiểm tra trả về danh sách câu hỏi theo loại câu hỏi thành công
    @Test
    void getQuestionByQuestionType_Success() {
        QuestionType questionType = new QuestionType();
        questionType.setId(1L);

        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        List<Question> expected = Arrays.asList(q1, q2);

        when(mockQuestionRepository.findByQuestionType(questionType)).thenReturn(expected);

        List<Question> result = questionService.getQuestionByQuestionType(questionType);

        verify(mockQuestionRepository).findByQuestionType(questionType);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_045
    // Kiểm tra trả về danh sách câu hỏi trống theo loại câu hỏi
    @Test
    void getQuestionByQuestionType_EmptyList() {
        QuestionType questionType = new QuestionType();
        questionType.setId(1L);
        when(mockQuestionRepository.findByQuestionType(questionType)).thenReturn(Collections.emptyList());

        List<Question> result = questionService.getQuestionByQuestionType(questionType);

        verify(mockQuestionRepository).findByQuestionType(questionType);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_046
    // Kiểm tra khi loại câu hỏi không tồn tại (không có trong hệ thống) thì trả về danh sách rỗng
    @Test
    void getQuestionByQuestionType_QuestionTypeNotExists() {
        QuestionType nonExistentType = new QuestionType();
        nonExistentType.setId(999L);
        when(mockQuestionRepository.findByQuestionType(nonExistentType)).thenReturn(Collections.emptyList());

        List<Question> result = questionService.getQuestionByQuestionType(nonExistentType);

        verify(mockQuestionRepository).findByQuestionType(nonExistentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info(result.toString());
    }

    // ---getQuestionPointList()---
    // Test Case ID: UT_QBM_047
    // Kiểm tra lấy danh sách câu hỏi theo điểm câu hỏi trong đề thi
    @Test
    void getQuestionPointList_Success() {
        ExamQuestionPoint eqp1 = new ExamQuestionPoint();
        eqp1.setQuestionId(1L);
        eqp1.setPoint(5);
        ExamQuestionPoint eqp2 = new ExamQuestionPoint();
        eqp2.setQuestionId(2L);
        eqp2.setPoint(10);

        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);

        when(mockQuestionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(mockQuestionRepository.findById(2L)).thenReturn(Optional.of(q2));

        List<ExamQuestionPoint> examQuestionPoints = Arrays.asList(eqp1, eqp2);
        List<Question> result = questionService.getQuestionPointList(examQuestionPoints);

        verify(mockQuestionRepository).findById(1L);
        verify(mockQuestionRepository).findById(2L);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(q1, result.get(0));
        assertSame(q2, result.get(1));

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_048
    // Kiểm tra danh sách điểm câu hỏi rỗng trả về danh sách câu hỏi rỗng
    @Test
    void getQuestionPointList_EmptyList() {
        List<Question> result = questionService.getQuestionPointList(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_049
    // Kiểm tra questionId không tồn tại
    @Test
    void getQuestionPointList_QuestionIdNotFound() {
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(100L);
        when(mockQuestionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> questionService.getQuestionPointList(Collections.singletonList(eqp)));

        verify(mockQuestionRepository).findById(100L);
    }

    // ---convertFromQuestionList()---
    // Test Case ID: UT_QBM_050
    // Kiểm tra chuyển một câu hỏi có các lựa chọn: reset isCorrected về 0 và tạo AnswerSheet
    @Test
    void convertFromQuestionList_Success() {
        Choice c1 = new Choice();
        c1.setId(1L);
        c1.setIsCorrected(1);
        Choice c2 = new Choice();
        c2.setId(2L);
        c2.setIsCorrected(1);

        Question q = new Question();
        q.setId(1L);
        q.setPoint(5);
        q.setChoices(Arrays.asList(c1, c2));

        List<AnswerSheet> result = questionService.convertFromQuestionList(Collections.singletonList(q));

        assertEquals(1, result.size());
        AnswerSheet sheet = result.get(0);
        assertEquals(1L, sheet.getQuestionId().longValue());
        assertEquals(5, sheet.getPoint().intValue());
        assertEquals(2, sheet.getChoices().size());
        assertEquals(0, sheet.getChoices().get(0).getIsCorrected());
        assertEquals(0, sheet.getChoices().get(1).getIsCorrected());

        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_051
    // Kiểm tra danh sách câu hỏi rỗng trả về danh sách AnswerSheet rỗng
    @Test
    void convertFromQuestionList_EmptyList() {
        List<AnswerSheet> result = questionService.convertFromQuestionList(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_052
    // Kiểm tra câu hỏi không có lựa chọn (danh sách choices rỗng) vẫn tạo AnswerSheet với choices rỗng
    @Test
    void convertFromQuestionList_EmptyChoices() {
        Question q = new Question();
        q.setId(1L);
        q.setPoint(5);
        q.setChoices(Collections.emptyList());

        List<AnswerSheet> result = questionService.convertFromQuestionList(Collections.singletonList(q));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getQuestionId().longValue());
        assertTrue(result.get(0).getChoices().isEmpty());
        log.info(result.toString());
    }

    // ---getQuestionList()---
    // Test Case ID: UT_QBM_053
    // Kiểm tra lấy danh sách tất cả câu hỏi thành công
    @Test
    void getQuestionList_Success() {
        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        when(mockQuestionRepository.findAll()).thenReturn(Arrays.asList(q1, q2));

        List<Question> result = questionService.getQuestionList();

        verify(mockQuestionRepository).findAll();
        assertEquals(2, result.size());
        assertSame(q1, result.get(0));
        assertSame(q2, result.get(1));
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_054
    // Kiểm tra không có câu hỏi nào trong hệ thống
    @Test
    void getQuestionList_Empty() {
        when(mockQuestionRepository.findAll()).thenReturn(Collections.emptyList());

        List<Question> result = questionService.getQuestionList();

        verify(mockQuestionRepository).findAll();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        log.info(result.toString());
    }

    // ---findQuestionsByPart()---
    // Test Case ID: UT_QBM_055
    // Kiểm tra phân trang câu hỏi theo part trả về thành công
    @Test
    void findQuestionsByPart_SuccessPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Part part = new Part();
        part.setId(1L);
        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        Page<Question> expected = new PageImpl<>(Arrays.asList(q1, q2), pageable, 2);
        when(mockQuestionRepository.findQuestionsByPart(pageable, part)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart(pageable, part);

        verify(mockQuestionRepository).findQuestionsByPart(pageable, part);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(q1, result.getContent().get(0));
        assertSame(q2, result.getContent().get(1));

        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_056
    // Kiểm tra phân trang theo part trả về danh sách rỗng (trang trống)
    @Test
    void findQuestionsByPart_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Part part = new Part();
        part.setId(1L);
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart(pageable, part)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart(pageable, part);

        verify(mockQuestionRepository).findQuestionsByPart(pageable, part);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // ---findQuestionsByPartAndDeletedFalse()---
    // Test Case ID: UT_QBM_057
    // Kiểm tra phân trang câu hỏi theo part và deleted=false — có dữ liệu
    @Test
    void findQuestionsByPartAndDeletedFalse_SuccessPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Part part = new Part();
        part.setId(1L);
        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        Page<Question> expected = new PageImpl<>(Arrays.asList(q1, q2), pageable, 2);
        when(mockQuestionRepository.findQuestionsByPartAndDeletedFalse(pageable, part)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPartAndDeletedFalse(pageable, part);

        verify(mockQuestionRepository).findQuestionsByPartAndDeletedFalse(pageable, part);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(q1, result.getContent().get(0));
        assertSame(q2, result.getContent().get(1));
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_058
    // Kiểm tra phân trang theo part và deleted=false — không có bản ghi
    @Test
    void findQuestionsByPartAndDeletedFalse_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Part part = new Part();
        part.setId(1L);
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPartAndDeletedFalse(pageable, part)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPartAndDeletedFalse(pageable, part);

        verify(mockQuestionRepository).findQuestionsByPartAndDeletedFalse(pageable, part);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // ---findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse()---
    // Test Case ID: UT_QBM_059
    // Kiểm tra phân trang theo partId, username và deleted=false trả dữ liệu thành công
    @Test
    void findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse_SuccessPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "admin";
        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        Page<Question> expected = new PageImpl<>(Arrays.asList(q1, q2), pageable, 2);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(
                pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(q1, result.getContent().get(0));
        assertSame(q2, result.getContent().get(1));
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_060
    // Kiểm tra phân trang theo partId, username và deleted=false — trang rỗng
    @Test
    void findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "admin";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(
                pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_061
    // Kiểm tra khi partId không tồn tại hoặc không có câu hỏi phù hợp — trang rỗng
    @Test
    void findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse_NotFoundPartId() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 100L;
        String username = "admin";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(
                pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_062
    // Kiểm tra khi username không tồn tại — trang rỗng
    @Test
    void findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse_UsernameNotExists() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "NotExistUser";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(
                pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // ---findAllQuestions()---
    // Test Case ID: UT_QBM_063
    // Kiểm tra phân trang lấy tất cả câu hỏi thành công
    @Test
    void findAllQuestions_SuccessPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        Page<Question> expected = new PageImpl<>(Arrays.asList(q1, q2), pageable, 2);
        when(mockQuestionRepository.findAll(pageable)).thenReturn(expected);

        Page<Question> result = questionService.findAllQuestions(pageable);

        verify(mockQuestionRepository).findAll(pageable);
        assertEquals(2, result.getTotalElements());
        assertSame(q1, result.getContent().get(0));
        assertSame(q2, result.getContent().get(1));
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_064
    // Kiểm tra phân trang lấy tất cả câu hỏi — không có bản ghi
    @Test
    void findAllQuestions_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findAll(pageable)).thenReturn(expected);

        Page<Question> result = questionService.findAllQuestions(pageable);

        verify(mockQuestionRepository).findAll(pageable);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
    }

    // ---findQuestionTextById()---
    // Test Case ID: UT_QBM_065
    // Kiểm tra lấy nội dung câu hỏi theo id thành công
    @Test
    void findQuestionTextById_Success() {
        Long questionId = 1L;
        when(mockQuestionRepository.findQuestionTextById(questionId)).thenReturn("Nội dung câu hỏi");

        String result = questionService.findQuestionTextById(questionId);

        verify(mockQuestionRepository).findQuestionTextById(questionId);
        assertEquals("Nội dung câu hỏi", result);
        log.info(result);
    }

    // Test Case ID: UT_QBM_066
    // Kiểm tra khi không có câu hỏi hoặc repository trả về null
    @Test
    void findQuestionTextById_ReturnsNull() {
        Long questionId = 100L;
        when(mockQuestionRepository.findQuestionTextById(questionId)).thenReturn(null);

        String result = questionService.findQuestionTextById(questionId);

        verify(mockQuestionRepository).findQuestionTextById(questionId);
        assertNull(result);
    }

    // ---findQuestionsByPart_IdAndCreatedBy_Username()---
    // Test Case ID: UT_QBM_067
    // Kiểm tra phân trang theo partId và username — có dữ liệu
    @Test
    void findQuestionsByPart_IdAndCreatedBy_Username_SuccessPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "admin";
        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        Page<Question> expected = new PageImpl<>(Arrays.asList(q1, q2), pageable, 2);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);
        assertEquals(2, result.getTotalElements());
        assertSame(q1, result.getContent().get(0));
        assertSame(q2, result.getContent().get(1));
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_068
    // Kiểm tra phân trang theo partId và username — trang rỗng
    @Test
    void findQuestionsByPart_IdAndCreatedBy_Username_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "admin";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_069
    // Kiểm tra khi partId không tồn tại
    @Test
    void findQuestionsByPart_IdAndCreatedBy_Username_NotFoundPartId() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 100L;
        String username = "admin";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_070
    // Kiểm tra khi username không tồn tại — trang rỗng
    @Test
    void findQuestionsByPart_IdAndCreatedBy_Username_UsernameNotExists() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "NotExistUser";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username))
                .thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);

        verify(mockQuestionRepository).findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
    }

    // ---findQuestionsByCreatedBy_Username()---
    // Test Case ID: UT_QBM_071
    // Kiểm tra phân trang câu hỏi theo người tạo thành công
    @Test
    void findQuestionsByCreatedBy_Username_SuccessPage() {
        Pageable pageable = PageRequest.of(0, 10);
        String username = "admin";
        Question q = new Question();
        q.setId(1L);
        Page<Question> expected = new PageImpl<>(Collections.singletonList(q), pageable, 1);
        when(mockQuestionRepository.findQuestionsByCreatedBy_Username(pageable, username)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByCreatedBy_Username(pageable, username);

        verify(mockQuestionRepository).findQuestionsByCreatedBy_Username(pageable, username);
        assertEquals(1, result.getTotalElements());
        assertSame(q, result.getContent().get(0));
        log.info(result.toString());
        log.info(result.getContent().toString());
    }

    // Test Case ID: UT_QBM_072
    // Kiểm tra phân trang theo người tạo — không có bản ghi
    @Test
    void findQuestionsByCreatedBy_Username_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        String username = "admin";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByCreatedBy_Username(pageable, username)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByCreatedBy_Username(pageable, username);

        verify(mockQuestionRepository).findQuestionsByCreatedBy_Username(pageable, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_073
    // Kiểm tra khi username không tồn tại hoặc không có câu hỏi — trang rỗng
    @Test
    void findQuestionsByCreatedBy_Username_UsernameNotExists() {
        Pageable pageable = PageRequest.of(0, 10);
        String username = "NotExistUser";
        Page<Question> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(mockQuestionRepository.findQuestionsByCreatedBy_Username(pageable, username)).thenReturn(expected);

        Page<Question> result = questionService.findQuestionsByCreatedBy_Username(pageable, username);

        verify(mockQuestionRepository).findQuestionsByCreatedBy_Username(pageable, username);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        log.info(result.toString());
    }

    // ---save()---
    // Test Case ID: UT_QBM_074
    // Kiểm tra lưu câu hỏi mức EASY — gán điểm 5 và gọi repository.save
    @Test
    void save_WithEasyDifficulty_SetsPoint5() {
        Question q = new Question();
        q.setId(1L);
        q.setDifficultyLevel(DifficultyLevel.EASY);

        questionService.save(q);

        verify(mockQuestionRepository).save(q);
        assertEquals(5, q.getPoint());
    }

    // Test Case ID: UT_QBM_075
    // Kiểm tra lưu câu hỏi mức MEDIUM — gán điểm 10
    @Test
    void save_WithMediumDifficulty_SetsPoint10() {
        Question q = new Question();
        q.setDifficultyLevel(DifficultyLevel.MEDIUM);

        questionService.save(q);

        verify(mockQuestionRepository).save(q);
        assertEquals(10, q.getPoint());
    }

    // Test Case ID: UT_QBM_076
    // Kiểm tra lưu câu hỏi mức HARD — gán điểm 15
    @Test
    void save_WithHardDifficulty_SetsPoint15() {
        Question q = new Question();
        q.setDifficultyLevel(DifficultyLevel.HARD);

        questionService.save(q);

        verify(mockQuestionRepository).save(q);
        assertEquals(15, q.getPoint());
    }

    // Test Case ID: UT_QBM_077
    // Kiểm tra difficultyLevel null — switch ném NullPointerException
    @Test
    void save_WithNullDifficulty_ThrowsNullPointerException() {
        Question q = new Question();
        q.setDifficultyLevel(null);

        assertThrows(NullPointerException.class, () -> questionService.save(q));
    }

    // Test Case ID: UT_QBM_078
    // Kiểm tra mức độ khác (OTHER) — rơi vào default, gán điểm 0
    @Test
    void save_WithOtherDifficulty_SetsPoint0() {
        Question q = new Question();
        q.setDifficultyLevel(DifficultyLevel.OTHER);

        questionService.save(q);

        verify(mockQuestionRepository).save(q);
        assertEquals(0, q.getPoint());
        log.info(String.valueOf(q.getPoint()));
    }

}
