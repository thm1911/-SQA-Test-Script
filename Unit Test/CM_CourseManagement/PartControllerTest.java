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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PartControllerTest {

    private PartService partService;
    private CourseService courseService;
    private PartController partController;

    private Course course1;
    private Part part1;
    private Part part2;

    @BeforeEach
    void setUp() {
        // Khởi tạo mock và controller
        partService = mock(PartService.class);
        courseService = mock(CourseService.class);
        partController = new PartController(partService, courseService);

        // Tạo dữ liệu mẫu
        course1 = new Course(1L, "C001", "Course 1", null, new ArrayList<>());
        part1 = new Part(1L, "Part 1", course1);
        part2 = new Part(2L, "Part 2", course1);
    }

    // UT_CM_032
    // Mục tiêu: Kiểm tra API lấy danh sách part theo courseId thành công khi có dữ liệu
    @Test
    void UT_CM_032_getPartListByCourse_Success() {
        // Giả lập dữ liệu phân trang
        Pageable pageable = PageRequest.of(0, 10);

        Page<Part> page = new PageImpl<>(Arrays.asList(part1, part2), pageable, 2);

        when(partService.getPartLisByCourse(pageable, 5L)).thenReturn(page);

        // Gọi API lấy danh sách part theo courseId
        PageResult result = partController.getPartListByCourse(pageable, 5L);

        // Kiểm tra response: result không null, data không null và có đúng 2 phần tử
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());

        List<Object> data = result.getData();

        Part p1 = (Part) data.get(0);
        Part p2 = (Part) data.get(1);

        // Kiểm tra đúng dữ liệu được trả về
        assertEquals(part1.getId(), p1.getId());
        assertEquals(part2.getId(), p2.getId());
    }

    // UT_CM_033
    // Mục tiêu: Kiểm tra API trả về danh sách rỗng khi course không có part hoặc courseId không tồn tại
    @Test
    void UT_CM_033_getPartListByCourse_Empty() {
        // Giả lập dữ liệu phân trang
        Pageable pageable = PageRequest.of(0, 10);

        Page<Part> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(partService.getPartLisByCourse(pageable, 999L)).thenReturn(emptyPage);

        // Gọi API lấy danh sách part theo courseId
        PageResult result = partController.getPartListByCourse(pageable, 999L);

        // Kiểm tra response: result không null, data không null nhưng rỗng
        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    // UT_CM_034
    // Mục tiêu: Kiểm tra API lấy danh sách part theo courseId thành công khi có dữ liệu
    @Test
    void UT_CM_034_getPartListByCourse_Success() {
        // Giả lập course tồn tại
        when(courseService.getCourseById(5L)).thenReturn(Optional.of(course1));

        // Giả lập danh sách part
        when(partService.getPartListByCourse(course1))
                .thenReturn(Arrays.asList(part1, part2));

        // Gọi API
        List<Part> result = partController.getPartListByCourse(5L);

        // Kiểm tra kết quả trả về
        assertNotNull(result);
        assertEquals(2, result.size());

        // Kiểm tra đúng dữ liệu
        assertEquals(part1.getId(), result.get(0).getId());
        assertEquals(part2.getId(), result.get(1).getId());
    }

    // UT_CM_035
    // Mục tiêu: Ném exception khi courseId không tồn tại
    @Test
    void UT_CM_035_getPartListByCourse_CourseNotFound() {
        // Giả lập không tìm thấy course
        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        // Kiểm tra exception
        assertThrows(NoSuchElementException.class,
                () -> partController.getPartListByCourse(999L));

        // Verify chỉ gọi service đầu tiên
        verify(courseService).getCourseById(999L);
        verify(partService, never()).getPartListByCourse(any());
    }

    // UT_CM_036
    // Mục tiêu: Kiểm tra API lấy part theo id thành công khi tồn tại dữ liệu
    @Test
    void UT_CM_036_getPartById_Success() {

        // Giả lập service trả về part
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        // Gọi API
        ResponseEntity<?> response = partController.getPartById(1L);

        // Lấy body
        Optional<Part> result = (Optional<Part>) response.getBody();

        // Kiểm tra response có status 200 và body không null, có dữ liệu
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(result);
        assertTrue(result.isPresent());

        // Kiểm tra dữ liệu đúng
        assertEquals(part1.getId(), result.get().getId());
        assertEquals(part1.getName(), result.get().getName());
    }

    // UT_CM_037
    // Mục tiêu: Kiểm tra API lấy part theo id ném exception khi không tìm thấy part
    @Test
    void UT_CM_037_getPartById_NotFound() {

        // Giả lập không tìm thấy part
        when(partService.findPartById(999L)).thenReturn(Optional.empty());

        // Kiểm tra exception
        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> partController.getPartById(999L)
        );

        // Kiểm tra message
        assertEquals("Not found with part id: 999", ex.getMessage());
    }

    // UT_CM_038
    // Mục tiêu: Kiểm tra API cập nhật tên part thành công khi dữ liệu hợp lệ
    @Test
    void UT_CM_038_updatePartName_Success() {

        // Giả lập part tồn tại trong hệ thống
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        // Dữ liệu tên mới
        String name = "New Part Name";

        // Gọi API cập nhật tên part
        ResponseEntity<?> response =
                partController.updatePartName(1L, name);

        Part result = (Part) response.getBody();

        // Kiểm tra response HTTP
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Kiểm tra dữ liệu sau cập nhật
        assertNotNull(result);
        assertEquals("New Part Name", result.getName());
    }

    // UT_CM_039
    // Mục tiêu: Kiểm tra API cập nhật tên part ném exception khi part không tồn tại
    @Test
    void UT_CM_039_updatePartName_NotFound() {

        // Giả lập không tìm thấy part theo id
        when(partService.findPartById(999L)).thenReturn(Optional.empty());

        // Kiểm tra exception khi update part không tồn tại
        assertThrows(NoSuchElementException.class,
                () -> partController.updatePartName(999L, "New Name"));
    }

    // UT_CM_040
    // Mục tiêu: Kiểm tra API cập nhật tên part ném exception khi tên null
    @Test
    void UT_CM_040_updatePartName_NameNull() {
        // Giả lập part tồn tại
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        // Kiểm tra exception khi name null
        assertThrows(NullPointerException.class,
                () -> partController.updatePartName(1L, null));
    }

    // UT_CM_041
    // Mục tiêu: Kiểm tra API cập nhật tên part ném exception khi tên rỗng
    @Test
    void UT_CM_041_updatePartName_NameEmpty() {
        // Giả lập part tồn tại
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        // Kiểm tra exception khi name rỗng
        assertThrows(IllegalArgumentException.class,
                () -> partController.updatePartName(1L, ""));
    }

    // UT_CM_042
    // Mục tiêu: Kiểm tra API cập nhật tên part ném exception khi tên chỉ chứa khoảng trắng
    @Test
    void UT_CM_042_updatePartName_NameBlank() {
        // Giả lập part tồn tại
        when(partService.findPartById(1L)).thenReturn(Optional.of(part1));

        // Kiểm tra exception khi name chỉ có khoảng trắng
        assertThrows(IllegalArgumentException.class,
                () -> partController.updatePartName(1L, "   "));
    }

    // UT_CM_043
    // Mục tiêu: Kiểm tra API tạo part thành công khi course tồn tại và dữ liệu hợp lệ
    @Test
    void UT_CM_043_createPartByCourse_Success() {

        // Giả lập course tồn tại
        when(courseService.getCourseById(1L))
                .thenReturn(Optional.of(course1));

        Part part = new Part();
        part.setName("Part 1");

        // Gọi API
        partController.createPartByCourse(part, 1L);

        // Kiểm tra part được gán course đúng
        assertEquals(course1, part.getCourse());

        // Do code trả về kiểu void, nên chỉ có thể kiểm tra được là service có đuợc gọi hay không
        // Chuyển cho test API sẽ kiểm tra dữ liệu có được lưu vào db chưa
        verify(partService).savePart(part);
    }

    // UT_CM_044
    // Mục tiêu: Kiểm tra API tạo part ném exception khi course không tồn tại
    @Test
    void UT_CM_044_createPartByCourse_CourseNotFound() {

        // Giả lập course không tồn tại
        when(courseService.getCourseById(999L)).thenReturn(Optional.empty());

        Part part = new Part();
        part.setName("Part 1");

        // Kiểm tra exception
        assertThrows(NoSuchElementException.class,
                () -> partController.createPartByCourse(part, 999L));
    }

    // UT_CM_045
    // Mục tiêu: Kiểm tra API tạo part ném exception khi tên null
    @Test
    void UT_CM_045_createPartByCourse_NullName() {
        // Giả lập course tồn tại   
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course1));

        Part part = new Part();
        part.setName(null);

        // Kiểm tra exception khi name null
        assertThrows(NullPointerException.class,
                () -> partController.createPartByCourse(part, 1L));
    }

    // UT_CM_046
    // Mục tiêu: Kiểm tra API tạo part ném exception khi tên rỗng
    @Test
    void UT_CM_046_createPartByCourse_EmptyName() {
        // Giả lập course tồn tại
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course1));

        Part part = new Part();
        part.setName("");

        // Kiểm tra exception khi name rỗng
        assertThrows(IllegalArgumentException.class,
                () -> partController.createPartByCourse(part, 1L));
    }

    // UT_CM_047
    // Mục tiêu: Kiểm tra API tạo part ném exception khi tên chỉ chứa khoảng trắng
    @Test
    void UT_CM_047_createPartByCourse_BlankName() {
        // Giả lập course tồn tại
        when(courseService.getCourseById(1L)).thenReturn(Optional.of(course1));

        Part part = new Part();
        part.setName("   ");

        // Kiểm tra exception khi name chỉ có khoảng trắng
        assertThrows(IllegalArgumentException.class,
                () -> partController.createPartByCourse(part, 1L));
    }
}