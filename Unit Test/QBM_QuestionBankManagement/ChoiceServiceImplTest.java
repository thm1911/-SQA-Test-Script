package com.thanhtam.backend;

import com.thanhtam.backend.repository.ChoiceRepository;
import com.thanhtam.backend.service.ChoiceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class ChoiceServiceImplTest {

    private ChoiceRepository mockChoiceRepository = mock(ChoiceRepository.class);
    private ChoiceServiceImpl choiceService = new ChoiceServiceImpl(mockChoiceRepository);

    @BeforeEach
    void setUp() {
        reset(mockChoiceRepository);
    }

    // ---findIsCorrectedById()---
    // Test Case ID: UT_QBM_079
    // Kiểm tra lấy cờ corrected theo id thành công
    @Test
    void findIsCorrectedById_returnCorrect() {
        Long id = 1L;
        when(mockChoiceRepository.findIsCorrectedById(id)).thenReturn(1);

        Integer result = choiceService.findIsCorrectedById(id);

        verify(mockChoiceRepository).findIsCorrectedById(id);
        assertEquals(1, result.intValue());
        log.info(String.valueOf(result));
    }

    // Test Case ID: UT_QBM_080
    // Kiểm tra giá trị corrected = 0
    @Test
    void findIsCorrectedById_returnIncorrect() {
        Long id = 1L;
        when(mockChoiceRepository.findIsCorrectedById(id)).thenReturn(0);

        Integer result = choiceService.findIsCorrectedById(id);

        verify(mockChoiceRepository).findIsCorrectedById(id);
        assertEquals(0, result.intValue());
        log.info(String.valueOf(result));
    }

    // Test Case ID: UT_QBM_081
    // Kiểm tra khi id không tồn tại
    @Test
    void findIsCorrectedById_NotFoundId() {
        Long id = 100L;
        when(mockChoiceRepository.findIsCorrectedById(id)).thenReturn(null);

        Integer result = choiceService.findIsCorrectedById(id);

        verify(mockChoiceRepository).findIsCorrectedById(id);
        assertNull(result);
        log.info(String.valueOf(result));
    }

    // ---findChoiceTextById()---
    // Test Case ID: UT_QBM_082
    // Kiểm tra lấy nội dung lựa chọn theo id thành công
    @Test
    void findChoiceTextById_Success() {
        Long id = 1L;
        when(mockChoiceRepository.findChoiceTextById(id)).thenReturn("Nội dung");

        String result = choiceService.findChoiceTextById(id);

        verify(mockChoiceRepository).findChoiceTextById(id);
        assertEquals("Nội dung", result);
        log.info(result);
    }

    // Test Case ID: UT_QBM_083
    // Kiểm tra khi id không tồn tại hoặc repository trả về null
    @Test
    void findChoiceTextById_NotFoundId() {
        Long id = 100L;
        when(mockChoiceRepository.findChoiceTextById(id)).thenReturn(null);

        String result = choiceService.findChoiceTextById(id);

        verify(mockChoiceRepository).findChoiceTextById(id);
        assertNull(result);
        log.info(String.valueOf(result));
    }
}
