package com.thanhtam.backend;

import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.repository.CourseRepository;
import com.thanhtam.backend.service.CourseServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CourseServiceImplTest {

    private CourseRepository courseRepository;
    private CourseServiceImpl courseService;

    private Course course;

    @Before
    public void setUp() {
        courseRepository = mock(CourseRepository.class);
        courseService = new CourseServiceImpl(courseRepository);

        course = new Course(1L, "C001", "Course 1", null, new ArrayList<>());
    }

    // UT_CM_033
    // Mục tiêu: Kiểm tra lấy course theo ID khi tồn tại
    @Test
    public void getCourseById_Found() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Optional<Course> result = courseService.getCourseById(1L);

        assertTrue(result.isPresent());
        assertEquals("C001", result.get().getCourseCode());
        assertEquals(Long.valueOf(1L), result.get().getId());

        verify(courseRepository).findById(1L);
    }

    // UT_CM_034
    // Mục tiêu: Kiểm tra lấy course theo ID không tồn tại
    @Test
    public void getCourseById_NotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Course> result = courseService.getCourseById(99L);

        assertFalse(result.isPresent());

        verify(courseRepository).findById(99L);
    }

    // UT_CM_035
    // Mục tiêu: Kiểm tra lấy danh sách course có dữ liệu
    @Test
    public void getCourseList_Success() {
        when(courseRepository.findAll()).thenReturn(Arrays.asList(course));

        List<Course> result = courseService.getCourseList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("C001", result.get(0).getCourseCode());

        verify(courseRepository).findAll();
    }

    // UT_CM_036
    // Mục tiêu: Kiểm tra danh sách course rỗng
    @Test
    public void getCourseList_Empty() {
        when(courseRepository.findAll()).thenReturn(Collections.<Course>emptyList());

        List<Course> result = courseService.getCourseList();

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(courseRepository).findAll();
    }

    // UT_CM_037
    // Mục tiêu: Kiểm tra phân trang course
    @Test
    public void getCourseListByPage_Success() {
        Pageable pageable = PageRequest.of(0, 2);

        List<Course> courses = Arrays.asList(
                new Course(1L, "C001", "A", null, new ArrayList<>()),
                new Course(2L, "C002", "B", null, new ArrayList<>())
        );

        Page<Course> page = new PageImpl<>(courses, pageable, 5);

        when(courseRepository.findAll(pageable)).thenReturn(page);

        Page<Course> result = courseService.getCourseListByPage(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());

        verify(courseRepository).findAll(pageable);
    }

      // UT_CM_038
    // Mục tiêu: Kiểm tra lưu course thành công
    @Test
    public void saveCourse_Success() {
        courseService.saveCourse(course);

        verify(courseRepository, times(1)).save(course);
    }

        // UT_CM_039
    // Mục tiêu: Kiểm tra xóa course theo ID
    @Test
    public void delete_Success() {
        courseService.delete(1L);

        verify(courseRepository).deleteById(1L);
    }

        // UT_CM_040
    // Mục tiêu: Kiểm tra tồn tại courseCode = true
    @Test
    public void existsByCode_True() {
        when(courseRepository.existsByCourseCode("C001")).thenReturn(true);

        boolean result = courseService.existsByCode("C001");

        assertTrue(result);

        verify(courseRepository).existsByCourseCode("C001");
    }

    // UT_CM_041
    // Mục tiêu: Kiểm tra tồn tại courseCode = false
    @Test
    public void existsByCode_False() {
        when(courseRepository.existsByCourseCode("X999")).thenReturn(false);

        boolean result = courseService.existsByCode("X999");

        assertFalse(result);

        verify(courseRepository).existsByCourseCode("X999");
    }

    // UT_CM_042
    // Mục tiêu: Kiểm tra existsById = true
    @Test
    public void existsById_True() {
        when(courseRepository.existsById(1L)).thenReturn(true);

        boolean result = courseService.existsById(1L);

        assertTrue(result);

        verify(courseRepository).existsById(1L);
    }

    // UT_CM_043
    // Mục tiêu: Kiểm tra existsById = false
    @Test
    public void existsById_False() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        boolean result = courseService.existsById(99L);

        assertFalse(result);

        verify(courseRepository).existsById(99L);
    }

    // UT_CM_044
    // Mục tiêu: Kiểm tra findAllByIntakeId có dữ liệu
    @Test
    public void findAllByIntakeId_Found() {
        when(courseRepository.findAllByIntakeId(1L))
                .thenReturn(Arrays.asList(course));

        List<Course> result = courseService.findAllByIntakeId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(courseRepository).findAllByIntakeId(1L);
    }

    // UT_CM_045
    // Mục tiêu: Kiểm tra findAllByIntakeId rỗng
    @Test
    public void findAllByIntakeId_Empty() {
        when(courseRepository.findAllByIntakeId(99L))
                .thenReturn(Collections.<Course>emptyList());

        List<Course> result = courseService.findAllByIntakeId(99L);

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(courseRepository).findAllByIntakeId(99L);
    }

    // UT_CM_046
    // Mục tiêu: Kiểm tra findCourseByPartId có dữ liệu
    @Test
    public void findCourseByPartId_Found() {
        when(courseRepository.findCourseByPartId(1L)).thenReturn(course);

        Course result = courseService.findCourseByPartId(1L);

        assertNotNull(result);
        assertEquals("C001", result.getCourseCode());

        verify(courseRepository).findCourseByPartId(1L);
    }

    // UT_CM_047
    // Mục tiêu: Kiểm tra findCourseByPartId không tồn tại
    @Test
    public void findCourseByPartId_NotFound() {
        when(courseRepository.findCourseByPartId(99L)).thenReturn(null);

        Course result = courseService.findCourseByPartId(99L);

        assertNull(result);

        verify(courseRepository).findCourseByPartId(99L);
    }
}