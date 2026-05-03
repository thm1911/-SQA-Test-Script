package com.thanhtam.backend;

import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.repository.CourseRepository;
import com.thanhtam.backend.service.CourseServiceImpl;
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

public class CourseServiceImplTest {
    private CourseRepository courseRepository;
    private CourseServiceImpl courseService;

    private Course course1, course2;

    @BeforeEach
    void setUp() {
        // Tạo mock repository và service
        courseRepository = mock(CourseRepository.class);
        courseService = new CourseServiceImpl(courseRepository);

        // Tạo dữ liệu mẫu cho các test
        course1 = new Course(5L, "G03", "Professional Speaking",
                "https://isc-quiz.s3-ap-southeast-1.amazonaws.com/course/Professional_Speaking.jpg",
                new ArrayList<>());

        course2 = new Course(12L, "J01", "Java Back-End",
                "https://isc-quiz.s3-ap-southeast-1.amazonaws.com/course/Java_Back_End.png",
                new ArrayList<>());
    }

    // UT_CM_048
    // Mục tiêu: Kiểm tra hàm lấy course theo id thành công khi tồn tại dữ liệu
    @Test
    void UT_CM_048_getCourseById_Success() {

        // Giả lập repository trả về course tồn tại
        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course1));

        // Gọi service lấy course theo id
        Optional<Course> result = courseService.getCourseById(1L);

        // Kiểm tra kết quả trả về
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(course1.getId(), result.get().getId());
    }

    // UT_CM_049
    // Mục tiêu: Kiểm tra hàm lấy course theo id thất bại khi không tồn tại dữ liệu
    @Test
    void UT_CM_049_getCourseById_NotFound() {
        // Giả lập repository không tìm thấy course
        when(courseRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Gọi service lấy course theo id
        Optional<Course> result = courseService.getCourseById(999L);

        // Kiểm tra kết quả trả về rỗng
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    // UT_CM_050
    // Mục tiêu: Kiểm tra hàm lấy danh sách course thành công
    @Test
    void UT_CM_050_getCourseList_Success() {
        // Giả lập repository trả về danh sách course
        when(courseRepository.findAll())
                .thenReturn(Arrays.asList(course1, course2));

        // Gọi service lấy danh sách course
        List<Course> result = courseService.getCourseList();

        // Kiểm tra kết quả trả về
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(course1.getId(), result.get(0).getId());
        assertEquals(course2.getId(), result.get(1).getId());
    }

    // UT_CM_051
    // Mục tiêu: Kiểm tra hàm lấy danh sách course khi không có dữ liệu
    @Test
    void UT_CM_051_getCourseList_Empty() {
        // Giả lập repository trả về danh sách rỗng
        when(courseRepository.findAll())
                .thenReturn(new ArrayList<>());

        // Gọi service lấy danh sách course
        List<Course> result = courseService.getCourseList();

        // Kiểm tra kết quả trả về
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // UT_CM_052
    // Mục tiêu: Kiểm tra hàm lấy danh sách course có phân trang thành công
    @Test
    void UT_CM_052_getCourseListByPage_Success() {
        // Tạo pageable cho phân trang
        Pageable pageable = PageRequest.of(0, 10);

        // Giả lập repository trả về page course
        Page<Course> page = new PageImpl<>(
                Arrays.asList(course1, course2),
                pageable,
                2
        );

        when(courseRepository.findAll(pageable))
                .thenReturn(page);

        // Gọi service lấy danh sách course theo phân trang
        Page<Course> result = courseService.getCourseListByPage(pageable);

        // Kiểm tra kết quả trả về
        assertNotNull(result);
        // Kiểm tra số lượng course trong page
        assertEquals(2, result.getContent().size());
        // Kiểm tra dữ liệu của course trong page
        assertEquals(course1.getId(), result.getContent().get(0).getId());
        assertEquals(course2.getId(), result.getContent().get(1).getId());
    }

    // UT_CM_053
    // Mục tiêu: Kiểm tra hàm lấy danh sách course theo phân trang khi không có dữ liệu
    @Test
    void UT_CM_053_getCourseListByPage_Empty() {
        // Tạo pageable cho phân trang
        Pageable pageable = PageRequest.of(0, 10);

        // Giả lập repository trả về page rỗng
        Page<Course> page = new PageImpl<>(new ArrayList<>(), pageable, 0);

        when(courseRepository.findAll(pageable))
                .thenReturn(page);

        // Gọi service lấy danh sách course theo phân trang
        Page<Course> result = courseService.getCourseListByPage(pageable);

        // Kiểm tra kết quả trả về: result không null nhưng content rỗng
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // UT_CM_054
    // Mục tiêu: Kiểm tra hàm lưu course thành công khi dữ liệu hợp lệ
    @Test
    void UT_CM_054_saveCourse_Success() {
        // Giả lập repository save course
        when(courseRepository.save(course1))
                .thenReturn(course1);

        // Gọi service lưu course
        courseService.saveCourse(course1);

        // Kiểm tra repository được gọi đúng dữ liệu
        // Do hàm trả kiểu void nên chỉ kiểm tra repository được gọi đúng dữ liệu
        // Chuyển test API sẽ kiểm tra lưu course thành công trong db khi dữ liệu hợp lệ
        verify(courseRepository).save(course1);
    }

    // UT_CM_055
    // Mục tiêu: Kiểm tra hàm lưu course thất bại khi repository xảy ra lỗi
    @Test
    void UT_CM_055_saveCourse_Fail() {
        // Giả lập repository ném exception khi lưu dữ liệu
        doThrow(new RuntimeException("DB ERROR"))
                .when(courseRepository).save(any(Course.class));

        Course course = new Course();
        course.setName("Course");
        course.setCourseCode("C01");
        course.setImgUrl("img.png");

        // Kiểm tra service ném exception khi lưu thất bại
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> courseService.saveCourse(course));

        // Kiểm tra đúng message lỗi
        assertEquals("DB ERROR", ex.getMessage());
    }

    // UT_CM_056
    // Mục tiêu: Kiểm tra hàm deleteCourse gọi repository thành công
    @Test
    void UT_CM_056_deleteCourse_Success() {
        Long courseId = 5L;

        doNothing().when(courseRepository).deleteById(courseId);

        courseService.delete(courseId);

        // Verify repository được gọi đúng 1 lần
        verify(courseRepository, times(1)).deleteById(courseId);
    }

    // UT_CM_057
    // Mục tiêu: Kiểm tra hàm deleteCourse khi repository ném exception
    @Test
    void UT_CM_057_deleteCourse_Fail() {
        Long courseId = 5L;

        // Giả lập repository ném exception khi xóa dữ liệu
        doThrow(new RuntimeException("DELETE ERROR"))
                .when(courseRepository).deleteById(courseId);

        // Kiểm tra service ném exception khi xóa thất bại
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> courseService.delete(courseId));

        // Kiểm tra đúng message lỗi
        assertEquals("DELETE ERROR", ex.getMessage());
    }

    // UT_CM_058
    // Mục tiêu: Kiểm tra hàm existsByCode trả về true khi courseCode tồn tại
    @Test
    void UT_CM_058_existsByCode_True() {
        // Sử dụng courseCode tồn tại trong dữ liệu mẫu
        String code = "G03";

        // Giả lập repository trả về true khi kiểm tra tồn tại courseCode
        when(courseRepository.existsByCourseCode(code))
                .thenReturn(true);

        // Gọi service kiểm tra tồn tại courseCode
        boolean result = courseService.existsByCode(code);
        
        // Kiểm tra kết quả trả về true
        assertTrue(result);
    }

    // UT_CM_059
    // Mục tiêu: Kiểm tra hàm existsByCode trả về false khi courseCode không tồn tại
    @Test
    void UT_CM_059_existsByCode_False() {
        // Sử dụng courseCode không tồn tại trong dữ liệu mẫu
        String code = "NON_EXIST";

        // Giả lập repository trả về false khi kiểm tra tồn tại courseCode
        when(courseRepository.existsByCourseCode(code))
                .thenReturn(false);

        // Gọi service kiểm tra tồn tại courseCode
        boolean result = courseService.existsByCode(code);

        // Kiểm tra kết quả trả về false
        assertFalse(result);
    }

    // UT_CM_060
    // Mục tiêu: Kiểm tra hàm existsById trả về true khi courseId tồn tại
    @Test
    void UT_CM_060_existsById_True() {
        // Sử dụng id tồn tại trong dữ liệu mẫu
        Long id = 5L;

        // Giả lập repository trả về true khi kiểm tra tồn tại courseId
        when(courseRepository.existsById(id))
                .thenReturn(true);

        // Gọi service kiểm tra tồn tại courseId
        boolean result = courseService.existsById(id);

        // Kiểm tra kết quả trả về true
        assertTrue(result);
    }

    // UT_CM_061
    // Mục tiêu: Kiểm tra hàm existsById trả về false khi courseId không tồn tại
    @Test
    void UT_CM_061_existsById_False() {

        Long id = 999L;

        when(courseRepository.existsById(id))
                .thenReturn(false);

        boolean result = courseService.existsById(id);

        assertFalse(result);
    }

    // UT_CM_062
    // Mục tiêu: Kiểm tra service trả về danh sách course khi intakeId hợp lệ và có dữ liệu
    @Test
    void UT_CM_062_findAllByIntakeId_Success() {
        // Giả lập intakeId và danh sách course trả về từ repository
        Long intakeId = 1L;
        List<Course> mockList = Arrays.asList(course1, course2);

        // Giả lập repository trả về danh sách course theo intakeId
        when(courseRepository.findAllByIntakeId(intakeId))
                .thenReturn(mockList);

        // Gọi service lấy danh sách course theo intakeId
        List<Course> result = courseService.findAllByIntakeId(intakeId);

        // Kiểm tra kết quả trả về: không null, đúng số lượng và đúng dữ liệu
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(course1.getId(), result.get(0).getId());
        assertEquals(course2.getId(), result.get(1).getId());
    }

    // UT_CM_063
    // Mục tiêu: Kiểm tra service trả về danh sách rỗng khi không có course theo intakeId
    @Test
    void UT_CM_063_findAllByIntakeId_Empty() {
        // Giả lập intakeId không có course nào
        Long intakeId = 999L;

        // Giả lập repository trả về danh sách rỗng khi tìm theo intakeId
        when(courseRepository.findAllByIntakeId(intakeId))
                .thenReturn(new ArrayList<>());

        // Gọi service lấy danh sách course theo intakeId
        List<Course> result = courseService.findAllByIntakeId(intakeId);

        // Kiểm tra kết quả trả về: không null nhưng danh sách rỗng
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // UT_CM_064
    // Mục tiêu: Kiểm tra service trả về course khi tìm thấy theo partId
    @Test
    void UT_CM_064_findCourseByPartId_Success() {
        // Giả lập partId và course trả về từ repository
        Long partId = 1L;

        // Giả lập repository trả về course khi tìm theo partId
        when(courseRepository.findCourseByPartId(partId))
                .thenReturn(course1);


        // Gọi service lấy course theo partId
        Course result = courseService.findCourseByPartId(partId);

        // Kiểm tra kết quả trả về: không null và đúng dữ liệu
        assertNotNull(result);
        assertEquals(course1.getId(), result.getId());
        assertEquals(course1.getCourseCode(), result.getCourseCode());
    }
}