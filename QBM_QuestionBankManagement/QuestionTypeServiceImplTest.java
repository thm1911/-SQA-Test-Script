package com.thanhtam.backend;

import com.thanhtam.backend.entity.QuestionType;
import com.thanhtam.backend.repository.QuestionTypeRepository;
import com.thanhtam.backend.service.QuestionTypeServiceImpl;
import com.thanhtam.backend.ultilities.EQTypeCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class QuestionTypeServiceImplTest {

    private QuestionTypeRepository mockQuestionTypeRepository = mock(QuestionTypeRepository.class);
    private QuestionTypeServiceImpl questionTypeService = new QuestionTypeServiceImpl(mockQuestionTypeRepository);

    @BeforeEach
    void setUp() {
        reset(mockQuestionTypeRepository);
    }

    // ---getQuestionTypeById()---
    // Test Case ID: UT_QBM_084
    // Kiểm tra tìm loại câu hỏi theo id thành công
    @Test
    void getQuestionTypeById_Found() {
        Long id = 1L;
        QuestionType expected = new QuestionType();
        expected.setId(id);
        expected.setDescription("Trắc nghiệm");
        when(mockQuestionTypeRepository.findById(id)).thenReturn(Optional.of(expected));

        Optional<QuestionType> result = questionTypeService.getQuestionTypeById(id);

        verify(mockQuestionTypeRepository).findById(id);
        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        assertEquals(id, result.get().getId());
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_085
    // Kiểm tra id không tồn tại
    @Test
    void getQuestionTypeById_NotFound() {
        Long id = 100L;
        when(mockQuestionTypeRepository.findById(id)).thenReturn(Optional.empty());

        Optional<QuestionType> result = questionTypeService.getQuestionTypeById(id);

        verify(mockQuestionTypeRepository).findById(id);
        assertFalse(result.isPresent());
    }

    // ---getQuestionTypeByCode()---
    // Test Case ID: UT_QBM_086
    // Kiểm tra tìm loại câu hỏi theo mã type thành công
    @Test
    void getQuestionTypeByCode_Found() {
        EQTypeCode code = EQTypeCode.MC;
        QuestionType expected = new QuestionType();
        expected.setId(1L);
        expected.setTypeCode(code);
        when(mockQuestionTypeRepository.findAllByTypeCode(code)).thenReturn(Optional.of(expected));

        Optional<QuestionType> result = questionTypeService.getQuestionTypeByCode(code);

        verify(mockQuestionTypeRepository).findAllByTypeCode(code);
        assertTrue(result.isPresent());
        assertSame(expected, result.get());
        assertEquals(EQTypeCode.MC, result.get().getTypeCode());
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_087
    // Kiểm tra mã type không tồn tại
    @Test
    void getQuestionTypeByCode_NotFound() {
        EQTypeCode code = EQTypeCode.TF;
        when(mockQuestionTypeRepository.findAllByTypeCode(code)).thenReturn(Optional.empty());

        Optional<QuestionType> result = questionTypeService.getQuestionTypeByCode(code);

        verify(mockQuestionTypeRepository).findAllByTypeCode(code);
        assertFalse(result.isPresent());
    }

    // ---getQuestionTypeList()---
    // Test Case ID: UT_QBM_088
    // Kiểm tra lấy danh sách tất cả loại câu hỏi
    @Test
    void getQuestionTypeList_Success() {
        QuestionType t1 = new QuestionType();
        t1.setId(1L);
        QuestionType t2 = new QuestionType();
        t2.setId(2L);
        when(mockQuestionTypeRepository.findAll()).thenReturn(Arrays.asList(t1, t2));

        List<QuestionType> result = questionTypeService.getQuestionTypeList();

        verify(mockQuestionTypeRepository).findAll();
        assertEquals(2, result.size());
        assertSame(t1, result.get(0));
        assertSame(t2, result.get(1));
        log.info(result.toString());
    }

    // Test Case ID: UT_QBM_089
    // Kiểm tra danh sách rỗng
    @Test
    void getQuestionTypeList_Empty() {
        when(mockQuestionTypeRepository.findAll()).thenReturn(Collections.emptyList());

        List<QuestionType> result = questionTypeService.getQuestionTypeList();

        verify(mockQuestionTypeRepository).findAll();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        log.info(result.toString());
    }

    // ---existsById()---
    // Test Case ID: UT_QBM_090
    // Kiểm tra id tồn tại — trả về true
    @Test
    void existsById_ReturnsTrue() {
        Long id = 1L;
        when(mockQuestionTypeRepository.existsById(id)).thenReturn(true);

        boolean result = questionTypeService.existsById(id);

        verify(mockQuestionTypeRepository).existsById(id);
        assertTrue(result);
    }

    // Test Case ID: UT_QBM_091
    // Kiểm tra id không tồn tại — trả về false
    @Test
    void existsById_ReturnsFalse() {
        Long id = 100L;
        when(mockQuestionTypeRepository.existsById(id)).thenReturn(false);

        boolean result = questionTypeService.existsById(id);

        verify(mockQuestionTypeRepository).existsById(id);
        assertFalse(result);
    }
}
