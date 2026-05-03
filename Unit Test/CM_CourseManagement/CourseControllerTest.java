package com.thanhtam.backend;

import com.thanhtam.backend.controller.CourseController;
import com.thanhtam.backend.dto.PageResult;
import com.thanhtam.backend.dto.ServiceResult;
import com.thanhtam.backend.entity.Course;
import com.thanhtam.backend.service.CourseService;
import com.thanhtam.backend.service.S3Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CourseControllerTest {
    private CourseController courseController;
    private CourseService courseService;
    private S3Services s3Services;

    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        // Khởi tạo mock service và controller
        courseService = mock(CourseService.class);
        s3Services = mock(S3Services.class);
        courseController = new CourseController(courseService, s3Services);

        // Tạo dữ liệu mẫu cho các test
        course1 = new Course(5L, "G03", "Professional Speaking",
                "https://isc-quiz.s3-ap-southeast-1.amazonaws.com/course/Professional_Speaking.jpg",
                new ArrayList<>());

        course2 = new Course(12L, "J01", "Java Back-End",
                "https://isc-quiz.s3-ap-southeast-1.amazonaws.com/course/Java_Back_End.png",
                new ArrayList<>());
    }

    // UT_CM_001
    // Mục tiêu: Kiểm tra hàm lấy danh sách môn học thành công
    @Test
    void UT_CM_001_getAllCourse_Success() {
        // Giả lập dữ liệu trả về từ service
        List<Course> list = Arrays.asList(course1, course2);
        when(courseService.getCourseList()).thenReturn(list);

        // Gọi API lấy danh sách course
        List<Course> result = courseController.getAllCourse();

        // Kiểm tra dữ liệu trả về: không null, đúng số lượng, đúng nội dung                        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("G03", result.get(0).getCourseCode());
    }


    // UT_CM_002
    // Mục tiêu: Kiểm tra hàm lấy danh sách môn học trả về rỗng
    @Test
    void UT_CM_002_getAllCourse_Empty() {
        // Giả lập service trả về danh sách rỗng
        when(courseService.getCourseList()).thenReturn(new ArrayList<>());

        // Gọi API lấy danh sách course
        List<Course> result = courseController.getAllCourse();

        // Kiểm tra dữ liệu trả về: không null, rỗng
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    // UT_CM_003
    // Mục tiêu: Kiểm tra hàm lấy danh sách môn học có phân trang
    @Test
    void UT_CM_003_getCourseListByPage_Success() {
        // Giả lập dữ liệu phân trang
        Pageable pageable = PageRequest.of(0, 2);
        Page<Course> page = new PageImpl<>(Arrays.asList(course1, course2), pageable, 2);

        when(courseService.getCourseListByPage(pageable)).thenReturn(page);

        // Gọi API lấy danh sách course theo phân trang
        PageResult result = courseController.getCourseListByPage(pageable);

        // Kiểm tra dữ liệu trả về: không null, đúng số lượng
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());

        // Kiểm tra đúng dữ liệu được trả về
        List<Object> data = result.getData();

        Course c1 = (Course) data.get(0);
        Course c2 = (Course) data.get(1);

        // Kiểm tra đúng dữ liệu được trả về
        assertEquals(course1.getId(), c1.getId());
        assertEquals(course2.getId(), c2.getId());

    }

    // UT_CM_004
    // Mục tiêu: Kiểm tra hàm lấy danh sách course có phân trang khi không có dữ liệu
    @Test
    void UT_CM_004_getCourseListByPage_Empty() {

        // Giả lập service trả về danh sách rỗng
        Pageable pageable = PageRequest.of(0, 10);
        when(courseService.getCourseListByPage(pageable)).thenReturn(Page.empty());

        // Gọi API lấy danh sách course theo phân trang
        PageResult result = courseController.getCourseListByPage(pageable);

        // Kiểm tra dữ liệu trả về không null và danh sách rỗng
        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    // UT_CM_005
    // Mục tiêu: Kiểm tra API cho phép sử dụng courseCode mới khi chưa tồn tại trong hệ thống
    @Test
    void UT_CM_005_checkCourseCode_NonDuplicated() {
        // Giả lập service trả về false vì courseCode chưa tồn tại
        when(courseService.existsByCode("NEW_CODE")).thenReturn(false);

        // Gọi API kiểm tra courseCode
        boolean result = courseController.checkCourseCode("NEW_CODE", 5L);

        // Kiểm tra: trả về false vì không bị trùng
        assertFalse(result);
    }

    // UT_CM_006
    // Mục tiêu: Kiểm tra API chặn courseCode khi đã tồn tại ở course khác
    @Test
    void UT_CM_006_checkCourseCode_Duplicated() {
        // Giả lập courseCode đã tồn tại trong hệ thống
        when(courseService.existsByCode("G04")).thenReturn(true);

        // Giả lập course hiện tại có code khác với G04
        Course otherCourse = new Course();
        otherCourse.setCourseCode("DIFFERENT");
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(otherCourse));

        // Gọi API kiểm tra courseCode
        boolean result = courseController.checkCourseCode("G04", 5L);

        // Kiểm tra: trả về true vì bị trùng với course khác
        assertTrue(result);
    }

    // UT_CM_007
    // Mục tiêu: Kiểm tra API cho phép sử dụng courseCode của chính môn học 
    // (khi cập nhật môn học sẽ không coi courseCode của chính môn học đó là bị trùng)
    @Test
    void UT_CM_007_checkCourseCode_SelfIdentical() {
        // Giả lập courseCode đã tồn tại trong hệ thống
        when(courseService.existsByCode("G03")).thenReturn(true);

        // Giả lập course hiện tại có đúng courseCode G03
        Course currentCourse = new Course();
        currentCourse.setCourseCode("G03");
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(currentCourse));

        // Gọi API kiểm tra courseCode
        boolean result = courseController.checkCourseCode("G03", 5L);

        // Kiểm tra: trả về false vì không coi là trùng với chính nó
        assertFalse(result);
    }

    // UT_CM_008
    // Mục tiêu: Kiểm tra API khi courseId không tồn tại trong hệ thống
    @Test
    void UT_CM_008_checkCourseCode_CourseIdNotFound() {
        // Giả lập courseCode đã tồn tại
        when(courseService.existsByCode("G04")).thenReturn(true);

        // Giả lập course không tồn tại
        when(courseService.getCourseById(5L)).thenReturn(Optional.empty());

        // Gọi API kiểm tra courseCode và kiểm tra ngoại lệ
        assertThrows(NoSuchElementException.class, () ->
                courseController.checkCourseCode("G04", 5L)
        );

    }

    // UT_CM_009
    // Mục tiêu: Kiểm tra hàm validate courseCode (null hoặc rỗng)
    @Test
    void UT_CM_009_checkCourseCode_Validation() {

        // Case 1: code bị null
        assertThrows(IllegalArgumentException.class, () ->
                courseController.checkCourseCode(null, 5L)
        );

        // Case 2: code là chuỗi rỗng
        assertThrows(IllegalArgumentException.class, () ->
                courseController.checkCourseCode("", 5L)
        );

        // Case 3: code là chuỗi chỉ có khoảng trắng
        assertThrows(IllegalArgumentException.class, () ->
                courseController.checkCourseCode("   ", 5L)
        );
    }

    // UT_CM_010
    // Mục tiêu: Kiểm tra hàm checkCode trả về true khi courseCode đã tồn tại
    @Test
    void UT_CM_010_checkCode_Exist() {
        // Giả lập courseCode đã tồn tại trong hệ thống
        when(courseService.existsByCode("G03")).thenReturn(true);

        // Gọi API kiểm tra courseCode
        boolean result = courseController.checkCode("G03");

        // Kiểm tra: trả về true vì courseCode đã tồn tại
        assertTrue(result);
    }

    // UT_CM_011
    // Mục tiêu: Kiểm tra hàm checkCode trả về false khi courseCode không tồn tại
    @Test
    void UT_CM_011_checkCode_NotExist() {
        // Giả lập courseCode không tồn tại trong hệ thống
        when(courseService.existsByCode("NONEXISTENT")).thenReturn(false);

        // Gọi API kiểm tra courseCode
        boolean result = courseController.checkCode("NONEXISTENT");

        // Kiểm tra: trả về false vì courseCode không tồn tại
        assertFalse(result);
    }

    // UT_CM_012
    // Mục tiêu: Kiểm tra API lấy course theo ID thành công khi tồn tại dữ liệu
    @Test
    void UT_CM_012_getCourseById_Found() {
        // Giả lập service trả về course tồn tại
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // Gọi API
        ResponseEntity<?> response = courseController.getCourseById(5L);

        // Lấy body trả về và ép kiểu về Optional<Course>
        Optional<Course> result = (Optional<Course>) response.getBody();

        // Kiểm tra response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);
        assertTrue(result.isPresent());

        // Kiểm tra đúng dữ liệu course
        assertEquals(course1.getId(), result.get().getId());
        assertEquals(course1.getCourseCode(), result.get().getCourseCode());
    }

    // UT_CM_013
    // Mục tiêu: Kiểm tra API lấy course theo ID thất bại khi không tồn tại dữ liệu
    @Test
    void UT_CM_013_getCourseById_NotFound() {
        // Giả lập service trả về Optional.empty() khi courseId không tồn tại
        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        // Gọi API lấy course theo ID và kiểm tra ngoại lệ/
        assertThrows(EntityNotFoundException.class,
                () -> courseController.getCourseById(999L));
    }


    // UT_CM_014
    // Mục tiêu: Kiểm tra API tạo course thành công khi courseCode chưa tồn tại
    @Test
    void UT_CM_014_createCourse_Success() {
        // Giả lập courseCode chưa tồn tại trong hệ thống
        when(courseService.existsByCode("C01")).thenReturn(false);

        Course valid = new Course();
        valid.setName("Course");
        valid.setCourseCode("C01");
        valid.setImgUrl("a.png");

        // Gọi API tạo course
        ResponseEntity<Object> response = courseController.createCourse(valid);
        ServiceResult result = (ServiceResult) response.getBody();

        // Kiểm tra kết quả trả về
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Kiểm tra status code trong body trả về là 201 (Created) vì tạo mới thành công
        assertEquals(201, result.getStatusCode());
        assertNotNull(result.getData());
    }


    // UT_CM_015
    // Mục tiêu: Kiểm tra API từ chối tạo course khi courseCode đã tồn tại
    @Test
    void UT_CM_015_createCourse_DuplicateCode() {
        // Giả lập courseCode đã tồn tại trong hệ thống
        when(courseService.existsByCode("G03")).thenReturn(true);

        // Gọi API tạo course
        ResponseEntity<Object> response = courseController.createCourse(course1);
        ServiceResult result = (ServiceResult) response.getBody();

        // Kiểm tra kết quả trả về
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // Kiểm tra status code trong body trả về là 409 (Conflict) vì trùng courseCose
        assertEquals(409, result.getStatusCode());
        assertEquals(course1.getCourseCode(), result.getData());
    }

    // UT_CM_016
    // Mục tiêu: Kiểm tra API xử lý exception khi service throw lỗi hệ thống
    @Test
    void UT_CM_016_createCourse_Exception() {
        // Giả lập courseCode chưa tồn tại để đi vào logic lưu course
        when(courseService.existsByCode("C01")).thenReturn(false);

        // Giả lập service throw exception khi lưu course
        doThrow(new RuntimeException("DB ERROR"))
                .when(courseService).saveCourse(any());

        Course course = new Course();
        course.setName("Course");
        course.setCourseCode("C01");
        course.setImgUrl("img");

        // Gọi API tạo course và kiểm tra response
        ResponseEntity<Object> response = courseController.createCourse(course);

        // Kiểm tra kết quả trả về
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("DB ERROR"));
    }

    // UT_CM_017
    // Mục tiêu: Kiểm tra API tạo course với validation khi các trường có giá trị null
    @Test
    void UT_CM_017_createCourse_NullValidation() {

        doNothing().when(courseService).saveCourse(any());

        // name null
        Course nameNull = new Course();
        nameNull.setName(null);
        nameNull.setCourseCode("C01");
        nameNull.setImgUrl("img");

        ResponseEntity<Object> res1 = courseController.createCourse(nameNull);
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());

        // courseCode null
        Course codeNull = new Course();
        codeNull.setName("Course");
        codeNull.setCourseCode(null);
        codeNull.setImgUrl("img");

        ResponseEntity<Object> res2 = courseController.createCourse(codeNull);
        assertEquals(HttpStatus.BAD_REQUEST, res2.getStatusCode());

        // imgUrl null (cho phép)
        Course imgNull = new Course();
        imgNull.setName("Course");
        imgNull.setCourseCode("C01");
        imgNull.setImgUrl(null);

        ResponseEntity<Object> res3 = courseController.createCourse(imgNull);
        assertEquals(HttpStatus.OK, res3.getStatusCode());
    }

    // UT_CM_018
    // Mục tiêu: Kiểm tra API tạo course với validation khi các trường là chuỗi rỗng
    @Test
    void UT_CM_018_createCourse_EmptyValidation() {

        doNothing().when(courseService).saveCourse(any());

        // name rỗng
        Course nameEmpty = new Course();
        nameEmpty.setName("");
        nameEmpty.setCourseCode("C01");
        nameEmpty.setImgUrl("img");

        ResponseEntity<Object> res1 = courseController.createCourse(nameEmpty);
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());

        // courseCode rỗng
        Course codeEmpty = new Course();
        codeEmpty.setName("Course");
        codeEmpty.setCourseCode("");
        codeEmpty.setImgUrl("img");

        ResponseEntity<Object> res2 = courseController.createCourse(codeEmpty);
        assertEquals(HttpStatus.BAD_REQUEST, res2.getStatusCode());

        // imgUrl rỗng
        Course imgEmpty = new Course();
        imgEmpty.setName("Course");
        imgEmpty.setCourseCode("C01");
        imgEmpty.setImgUrl("");

        ResponseEntity<Object> res3 = courseController.createCourse(imgEmpty);
        assertEquals(HttpStatus.BAD_REQUEST, res3.getStatusCode());
    }

    // UT_CM_019
    // Mục tiêu: Kiểm tra API tạo course với validation khi các trường chỉ chứa khoảng trắng
    @Test
    void UT_CM_019_createCourse_SpaceValidation() {

        doNothing().when(courseService).saveCourse(any());

        // name khoảng trắng
        Course nameSpace = new Course();
        nameSpace.setName("   ");
        nameSpace.setCourseCode("C01");
        nameSpace.setImgUrl("img");

        ResponseEntity<Object> res1 = courseController.createCourse(nameSpace);
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());

        // courseCode khoảng trắng
        Course codeSpace = new Course();
        codeSpace.setName("Course");
        codeSpace.setCourseCode("   ");
        codeSpace.setImgUrl("img");

        ResponseEntity<Object> res2 = courseController.createCourse(codeSpace);
        assertEquals(HttpStatus.BAD_REQUEST, res2.getStatusCode());

        // imgUrl khoảng trắng
        Course imgSpace = new Course();
        imgSpace.setName("Course");
        imgSpace.setCourseCode("C01");
        imgSpace.setImgUrl("   ");

        ResponseEntity<Object> res3 = courseController.createCourse(imgSpace);
        assertEquals(HttpStatus.BAD_REQUEST, res3.getStatusCode());
    }

    // UT_CM_020
    // Mục tiêu: Kiểm tra API cập nhật course thành công khi tồn tại course và dữ liệu hợp lệ
    @Test
    void UT_CM_020_updateCourse_Success() {

        // Giả lập course tồn tại trong DB
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // Dữ liệu cập nhật
        Course input = new Course();
        input.setName("New Name");
        input.setCourseCode("G03");
        input.setImgUrl("new-img.png");

        // Gọi API cập nhật course
        ResponseEntity<?> response = courseController.updateCourse(input, 5L);
        ServiceResult result = (ServiceResult) response.getBody();
        Course updated = (Course) result.getData();

        // Kiểm tra response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);

        // Kiểm tra dữ liệu update
        assertEquals("New Name", updated.getName());
        assertEquals("G03", updated.getCourseCode());
        assertEquals("new-img.png", updated.getImgUrl());
    }

    // UT_CM_021
    // Mục tiêu: Kiểm tra API cập nhật course khi courseId không tồn tại
    @Test
    void UT_CM_021_updateCourse_NotFound() {

        // Giả lập không tìm thấy course
        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        Course input = new Course();
        input.setName("Test");
        input.setCourseCode("C01");
        input.setImgUrl("img");

        // Kiểm tra exception
        assertThrows(EntityNotFoundException.class,
                () -> courseController.updateCourse(input, 999L));
    }

    // UT_CM_022
    // Mục tiêu: Kiểm tra API cập nhật course giữ lại imgUrl cũ khi client truyền rỗng
    @Test
    void UT_CM_022_updateCourse_KeepOldImgUrl() {

        // Giả lập course trong DB
        Course oldCourse = new Course(5L, "G03", "Old Name", "old-img.png", new ArrayList<>());
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(oldCourse));

        // Input chỉ update name, imgUrl rỗng
        Course input = new Course();
        input.setName("New Name");
        input.setCourseCode("G03");
        input.setImgUrl("");

        ResponseEntity<?> response = courseController.updateCourse(input, 5L);
        ServiceResult result = (ServiceResult) response.getBody();
        Course updated = (Course) result.getData();

        // Kiểm tra giữ lại ảnh cũ
        assertEquals("old-img.png", updated.getImgUrl());
        assertEquals("New Name", updated.getName());
    }

    // UT_CM_023
    // Mục tiêu: Kiểm tra API cập nhật course khi dữ liệu đầu vào có giá trị null
    @Test
    void UT_CM_023_updateCourse_NullValidation() {

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // name null
        Course nameNull = new Course();
        nameNull.setName(null);
        nameNull.setCourseCode("G03");
        nameNull.setImgUrl("img.png");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(nameNull, 5L));

        // courseCode null
        Course codeNull = new Course();
        codeNull.setName("Course");
        codeNull.setCourseCode(null);
        codeNull.setImgUrl("img.png");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(codeNull, 5L));

        // imgUrl null (cho phép)
        Course imgNull = new Course();
        imgNull.setName("Course");
        imgNull.setCourseCode("G03");
        imgNull.setImgUrl(null);

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(imgNull, 5L));
    }

    // UT_CM_024
    // Mục tiêu: Kiểm tra API cập nhật course khi dữ liệu đầu vào là chuỗi rỗng
    @Test
    void UT_CM_024_updateCourse_EmptyValidation() {

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // name empty
        Course nameEmpty = new Course();
        nameEmpty.setName("");
        nameEmpty.setCourseCode("G03");
        nameEmpty.setImgUrl("img.png");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(nameEmpty, 5L));

        // courseCode empty
        Course codeEmpty = new Course();
        codeEmpty.setName("Course");
        codeEmpty.setCourseCode("");
        codeEmpty.setImgUrl("img.png");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(codeEmpty, 5L));
    }

    // UT_CM_025
    // Mục tiêu: Kiểm tra API cập nhật course khi dữ liệu chỉ chứa khoảng trắng
    @Test
    void UT_CM_025_updateCourse_BlankValidation() {

        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // name blank
        Course nameBlank = new Course();
        nameBlank.setName("   ");
        nameBlank.setCourseCode("G03");
        nameBlank.setImgUrl("img.png");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(nameBlank, 5L));

        // courseCode blank
        Course codeBlank = new Course();
        codeBlank.setName("Course");
        codeBlank.setCourseCode("   ");
        codeBlank.setImgUrl("img.png");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(codeBlank, 5L));

        // imgUrl blank
        Course imgBlank = new Course();
        imgBlank.setName("Course");
        imgBlank.setCourseCode("G03");
        imgBlank.setImgUrl("   ");

        assertThrows(IllegalArgumentException.class,
                () -> courseController.updateCourse(imgBlank, 5L));
    }

    // UT_CM_026
    // Mục tiêu: Kiểm tra API xóa course thành công khi course tồn tại
    @Test
    void UT_CM_026_deleteCourse_Success() {
        // Giả lập course tồn tại
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // Gọi API xóa course
        ResponseEntity<?> response = courseController.deleteCourse(5L);
        ServiceResult result = (ServiceResult) response.getBody();

        // Kiểm tra response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);

        // Kiểm tra nội dung trả về
        // Kiểm tra mã trạng thái trong body trả về là 204 (No Content) vì xóa thành công
        assertEquals(204, result.getStatusCode());
        assertEquals("Deleted course with id: 5 successfully!", result.getMessage());
        assertNull(result.getData());
    }

    // UT_CM_027
    // Mục tiêu: Kiểm tra API xóa course khi courseId không tồn tại
    @Test
    void UT_CM_027_deleteCourse_NotFound() {
        // Giả lập không tìm thấy course
        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        // Kiểm tra exception khi không tồn tại course
        assertThrows(EntityNotFoundException.class,
                () -> courseController.deleteCourse(999L));

    }

    // UT_CM_028
    // Mục tiêu: Kiểm tra API lấy course theo partId thành công khi dữ liệu tồn tại
    @Test
    void UT_CM_028_getCourseByPart_Success() {
        // Giả lập service trả về course theo partId
        when(courseService.findCourseByPartId(1L)).thenReturn(course1);

        // Gọi API lấy course theo partId
        Course result = courseController.getCourseByPart(1L);

        // Kiểm tra dữ liệu trả về
        assertNotNull(result);
        assertEquals(course1.getId(), result.getId());
        assertEquals(course1.getCourseCode(), result.getCourseCode());
        assertEquals(course1.getName(), result.getName());
    }

    // UT_CM_029
    // Mục tiêu: Kiểm tra API trả về null khi không tìm thấy course theo partId
    @Test
    void UT_CM_029_getCourseByPart_NotFound() {
        // Giả lập service không tìm thấy course
        when(courseService.findCourseByPartId(999L)).thenReturn(null);

        // Gọi API lấy course theo partId
        Course result = courseController.getCourseByPart(999L);

        // Kiểm tra kết quả trả về
        assertNull(result);
    }

    // UT_CM_030
    // Mục tiêu: Kiểm tra API lấy danh sách course theo intakeId khi có dữ liệu
    @Test
    void UT_CM_030_findAllByIntakeId_Success() {

        // Giả lập service trả về danh sách course
        when(courseService.findAllByIntakeId(1L))
                .thenReturn(Arrays.asList(course1, course2));

        // Gọi API lấy danh sách course theo intakeId
        List<Course> result = courseController.findAllByIntakeId(1L);

        // Kiểm tra kết quả trả về không null và đúng số lượng
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(course1.getCourseCode(), result.get(0).getCourseCode());
        assertEquals(course2.getCourseCode(), result.get(1).getCourseCode());
    }

    // UT_CM_031
    // Mục tiêu: Kiểm tra API lấy danh sách course theo intakeId khi không có dữ liệu
    @Test
    void UT_CM_031_findAllByIntakeId_Empty() {

        // Giả lập service trả về danh sách rỗng
        when(courseService.findAllByIntakeId(999L))
                .thenReturn(new ArrayList<>());

        // Gọi API lấy danh sách course theo intakeId
        List<Course> result = courseController.findAllByIntakeId(999L);

        // Kiểm tra kết quả trả về không null và danh sách rỗng
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}