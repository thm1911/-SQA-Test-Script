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

        assertEquals(2, result.getData().size());
        verify(partService, times(1)).getPartLisByCourse(pageable, 1L);
    }

    // UT_CM_022
    // Kiểm tra lấy danh sách part theo course không phân trang
    @Test
    void getPartListByCourseWithoutPagination_Success() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));
        when(partService.getPartListByCourse(course)).thenReturn(Arrays.asList(part1, part2));

        List<Part> result = partController.getPartListByCourse(1L);

        assertEquals(2, result.size());
        verify(partService, times(1)).getPartListByCourse(course);
    }

    // UT_CM_023
    // Kiểm tra lấy part theo ID thành công
    @Test
    void getPartById_Found() {
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        ResponseEntity<?> response = partController.getPartById(1L);

        assertNotNull(response.getBody());
        verify(partService, times(1)).findPartById(1L);
    }

    // UT_CM_024
    // Kiểm tra lấy part theo ID không tồn tại
    @Test
    void getPartById_NotFound() {
        when(partService.findPartById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            partController.getPartById(99L);
        });

        verify(partService, times(1)).findPartById(99L);
    }

    // UT_CM_025
    // Kiểm tra update tên part thành công
    @Test
    void updatePartName_Success() {
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        ResponseEntity<?> response = partController.updatePartName(1L, "Updated");

        Part updated = (Part) response.getBody();

        assertEquals("Updated", updated.getName());
        verify(partService, times(1)).savePart(part1);
    }

    // UT_CM_026
    // Kiểm tra update part không tồn tại
    @Test
    void updatePartName_NotFound() {
        when(partService.findPartById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            partController.updatePartName(99L, "Name");
        });
    }

    // UT_CM_027
    // Kiểm tra tạo part theo course thành công
    @Test
    void createPartByCourse_Success() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));

        Part newPart = new Part();
        newPart.setName("New Part");

        partController.createPartByCourse(newPart, 1L);

        verify(partService, times(1)).savePart(newPart);
        assertEquals(course, newPart.getCourse());
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
    }

    // UT_CM_029
    // Kiểm tra danh sách part rỗng
    @Test
    void getPartListByCourse_Empty() {
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course));
        when(partService.getPartListByCourse(course)).thenReturn(Collections.emptyList());

        List<Part> result = partController.getPartListByCourse(1L);

        assertTrue(result.isEmpty());
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
}