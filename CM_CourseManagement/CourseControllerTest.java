package com.thanhtam.backend;

import com.thanhtam.backend.controller.CourseController;
import com.thanhtam.backend.dto.PageResult;
import com.thanhtam.backend.dto.ServiceResult;
import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.service.CourseService;
import com.thanhtam.backend.service.S3Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CourseControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CourseControllerTest.class);

    private CourseController courseController;
    private CourseService courseService;
    private S3Services s3Services;

    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        log.info("Init mocks and test data");

        courseService = mock(CourseService.class);
        s3Services = mock(S3Services.class);
        courseController = new CourseController(courseService, s3Services);

        course1 = new Course(5L, "G03", "Professional Speaking",
                "https://isc-quiz.s3-ap-southeast-1.amazonaws.com/course/Professional_Speaking.jpg",
                new ArrayList<>());

        course2 = new Course(12L, "J01", "Java Back-End",
                "https://isc-quiz.s3-ap-southeast-1.amazonaws.com/course/Java_Back_End.png",
                new ArrayList<>());
    }

    // UT_CM_001
    // Mục tiêu: Kiểm tra lấy tất cả khóa học
    @Test
    void getAllCourse_Success() {
        log.info("Start UT_CM_001 - getAllCourse_Success");

        List<Course> list = Arrays.asList(course1, course2);
        when(courseService.getCourseList()).thenReturn(list);

        List<Course> result = courseController.getAllCourse();

        assertEquals(2, result.size());
        verify(courseService, times(1)).getCourseList();

        log.info("End UT_CM_001");
    }

    // UT_CM_002
    // Mục tiêu: Kiểm tra phân trang
    @Test
    void getCourseListByPage_Success() {
        log.info("Start UT_CM_002 - getCourseListByPage_Success");

        Pageable pageable = PageRequest.of(0, 2);
        Page<Course> page = new PageImpl<>(Arrays.asList(course1, course2), pageable, 2);
        when(courseService.getCourseListByPage(pageable)).thenReturn(page);

        PageResult result = courseController.getCourseListByPage(pageable);

        assertNotNull(result);
        assertEquals(2, result.getData().size());
        verify(courseService, times(1)).getCourseListByPage(pageable);

        log.info("End UT_CM_002");
    }

    // UT_CM_003
    // Mục tiêu: Kiểm tra tồn tại mã khóa học
    @Test
    void checkCode_ExistAndNotExist() {
        log.info("Start UT_CM_003 - checkCode_ExistAndNotExist");

        when(courseService.existsByCode("G03")).thenReturn(true);
        when(courseService.existsByCode("NONEXISTENT")).thenReturn(false);

        assertTrue(courseController.checkCode("G03"));
        assertFalse(courseController.checkCode("NONEXISTENT"));

        verify(courseService).existsByCode("G03");
        verify(courseService).existsByCode("NONEXISTENT");

        log.info("End UT_CM_003");
    }

    // UT_CM_004
    // Mục tiêu: Kiểm tra lấy khóa học theo ID
    @Test
    void getCourseById_Found() {
        log.info("Start UT_CM_004 - getCourseById_Found");

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        ResponseEntity<?> response = courseController.getCourseById(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(courseService, times(1)).getCourseById(5L);

        log.info("End UT_CM_004");
    }

    // UT_CM_005
    // Mục tiêu: Kiểm tra tạo mới khóa học thành công
    @Test
    void createCourse_Success() {
        log.info("Start UT_CM_005 - createCourse_Success");

        when(courseService.existsByCode("G03")).thenReturn(false);

        ResponseEntity<Object> response = courseController.createCourse(course1);
        ServiceResult result = (ServiceResult) response.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(201, result.getStatusCode());

        verify(courseService, times(1)).saveCourse(course1);

        log.info("End UT_CM_005");
    }

    // UT_CM_006
    // Mục tiêu: Kiểm tra cập nhật khóa học thành công
    @Test
    void updateCourse_Success() {
        log.info("Start UT_CM_006 - updateCourse_Success");

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        ResponseEntity<?> response = courseController.updateCourse(course1, 5L);
        ServiceResult result = (ServiceResult) response.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(courseService, times(1)).saveCourse(course1);

        log.info("End UT_CM_006");
    }

    // UT_CM_007
    // Mục tiêu: Kiểm tra xóa khóa học thành công
    @Test
    void deleteCourse_Success() {
        log.info("Start UT_CM_007 - deleteCourse_Success");

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        ResponseEntity<?> response = courseController.deleteCourse(5L);
        ServiceResult result = (ServiceResult) response.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(204, result.getStatusCode());

        verify(courseService, times(1)).delete(5L);

        log.info("End UT_CM_007");
    }

    // UT_CM_008
    // Mục tiêu: Kiểm tra lấy theo partId
    @Test
    void getCourseByPart_Found() {
        log.info("Start UT_CM_008 - getCourseByPart_Found");

        when(courseService.findCourseByPartId(1L)).thenReturn(course1);

        Course result = courseController.getCourseByPart(1L);

        assertEquals(Long.valueOf(5L), result.getId());

        verify(courseService, times(1)).findCourseByPartId(1L);

        log.info("End UT_CM_008");
    }

    // UT_CM_009
    // Mục tiêu: Kiểm tra lấy theo intakeId
    @Test
    void findAllByIntakeId_Found() {
        log.info("Start UT_CM_009 - findAllByIntakeId_Found");

        when(courseService.findAllByIntakeId(1L)).thenReturn(Arrays.asList(course1));

        List<Course> result = courseController.findAllByIntakeId(1L);

        assertEquals(1, result.size());
        verify(courseService, times(1)).findAllByIntakeId(1L);

        log.info("End UT_CM_009");
    }

    // UT_CM_010
    // Mục tiêu: Kiểm tra không tìm thấy khóa học theo ID
    @Test
    void getCourseById_NotFound() {
        log.info("Start UT_CM_010 - getCourseById_NotFound");

        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            courseController.getCourseById(999L);
        });

        verify(courseService, times(1)).getCourseById(999L);

        log.info("End UT_CM_010");
    }

    // UT_CM_011
    // Mục tiêu: Kiểm tra update khóa học không tồn tại
    @Test
    void updateCourse_NotFound() {
        log.info("Start UT_CM_011 - updateCourse_NotFound");

        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            courseController.updateCourse(course1, 999L);
        });

        verify(courseService, never()).saveCourse(any());

        log.info("End UT_CM_011");
    }

    // UT_CM_012
    // Mục tiêu: Kiểm tra delete khóa học không tồn tại
    @Test
    void deleteCourse_NotFound() {
        log.info("Start UT_CM_012 - deleteCourse_NotFound");

        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            courseController.deleteCourse(999L);
        });

        verify(courseService, never()).delete(anyLong());

        log.info("End UT_CM_012");
    }

    // UT_CM_013
    // Mục tiêu: Kiểm tra trùng mã khi tạo mới
    @Test
    void createCourse_DuplicateCode() {
        log.info("Start UT_CM_013 - createCourse_DuplicateCode");

        when(courseService.existsByCode("G03")).thenReturn(true);

        ResponseEntity<Object> response = courseController.createCourse(course1);
        ServiceResult result = (ServiceResult) response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(409, result.getStatusCode());

        verify(courseService, never()).saveCourse(any());

        log.info("End UT_CM_013");
    }

    // UT_CM_014
    // Mục tiêu: Kiểm tra validation (truyền null)
    @Test
    void createCourse_ValidationError() {
        log.info("Start UT_CM_014 - createCourse_ValidationError");

        ResponseEntity<Object> response = courseController.createCourse(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verify(courseService, never()).saveCourse(any());

        log.info("End UT_CM_014");
    }

    // UT_CM_015
    // Mục tiêu: Kiểm tra validation update
    @Test
    void updateCourse_ValidationError() {
        log.info("Start UT_CM_015 - updateCourse_ValidationError");

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        Course invalidCourse = new Course();
        invalidCourse.setImgUrl(null);

        assertThrows(NullPointerException.class, () -> {
            courseController.updateCourse(invalidCourse, 5L);
        });

        log.info("End UT_CM_015");
    }

    // UT_CM_016
    // Mục tiêu: Kiểm tra danh sách rỗng
    @Test
    void getAllCourse_Empty() {
        log.info("Start UT_CM_016 - getAllCourse_Empty");

        when(courseService.getCourseList()).thenReturn(new ArrayList<>());

        List<Course> result = courseController.getAllCourse();

        assertTrue(result.isEmpty());

        verify(courseService, times(1)).getCourseList();

        log.info("End UT_CM_016");
    }

    // UT_CM_017
    // Mục tiêu: Kiểm tra phân trang rỗng
    @Test
    void getCourseListByPage_Empty() {
        log.info("Start UT_CM_017 - getCourseListByPage_Empty");

        Pageable pageable = PageRequest.of(0, 10);
        when(courseService.getCourseListByPage(pageable)).thenReturn(Page.empty());

        PageResult result = courseController.getCourseListByPage(pageable);

        assertTrue(result.getData().isEmpty());

        log.info("End UT_CM_017");
    }

    // UT_CM_018
    // Mục tiêu: Kiểm tra part không tồn tại
    @Test
    void getCourseByPart_NotFound() {
        log.info("Start UT_CM_018 - getCourseByPart_NotFound");

        when(courseService.findCourseByPartId(99L)).thenReturn(null);

        Course result = courseController.getCourseByPart(99L);

        assertNull(result);

        verify(courseService, times(1)).findCourseByPartId(99L);

        log.info("End UT_CM_018");
    }

    // UT_CM_019
    // Mục tiêu: Kiểm tra intake không tồn tại
    @Test
    void findAllByIntakeId_NotFound() {
        log.info("Start UT_CM_019 - findAllByIntakeId_NotFound");

        when(courseService.findAllByIntakeId(99L)).thenReturn(new ArrayList<>());

        List<Course> result = courseController.findAllByIntakeId(99L);

        assertTrue(result.isEmpty());

        verify(courseService, times(1)).findAllByIntakeId(99L);

        log.info("End UT_CM_019");
    }

    // UT_CM_020
    // Mục tiêu: Kiểm tra logic check course code
    @Test
    void checkCourseCode_AllCases() {
        log.info("Start UT_CM_020 - checkCourseCode_AllCases");

        when(courseService.existsByCode("G03")).thenReturn(true);
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // case 1: trùng chính nó
        assertFalse(courseController.checkCourseCode("G03", 5L));

        // case 2: tồn tại nhưng khác code
        when(courseService.existsByCode("J01")).thenReturn(true);
        assertTrue(courseController.checkCourseCode("J01", 5L));

        // case 3: không tồn tại
        when(courseService.existsByCode("NEW_CODE")).thenReturn(false);
        assertFalse(courseController.checkCourseCode("NEW_CODE", 5L));

        verify(courseService, atLeastOnce()).existsByCode(anyString());

        log.info("End UT_CM_020");
    }
}