package com.thanhtam.backend;

import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.entity.Part;
import com.thanhtam.backend.repository.PartRepository;
import com.thanhtam.backend.service.PartServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PartServiceImplTest {

    private PartRepository partRepository;
    private PartServiceImpl partService;

    private Course course;
    private Part part;

    @Before
    public void setUp() {
        partRepository = mock(PartRepository.class);
        partService = new PartServiceImpl(partRepository);

        course = new Course(1L, "C001", "Course 1", null, new ArrayList<>());
        part = new Part(1L, "Part 1", course);
    }

    // UT_CM_047
    // Mục tiêu: Lưu Part thành công, đảm bảo gọi đúng repository.save với object truyền vào
    @Test
    public void savePart_Success() {
        partService.savePart(part);

        verify(partRepository, times(1)).save(part);
        verifyNoMoreInteractions(partRepository);
    }

    // UT_CM_048
    // Mục tiêu: Lưu Part với giá trị null, vẫn gọi repository.save(null) theo hành vi hiện tại
    @Test
    public void savePart_Null() {
        partService.savePart(null);

        verify(partRepository, times(1)).save(isNull());
        verifyNoMoreInteractions(partRepository);
    }

    // UT_CM_049
    // Mục tiêu: Lấy danh sách Part theo Course (phân trang), trả về đúng số lượng và tổng bản ghi
    @Test
    public void getPartLisByCourse_Success() {
        Pageable pageable = PageRequest.of(0, 2);

        List<Part> parts = Arrays.asList(
                new Part(1L, "P1", course),
                new Part(2L, "P2", course)
        );

        Page<Part> page = new PageImpl<>(parts, pageable, 2);

        when(partRepository.findAllByCourseId(1L, pageable)).thenReturn(page);

        Page<Part> result = partService.getPartLisByCourse(pageable, 1L);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());

        verify(partRepository).findAllByCourseId(eq(1L), eq(pageable));
        verifyNoMoreInteractions(partRepository);
    }

    // UT_CM_050
    // Mục tiêu: Lấy danh sách Part theo Course khi không có dữ liệu, trả về Page rỗng nhưng không null
    @Test
    public void getPartLisByCourse_Empty() {
        Pageable pageable = PageRequest.of(0, 10);

        when(partRepository.findAllByCourseId(1L, pageable))
                .thenReturn(Page.empty());

        Page<Part> result = partService.getPartLisByCourse(pageable, 1L);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(partRepository).findAllByCourseId(eq(1L), eq(pageable));
    }

    // UT_CM_051
    // Mục tiêu: Lấy danh sách Part theo Course object, trả về đúng dữ liệu và đúng quan hệ Course
    @Test
    public void getPartListByCourse_Success() {
        when(partRepository.findAllByCourse(course))
                .thenReturn(Arrays.asList(part));

        List<Part> result = partService.getPartListByCourse(course);

        assertEquals(1, result.size());
        assertEquals("Part 1", result.get(0).getName());
        assertSame(course, result.get(0).getCourse());

        verify(partRepository).findAllByCourse(course);
        verifyNoMoreInteractions(partRepository);
    }

    // UT_CM_052
    // Mục tiêu: Tìm Part theo ID thành công, trả về Optional có dữ liệu
    @Test
    public void findPartById_Found() {
        when(partRepository.findById(1L)).thenReturn(Optional.of(part));

        Optional<Part> result = partService.findPartById(1L);

        assertTrue(result.isPresent());
        assertEquals("Part 1", result.get().getName());

        verify(partRepository).findById(1L);
    }

    // UT_CM_053
    // Mục tiêu: Tìm Part theo ID không tồn tại, trả về Optional rỗng
    @Test
    public void findPartById_NotFound() {
        when(partRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Part> result = partService.findPartById(99L);

        assertFalse(result.isPresent());

        verify(partRepository).findById(99L);
    }

    // UT_CM_054
    // Mục tiêu: Kiểm tra tồn tại Part theo ID trả về true khi tồn tại
    @Test
    public void existsById_True() {
        when(partRepository.existsById(1L)).thenReturn(true);

        boolean result = partService.existsById(1L);

        assertTrue(result);

        verify(partRepository).existsById(1L);
    }

    // UT_CM_055
    // Mục tiêu: Kiểm tra tồn tại Part theo ID trả về false khi không tồn tại
    @Test
    public void existsById_False() {
        when(partRepository.existsById(99L)).thenReturn(false);

        boolean result = partService.existsById(99L);

        assertFalse(result);

        verify(partRepository).existsById(99L);
    }
}