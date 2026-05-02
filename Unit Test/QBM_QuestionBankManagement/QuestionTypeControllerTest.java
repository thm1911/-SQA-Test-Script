package com.thanhtam.backend;

import com.thanhtam.backend.controller.QuestionTypeController;
import com.thanhtam.backend.entity.QuestionType;
import com.thanhtam.backend.service.QuestionTypeService;
import com.thanhtam.backend.ultilities.EQTypeCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class QuestionTypeControllerTest {

    QuestionTypeService mockQuestionTypeService = mock(QuestionTypeService.class);

    QuestionTypeController questionTypeController = new QuestionTypeController(mockQuestionTypeService);

    @BeforeEach
    void setUp() {
        reset(mockQuestionTypeService);
    }

    // ---getAllQuestionType()---
    // Test Case ID: UT_QBM_033
    // Kiểm tra lấy danh sách loại câu hỏi thành công
    @Test
    void getAllQuestionType_Success() {
        QuestionType first = new QuestionType();
        first.setId(1L);
        first.setTypeCode(EQTypeCode.TF);
        first.setDescription("Trắc nghiệm đúng sai");

        QuestionType second = new QuestionType();
        second.setId(2L);
        second.setTypeCode(EQTypeCode.MC);
        second.setDescription("Trắc nghiệm 1 đáp án");

        List<QuestionType> expected = Arrays.asList(first, second);
        when(mockQuestionTypeService.getQuestionTypeList()).thenReturn(expected);

        List<QuestionType> actual = questionTypeController.getAllQuestionType();

        verify(mockQuestionTypeService).getQuestionTypeList();
        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertEquals(1L, actual.get(0).getId());
        assertEquals(EQTypeCode.TF, actual.get(0).getTypeCode());
        assertEquals(2L, actual.get(1).getId());
        assertEquals(EQTypeCode.MC, actual.get(1).getTypeCode());
    }

    // Test Case ID: UT_QBM_034
    // Kiểm tra lấy danh sách loại câu hỏi trống
    @Test
    void getAllQuestionType_EmptyList() {
        when(mockQuestionTypeService.getQuestionTypeList()).thenReturn(Collections.emptyList());

        List<QuestionType> actual = questionTypeController.getAllQuestionType();

        verify(mockQuestionTypeService).getQuestionTypeList();
        assertNotNull(actual);
        assertEquals(0, actual.size());
    }

    // ---getQuestionTypeById()---
    // Test Case ID: UT_QBM_035
    // Kiểm tra lấy loại câu hỏi theo id khi dữ liệu tồn tại
    @Test
    void getQuestionTypeById_Found() {
        Long id = 1L;
        QuestionType expected = new QuestionType();
        expected.setId(id);
        expected.setTypeCode(EQTypeCode.MS);
        expected.setDescription("Trắc nghiệm nhiều lựa chọn");

        when(mockQuestionTypeService.getQuestionTypeById(id)).thenReturn(Optional.of(expected));

        QuestionType actual = questionTypeController.getQuestionTypeById(id);

        verify(mockQuestionTypeService).getQuestionTypeById(id);
        assertNotNull(actual);
        assertEquals(id, actual.getId());
        assertEquals(EQTypeCode.MS, actual.getTypeCode());
        assertEquals("Trắc nghiệm nhiều lựa chọn", actual.getDescription());

        log.info(actual.toString());
    }

    // Test Case ID: UT_QBM_036
    // Kiểm tra ném NoSuchElementException khi không tìm thấy theo id
    @Test
    void getQuestionTypeById_NotFound() {
        Long id = 100L;
        when(mockQuestionTypeService.getQuestionTypeById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionTypeController.getQuestionTypeById(id));
        verify(mockQuestionTypeService).getQuestionTypeById(id);
    }

    // ---getQuestionTypeByTypeCode()---
    // Test Case ID: UT_QBM_037
    // Kiểm tra lấy loại câu hỏi theo mã enum (typeCode) khi tồn tại
    @Test
    void getQuestionTypeByTypeCode_Found() {
        String typeCode = "MC";
        QuestionType expected = new QuestionType();
        expected.setId(1L);
        expected.setTypeCode(EQTypeCode.MC);
        expected.setDescription("Trắc nghiệm 1 đáp án");

        when(mockQuestionTypeService.getQuestionTypeByCode(EQTypeCode.MC)).thenReturn(Optional.of(expected));

        QuestionType actual = questionTypeController.getQuestionTypeByTypeCode(typeCode);

        verify(mockQuestionTypeService).getQuestionTypeByCode(EQTypeCode.MC);
        assertNotNull(actual);
        assertEquals(EQTypeCode.MC, actual.getTypeCode());
        assertEquals("Trắc nghiệm 1 đáp án", actual.getDescription());

        log.info(actual.toString());
    }

    // Test Case ID: UT_QBM_038
    // Kiểm tra ném NoSuchElementException khi không tìm thấy theo mã loại
    @Test
    void getQuestionTypeByTypeCode_ServiceEmpty() {
        String typeCode = "TF";
        when(mockQuestionTypeService.getQuestionTypeByCode(EQTypeCode.TF)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionTypeController.getQuestionTypeByTypeCode(typeCode));
        verify(mockQuestionTypeService).getQuestionTypeByCode(EQTypeCode.TF);
    }

    // Test Case ID: UT_QBM_039
    // Kiểm tra typeCode không phải tên hằng EQTypeCode => IllegalArgumentException
    @Test
    void getQuestionTypeByTypeCode_InvalidEnum() {
        assertThrows(IllegalArgumentException.class, () -> questionTypeController.getQuestionTypeByTypeCode("INVALID"));
    }

}
