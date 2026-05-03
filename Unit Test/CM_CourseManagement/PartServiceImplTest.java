package com.thanhtam.backend;

import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.entity.Part;
import com.thanhtam.backend.repository.PartRepository;
import com.thanhtam.backend.service.PartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PartServiceImplTest {
    private PartRepository partRepository;
    private PartServiceImpl partService;

    private Course course1;
    private Part part1;
    private Part part2;

    @BeforeEach
    public void setUp() {
        // Mock repository và khởi tạo service
        partRepository = mock(PartRepository.class);
        partService = new PartServiceImpl(partRepository);

        // Tạo dữ liệu mẫu
        course1 = new Course(1L, "C001", "Course 1", null, new ArrayList<>());
        part1 = new Part(1L, "Part 1", course1);
        part2 = new Part(2L, "Part 2", course1);
    }


    // UT_CM_065
    // Mục tiêu: Kiểm tra hàm savePart gọi repository thành công khi dữ liệu hợp lệ
    @Test
    void UT_CM_065_savePart_Success() {
        // Giả lập repository save thành công và trả về entity đã lưu
        when(partRepository.save(part1)).thenReturn(part1);

        // Gọi service savePart
        partService.savePart(part1);

        // Kiểm tra repository được gọi đúng 1 lần với đúng dữ liệu
        // Hàm trả về void, nên chỉ check được verify, không check được giá trị trả về
        // Kiểm tra lưu vào db thật sẽ do test API thực hiện
        verify(partRepository, times(1)).save(part1);
    }
    // UT_CM_066
    // Mục tiêu: Kiểm tra hàm savePart ném exception khi repository xảy ra lỗi
    @Test
    void UT_CM_066_savePart_Fail() {
        // Giả lập repository ném exception khi lưu dữ liệu
        doThrow(new RuntimeException("DB ERROR"))
                .when(partRepository).save(part1);

        // Kiểm tra service ném exception khi save thất bại
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> partService.savePart(part1));

        // Kiểm tra đúng message lỗi
        assertEquals("DB ERROR", ex.getMessage());
    }

    // UT_CM_067
    // Mục tiêu: Lấy danh sách Part theo CourseId có phân trang thành công khi có dữ liệu
    @Test
    void UT_CM_067_getPartLisByCourse_Success() {
        // Tạo pageable
        Pageable pageable = PageRequest.of(0, 10);

        // Dữ liệu mock trả về
        List<Part> parts = Arrays.asList(part1, part2);

        Page<Part> page = new PageImpl<>(parts, pageable, parts.size());

        // Giả lập repository trả về page có dữ liệu
        when(partRepository.findAllByCourseId(1L, pageable))
                .thenReturn(page);

        // Gọi service
        Page<Part> result = partService.getPartLisByCourse(pageable, 1L);

        // Kiểm tra kết quả
        assertNotNull(result);
        assertEquals(2, result.getContent().size());

        // kiểm tra đúng dữ liệu trả về
        assertEquals(part1.getId(), result.getContent().get(0).getId());
        assertEquals(part2.getId(), result.getContent().get(1).getId());
    }

    // UT_CM_068
    // Mục tiêu: Lấy danh sách Part theo CourseId có phân trang khi không có dữ liệu (Page rỗng)
    @Test
    void UT_CM_068_getPartLisByCourse_Empty() {
        // Tạo pageable
        Pageable pageable = PageRequest.of(0, 10);

        // Giả lập repository trả về Page rỗng
        when(partRepository.findAllByCourseId(1L, pageable))
                .thenReturn(Page.empty());

        // Gọi service
        Page<Part> result = partService.getPartLisByCourse(pageable, 1L);

        // Kiểm tra kết quả
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }


    // UT_CM_069
    // Mục tiêu: Lấy danh sách Part theo Course object thành công khi có dữ liệu
    @Test
    void UT_CM_069_getPartListByCourse_Success() {
        // Giả lập repository trả về danh sách part
        List<Part> parts = Arrays.asList(part1, part2);

        when(partRepository.findAllByCourse(course1))
                .thenReturn(parts);

        // Gọi service
        List<Part> result = partService.getPartListByCourse(course1);

        // Kiểm tra kết quả: result không null và có đúng số lượng phần tử
        assertNotNull(result);
        assertEquals(2, result.size());

        // kiểm tra đúng dữ liệu
        assertEquals(part1.getId(), result.get(0).getId());
        assertEquals(part2.getId(), result.get(1).getId());
    }

    // UT_CM_070
    // Mục tiêu: Lấy danh sách Part theo Course object khi không có dữ liệu
    @Test
    void UT_CM_070_getPartListByCourse_Empty() {
        // Giả lập repository trả về danh sách rỗng
        when(partRepository.findAllByCourse(course1))
                .thenReturn(new ArrayList<>());

        // Gọi service
        List<Part> result = partService.getPartListByCourse(course1);

        // Kiểm tra kết quả
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // UT_CM_071
    // Mục tiêu: Tìm Part theo ID thành công khi tồn tại dữ liệu
    @Test
    void UT_CM_071_findPartById_Success() {
        // Giả lập repository trả về part tồn tại
        when(partRepository.findById(1L))
                .thenReturn(Optional.of(part1));

        // Gọi service
        Optional<Part> result = partService.findPartById(1L);

        // Kiểm tra kết quả
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(part1.getId(), result.get().getId());
    }

    // UT_CM_072
    // Mục tiêu: Tìm Part theo ID khi không tồn tại dữ liệu
    @Test
    void UT_CM_072_findPartById_NotFound() {
        // Giả lập repository không tìm thấy
        when(partRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Gọi service
        Optional<Part> result = partService.findPartById(99L);

        // Kiểm tra kết quả
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    // UT_CM_073
    // Mục tiêu: Kiểm tra tồn tại Part theo ID trả về true
    @Test
    void UT_CM_073_existsById_True() {
        // Giả lập repository trả về true
        when(partRepository.existsById(1L))
                .thenReturn(true);

        // Gọi service
        boolean result = partService.existsById(1L);

        // Kiểm tra kết quả có đúng là true
        assertTrue(result);
    }

    // UT_CM_074
    // Mục tiêu: Kiểm tra tồn tại Part theo ID trả về false
    @Test
    void UT_CM_074_existsById_False() {
        // Giả lập repository trả về false
        when(partRepository.existsById(99L))
                .thenReturn(false);

        // Gọi service
        boolean result = partService.existsById(99L);

        // Kiểm tra kết quả có đúng là false
        assertFalse(result);
    }
}