package com.thanhtam.backend;

import com.thanhtam.backend.controller.PartController;
import com.thanhtam.backend.dto.PageResult;
import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.entity.Part;
import com.thanhtam.backend.service.CourseService;
import com.thanhtam.backend.service.PartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PartControllerTest {

    private PartService partService;
    private CourseService courseService;
    private PartController partController;

    private Course course;
    private Part part1;
    private Part part2;

    @BeforeEach
    void setUp() {
        partService = mock(PartService.class);
        courseService = mock(CourseService.class);
        partController = new PartController(partService, courseService);

        course = new Course(1L, "C001", "Course 1", null, new ArrayList<>());
        part1 = new Part(1L, "Part 1", course);
        part2 = new Part(2L, "Part 2", course);
    }

    // UT_CM_021
    // Kiểm tra lấy danh sách part theo course có phân trang
    @Test
    void getPartListByCourse_Success() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Part> page = new PageImpl<>(Arrays.asList(part1, part2), pageable, 5);

        when(partService.getPartLisByCourse(pageable, 1L)).thenReturn(page);

        PageResult result = partController.getPartListByCourse(pageable, 1L);

        // kiểm tra dữ liệu trả về
        assertNotNull(result);
        assertEquals(2, result.getData().size());
        assertEquals(Long.valueOf(5), result.getPaginationDetails().getTotalCount());

        // kiểm tra service được gọi
        verify(partService).getPartLisByCourse(pageable, 1L);
    }

    // UT_CM_022
    // Kiểm tra lấy danh sách part theo course không phân trang
    @Test
    void getPartListByCourseWithoutPagination_Success() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));
        when(partService.getPartListByCourse(course)).thenReturn(Arrays.asList(part1, part2));

        List<Part> result = partController.getPartListByCourse(1L);

        // kiểm tra dữ liệu
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(part1, result.get(0));

        // verify luồng gọi
        verify(courseService).getCourseById(1L);
        verify(partService).getPartListByCourse(course);
    }

    // UT_CM_023
    // Kiểm tra lấy part theo ID thành công
    @Test
    void getPartById_Found() {
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        ResponseEntity<?> response = partController.getPartById(1L);

        assertEquals(200, response.getStatusCodeValue());

        Optional<?> body = (Optional<?>) response.getBody();

        assertTrue(body.isPresent());
        Part result = (Part) body.get();

        assertEquals(part1.getId(), result.getId());
        assertEquals(part1.getName(), result.getName());

        verify(partService, times(1)).findPartById(1L);
    }

    // UT_CM_024
    // Kiểm tra lấy part theo ID không tồn tại
    @Test
    void getPartById_NotFound() {
        when(partService.findPartById(99L)).thenReturn(Optional.empty());

        // phải ném exception đúng theo requirement
        assertThrows(EntityNotFoundException.class, () -> {
            partController.getPartById(99L);
        });

        verify(partService).findPartById(99L);
    }

    // UT_CM_025
    // Kiểm tra update tên part thành công
    @Test
    void updatePartName_Success() {
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        ResponseEntity<?> response = partController.updatePartName(1L, "Updated");

        // kiểm tra response
        assertEquals(200, response.getStatusCodeValue());

        Part updated = (Part) response.getBody();

        // kiểm tra đúng object được update
        assertSame(part1, updated);
        assertEquals("Updated", updated.getName());

        verify(partService).savePart(part1);
    }

    // UT_CM_026
    // Kiểm tra update part không tồn tại
    @Test
    void updatePartName_NotFound() {
        when(partService.findPartById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            partController.updatePartName(99L, "Name");
        });

        verify(partService, times(1)).findPartById(99L);
        verify(partService, never()).savePart(any());
    }

    // UT_CM_027
    // Kiểm tra tạo part theo course thành công
    @Test
    void createPartByCourse_Success() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));

        Part newPart = new Part();
        newPart.setName("New Part");

        partController.createPartByCourse(newPart, 1L);

        // kiểm tra course được set đúng
        assertSame(course, newPart.getCourse());

        // verify gọi service
        verify(courseService).getCourseById(1L);
        verify(partService).savePart(newPart);
    }

    // UT_CM_028
    // Kiểm tra tạo part khi course không tồn tại
    @Test
    void createPartByCourse_CourseNotFound() {
        when(courseService.getCourseById(99L)).thenReturn(Optional.empty());

        Part newPart = new Part();

        assertThrows(NoSuchElementException.class, () -> {
            partController.createPartByCourse(newPart, 99L);
        });

        verify(courseService, times(1)).getCourseById(99L);
        verify(partService, never()).savePart(any());
    }

    // UT_CM_029
    // Kiểm tra danh sách part rỗng
    @Test
    void getPartListByCourse_Empty() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));
        when(partService.getPartListByCourse(course)).thenReturn(Collections.emptyList());

        List<Part> result = partController.getPartListByCourse(1L);

        // kiểm tra list rỗng
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(partService).getPartListByCourse(course);
    }

    // UT_CM_030
    // Kiểm tra course không tồn tại khi lấy list part
    @Test
    void getPartListByCourseWithoutPagination_CourseNotFound() {
        when(courseService.getCourseById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            partController.getPartListByCourse(99L);
        });
    }

    // UT_CM_031
    // Kiểm tra validation khi update tên part
    @Test
    void updatePartName_ValidationError() {
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        // case 1: name = null
        assertThrows(IllegalArgumentException.class, () -> {
            partController.updatePartName(1L, null);
        });

        // case 2: name rỗng
        assertThrows(IllegalArgumentException.class, () -> {
            partController.updatePartName(1L, "");
        });

        // case 3: name chỉ có khoảng trắng
        assertThrows(IllegalArgumentException.class, () -> {
            partController.updatePartName(1L, "   ");
        });

        // đảm bảo không gọi save khi dữ liệu sai
        verify(partService, never()).savePart(any());
    }

    // UT_CM_032
    // Kiểm tra validation khi tạo part
    @Test
    void createPartByCourse_ValidationError() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));

        // case 1: name = null
        Part caseNull = new Part();
        caseNull.setName(null);

        assertThrows(IllegalArgumentException.class, () -> {
            partController.createPartByCourse(caseNull, 1L);
        });

        // case 2: name = ""
        Part caseEmpty = new Part();
        caseEmpty.setName("");

        assertThrows(IllegalArgumentException.class, () -> {
            partController.createPartByCourse(caseEmpty, 1L);
        });

        // case 3: name = "   "
        Part caseSpace = new Part();
        caseSpace.setName("   ");

        assertThrows(IllegalArgumentException.class, () -> {
            partController.createPartByCourse(caseSpace, 1L);
        });

        verify(partService, never()).savePart(any());
    }
}