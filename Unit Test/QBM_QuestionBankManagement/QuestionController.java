package com.thanhtam.backend;

import com.thanhtam.backend.controller.QuestionController;
import com.thanhtam.backend.dto.PageResult;
import com.thanhtam.backend.dto.ServiceResult;
import com.thanhtam.backend.entity.Part;
import com.thanhtam.backend.entity.Question;
import com.thanhtam.backend.entity.QuestionType;
import com.thanhtam.backend.entity.Role;
import com.thanhtam.backend.entity.User;
import com.thanhtam.backend.service.PartService;
import com.thanhtam.backend.service.QuestionService;
import com.thanhtam.backend.service.QuestionTypeService;
import com.thanhtam.backend.service.RoleService;
import com.thanhtam.backend.service.UserService;
import com.thanhtam.backend.ultilities.EQTypeCode;
import com.thanhtam.backend.ultilities.ERole;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class QuestionControllerTest {

    QuestionService mockQuestionService = mock(QuestionService.class);
    PartService mockPartService = mock(PartService.class);
    QuestionTypeService mockQuestionTypeService = mock(QuestionTypeService.class);
    UserService mockUserService = mock(UserService.class);
    RoleService mockRoleService = mock(RoleService.class);

    // Khởi tạo controller
    QuestionController questionController = new QuestionController(
            mockQuestionService,
            mockPartService,
            mockQuestionTypeService,
            mockUserService,
            mockRoleService
    );

    @BeforeEach
    void setUp() {
        // Đặt lại trạng thái mock trước mỗi test để tránh ảnh hưởng chéo giữa các test case.
        reset(mockQuestionService, mockPartService, mockQuestionTypeService, mockUserService, mockRoleService);
    }

    // Test Case ID: UT_QBM_001
    // Kiểm tra lấy danh sách câu hỏi thành công
    @Test
    void getAllQuestion_Success() {
        // Tạo dữ liệu question giả lập
        Question firstQuestion = new Question();
        firstQuestion.setId(1L);
        firstQuestion.setQuestionText("Question 1");
        Question secondQuestion = new Question();
        secondQuestion.setId(2L);
        secondQuestion.setQuestionText("Question 2");
        List<Question> expectedQuestionList = Arrays.asList(firstQuestion, secondQuestion);

        when(mockQuestionService.getQuestionList()).thenReturn(expectedQuestionList);

        ResponseEntity<ServiceResult> responseEntity = questionController.getAllQuestion();

        verify(mockQuestionService).getQuestionList();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServiceResult responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        assertEquals(HttpStatus.OK.value(), responseBody.getStatusCode());
        assertEquals("Get question bank successfully!", responseBody.getMessage());

        List<Question> actualQuestionList = (List<Question>) responseBody.getData();
        assertNotNull(actualQuestionList);
        assertEquals(2, actualQuestionList.size());
        assertEquals(1L, actualQuestionList.get(0).getId());
        assertEquals("Question 1", actualQuestionList.get(0).getQuestionText());
        assertEquals(2L, actualQuestionList.get(1).getId());
        assertEquals("Question 2", actualQuestionList.get(1).getQuestionText());

        log.info(responseBody.getStatusCode() + "");
        log.info(responseBody.getMessage());
    }

    // Test Case ID: UT_QBM_002
    // Kiểm tra lấy danh sách câu hỏi trống
    @Test
    void getAllQuestion_EmptyList() {
        List<Question> expectedQuestionList = Collections.emptyList();
        when(mockQuestionService.getQuestionList()).thenReturn(expectedQuestionList);

        ResponseEntity<ServiceResult> responseEntity = questionController.getAllQuestion();

        verify(mockQuestionService).getQuestionList();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        ServiceResult responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        assertEquals(HttpStatus.OK.value(), responseBody.getStatusCode());
        assertEquals("Get question bank successfully!", responseBody.getMessage());

        List<Question> actualQuestionList = (List<Question>) responseBody.getData();
        assertNotNull(actualQuestionList);
        assertEquals(0, actualQuestionList.size());

        log.info(responseBody.getStatusCode() + "");
        log.info(responseBody.getMessage());
    }

    // Test Case ID: UT_QBM_003
    // Kiểm tra lấy câu hỏi theo id thành công khi dữ liệu tồn tại
    @Test
    void getQuestionById_Found() {
        Long questionId = 1L;
        Question expectedQuestion = new Question();
        expectedQuestion.setId(1L);
        expectedQuestion.setQuestionText("Câu hỏi kiểm thử");

        when(mockQuestionService.getQuestionById(questionId)).thenReturn(Optional.of(expectedQuestion));

        ResponseEntity<?> responseEntity = questionController.getQuestionById(questionId);

        verify(mockQuestionService).getQuestionById(questionId);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody() instanceof Question);

        Question actualQuestion = (Question) responseEntity.getBody();
        log.info(actualQuestion.toString());
        assertNotNull(actualQuestion);
        assertEquals(1L, actualQuestion.getId());
        assertEquals("Câu hỏi kiểm thử", actualQuestion.getQuestionText());

        log.info(responseEntity.getStatusCode() + "");
    }

    // Test Case ID: UT_QBM_004
    // Kiểm tra trả về thông báo không tìm thấy khi id không tồn tại
    @Test
    void getQuestionById_NotFound() {
        Long questionId = 999L;
        when(mockQuestionService.getQuestionById(questionId)).thenReturn(Optional.empty());

        ResponseEntity<?> responseEntity = questionController.getQuestionById(questionId);

        verify(mockQuestionService).getQuestionById(questionId);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody() instanceof ServiceResult);

        ServiceResult responseBody = (ServiceResult) responseEntity.getBody();
        assertNotNull(responseBody);
        assertEquals(HttpStatus.NOT_FOUND.value(), responseBody.getStatusCode());
        assertEquals("Not found with id: " + questionId, responseBody.getMessage());
        assertEquals(null, responseBody.getData());

        log.info(responseBody.getStatusCode() + "");
        log.info(responseBody.getMessage());
        log.info(responseBody.toString());
    }


    // Test Case ID: UT_QBM_005
    // Kiểm tra partId = 0 và người dùng là ADMIN => lấy toàn bộ câu hỏi
    @Test
    void getQuestionsByPart_PartIdZero_Admin() {
        Pageable pageable = PageRequest.of(0, 10);
        String username = "admin";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setRoles(Collections.singleton(adminRole));

        Question question = new Question();
        question.setId(1L);
        Page<Question> questionPage = new PageImpl<>(Collections.singletonList(question), pageable, 1);

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(adminUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockQuestionService.findAllQuestions(pageable)).thenReturn(questionPage);

        PageResult result = questionController.getQuestionsByPart(pageable, 0L);

        verify(mockQuestionService).findAllQuestions(pageable);
        assertNotNull(result);
        assertEquals(1, result.getData().size());

        log.info(result.getData().toString());
    }

    // Test Case ID: UT_QBM_006
    // Kiểm tra: partId = 0, user không phải ADMIN => Lấy danh sách câu hỏi do user đó tạo.
    @Test
    void getQuestionsByPart_PartIdZero_Lecturer() {
        Pageable pageable = PageRequest.of(0, 10);
        String usernameLecturer = "lecturer";


        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        Role lecturerRole = new Role();
        lecturerRole.setId(2L);
        lecturerRole.setName(ERole.ROLE_LECTURER);

        // User có role = LECTURER
        User lecturerUser = new User();
        lecturerUser.setUsername(usernameLecturer);
        lecturerUser.setRoles(Collections.singleton(lecturerRole));

        Question question1 = new Question();
        question1.setId(1L);
        question1.setCreatedBy(lecturerUser);

        Question question2 = new Question();
        question2.setId(2L);
        question2.setCreatedBy(lecturerUser);
        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question1, question2), pageable, 1);

        when(mockUserService.getUserName()).thenReturn(usernameLecturer);
        when(mockUserService.getUserByUsername(usernameLecturer)).thenReturn(Optional.of(lecturerUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockQuestionService.findQuestionsByCreatedBy_Username(pageable, usernameLecturer)).thenReturn(questionPage);

        PageResult result = questionController.getQuestionsByPart(pageable, 0L);

        verify(mockQuestionService).findQuestionsByCreatedBy_Username(pageable, usernameLecturer);
        assertNotNull(result);
        assertEquals(2, result.getData().size());
        assertTrue(result.getData().get(0) instanceof Question);
        Question returnedQuestion = (Question) result.getData().get(0);
        assertNotNull(returnedQuestion.getCreatedBy());
        assertEquals(usernameLecturer, returnedQuestion.getCreatedBy().getUsername());
        log.info(result.getData().toString());
    }

    // Test Case ID: UT_QBM_007
    // Kiểm tra partId > 0 và user là ADMIN => lấy câu hỏi theo part
    @Test
    void getQuestionsByPart_PartId_Admin() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "adminUser";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setRoles(Collections.singleton(adminRole));

        Part part = new Part();
        part.setId(partId);

        Question question = new Question();
        question.setId(1L);
        Page<Question> questionPage = new PageImpl<>(Collections.singletonList(question), pageable, 1);

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(adminUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockPartService.findPartById(partId)).thenReturn(Optional.of(part));
        when(mockQuestionService.findQuestionsByPart(pageable, part)).thenReturn(questionPage);

        PageResult result = questionController.getQuestionsByPart(pageable, partId);

        verify(mockPartService).findPartById(partId);
        verify(mockQuestionService).findQuestionsByPart(pageable, part);
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        log.info(result.getData().toString());
    }

    // Test Case ID: UT_QBM_008
    // Kiểm tra partId > 0 và user không phải ADMIN => lấy câu hỏi theo partId + username
    @Test
    void getQuestionsByPart_PartId_Lecturer() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "lecturerUser";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        Role lecturerRole = new Role();
        lecturerRole.setId(2L);
        lecturerRole.setName(ERole.ROLE_LECTURER);

        User lecturerUser = new User();
        lecturerUser.setUsername(username);
        lecturerUser.setRoles(Collections.singleton(lecturerRole));

        Question question1 = new Question();
        question1.setId(1L);
        question1.setCreatedBy(lecturerUser);

        Question question2 = new Question();
        question2.setId(2L);
        question2.setCreatedBy(lecturerUser);
        Page<Question> questionPage = new PageImpl<>(Arrays.asList(question1, question2), pageable, 1);

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(lecturerUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockQuestionService.findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username)).thenReturn(questionPage);

        PageResult result = questionController.getQuestionsByPart(pageable, partId);

        verify(mockQuestionService).findQuestionsByPart_IdAndCreatedBy_Username(pageable, partId, username);
        assertNotNull(result);
        assertEquals(2, result.getData().size());
        log.info(result.getData().toString());
    }

    // Test Case ID: UT_QBM_009
    // Kiểm tra ném lỗi khi không tìm thấy user theo username hiện tại
    @Test
    void getQuestionsByPart_UserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        String username = "missingUser";

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.getQuestionsByPart(pageable, 0L));
    }

    // Test Case ID: UT_QBM_010
    // Kiểm tra ném lỗi khi partId > 0, user là ADMIN nhưng không tìm thấy part
    @Test
    void getQuestionsByPart_PartNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 100L;
        String username = "admin";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setRoles(Collections.singleton(adminRole));

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(adminUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockPartService.findPartById(partId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.getQuestionsByPart(pageable, partId));
    }

    // Test Case ID: UT_QBM_011
    // Kiểm tra khi user là ADMIN => Lấy toàn bộ danh sách câu hỏi theo partId
    @Test
    void getQuestionsByPartNotDeleted_Admin() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "adminUser";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setRoles(Collections.singleton(adminRole));

        Part part = new Part();
        part.setId(partId);

        Question question = new Question();
        question.setId(1L);
        question.setDeleted(false);
        Page<Question> questionPage = new PageImpl<>(Collections.singletonList(question), pageable, 1);

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(adminUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockPartService.findPartById(partId)).thenReturn(Optional.of(part));
        when(mockQuestionService.findQuestionsByPartAndDeletedFalse(pageable, part)).thenReturn(questionPage);

        PageResult result = questionController.getQuestionsByPartNotDeleted(pageable, partId);

        verify(mockPartService).findPartById(partId);
        verify(mockQuestionService).findQuestionsByPartAndDeletedFalse(pageable, part);
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0) instanceof Question);
        assertEquals(false, ((Question) result.getData().get(0)).isDeleted());
        log.info(result.getData().toString());
    }

    // Test Case ID: UT_QBM_012
    // Kiểm tra khi user không phải ADMIN => Lấy danh sách câu hỏi theo partId & username
    @Test
    void getQuestionsByPartNotDeleted_Lecturer() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 1L;
        String username = "lecturer";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        Role lecturerRole = new Role();
        lecturerRole.setId(2L);
        lecturerRole.setName(ERole.ROLE_LECTURER);

        User lecturerUser = new User();
        lecturerUser.setUsername(username);
        lecturerUser.setRoles(Collections.singleton(lecturerRole));

        Question q = new Question();
        q.setId(1L);
        q.setDeleted(false);
        Page<Question> questionPage = new PageImpl<>(Collections.singletonList(q), pageable, 1);

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(lecturerUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockQuestionService.findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username))
                .thenReturn(questionPage);

        PageResult result = questionController.getQuestionsByPartNotDeleted(pageable, partId);

        verify(mockQuestionService).findQuestionsByPart_IdAndCreatedBy_UsernameAndDeletedFalse(pageable, partId, username);
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        log.info(result.getData().toString());
    }

    // Test Case ID: UT_QBM_013
    // Kiểm tra khi không tìm thấy user
    @Test
    void getQuestionsByPartNotDeleted_UserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        String username = "missingUser";

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.getQuestionsByPartNotDeleted(pageable, 1L));
    }

    // Test Case ID: UT_QBM_014
    // Kiểm tra khi user ADMIN nhưng không tìm thấy part theo partId
    @Test
    void getQuestionsByPartNotDeleted_PartNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        Long partId = 100L;
        String username = "admin";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(ERole.ROLE_ADMIN);

        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setRoles(Collections.singleton(adminRole));

        when(mockUserService.getUserName()).thenReturn(username);
        when(mockUserService.getUserByUsername(username)).thenReturn(Optional.of(adminUser));
        when(mockRoleService.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(mockPartService.findPartById(partId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.getQuestionsByPartNotDeleted(pageable, partId));
    }

    // Test Case ID: UT_QBM_015
    // Kiểm tra khi typeId tồn tại => Trả về HTTP 200, ServiceResult OK và danh sách câu hỏi
    @Test
    void getQuestionByQuestionType_TypeExists() {
        Long typeId = 1L;
        QuestionType questionType = new QuestionType();
        questionType.setId(typeId);
        questionType.setTypeCode(EQTypeCode.MC);

        Question q1 = new Question();
        q1.setId(1L);
        Question q2 = new Question();
        q2.setId(2L);
        List<Question> questions = Arrays.asList(q1, q2);

        when(mockQuestionTypeService.existsById(typeId)).thenReturn(true);
        when(mockQuestionTypeService.getQuestionTypeById(typeId)).thenReturn(Optional.of(questionType));
        when(mockQuestionService.getQuestionByQuestionType(questionType)).thenReturn(questions);

        ResponseEntity<?> response = questionController.getQuestionByQuestionType(typeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatusCode());
        assertEquals("Get question list with question type id: " + typeId, body.getMessage());
        List<?> data = (List<?>) body.getData();
        assertEquals(2, data.size());
        verify(mockQuestionTypeService).existsById(typeId);
        verify(mockQuestionTypeService).getQuestionTypeById(typeId);
        verify(mockQuestionService).getQuestionByQuestionType(questionType);

        log.info(body.getStatusCode() + "");
        log.info(body.getMessage());
        log.info(data.toString());
    }

    // Test Case ID: UT_QBM_016
    // kiểm tra typeId không tồn tại => Trả về HTTP 401 NOT_FOUND
    @Test
    void getQuestionByQuestionType_TypeNotFound() {
        Long typeId = 100L;
        when(mockQuestionTypeService.existsById(typeId)).thenReturn(false);

        ResponseEntity<?> response = questionController.getQuestionByQuestionType(typeId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);
        ServiceResult body = (ServiceResult) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatusCode());
        assertEquals("Not found question type with id: " + typeId, body.getMessage());
        assertEquals(null, body.getData());
        verify(mockQuestionTypeService).existsById(typeId);

        log.info(body.getStatusCode() + "");
        log.info(body.getMessage());
    }

    // Test Case ID: UT_QBM_017
    // Kiểm tra existsById true nhưng getQuestionTypeById rỗng => NoSuchElementException
    @Test
    void getQuestionByQuestionType_existType_serviceReturnEmpty() {
        Long typeId = 1L;
        when(mockQuestionTypeService.existsById(typeId)).thenReturn(true);
        when(mockQuestionTypeService.getQuestionTypeById(typeId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.getQuestionByQuestionType(typeId));
    }

    // Test Case ID: UT_QBM_018
    // Kiểm tra tạo câu hỏi thành công
    @Test
    void createQuestion_Success() {
        Question inputQuestion = new Question();
        inputQuestion.setId(1L);
        inputQuestion.setQuestionText("Nội dung câu hỏi");

        QuestionType questionTypeEntity = new QuestionType();
        questionTypeEntity.setId(1L);
        questionTypeEntity.setTypeCode(EQTypeCode.MC);

        Part partEntity = new Part();
        partEntity.setId(1L);

        Question reloaded = new Question();
        reloaded.setId(1L);
        reloaded.setQuestionText("Nội dung câu hỏi");
        reloaded.setQuestionType(questionTypeEntity);
        reloaded.setPart(partEntity);
        reloaded.setDeleted(false);

        when(mockQuestionTypeService.getQuestionTypeByCode(EQTypeCode.MC)).thenReturn(Optional.of(questionTypeEntity));
        when(mockPartService.findPartById(1L)).thenReturn(Optional.of(partEntity));
        when(mockQuestionService.getQuestionById(1L)).thenReturn(Optional.of(reloaded));

        Question result = questionController.createQuestion(inputQuestion, "MC", 1L);

        verify(mockQuestionService).save(inputQuestion);
        verify(mockQuestionService).getQuestionById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId().longValue());
        assertEquals(EQTypeCode.MC, result.getQuestionType().getTypeCode());
        assertEquals(1L, result.getPart().getId().longValue());
        assertEquals(false, result.isDeleted());

    }

    // Test Case ID: UT_QBM_019
    // Kiểm tra questionType không phải giá trị enum EQTypeCode
    @Test
    void createQuestion_InvalidQuestionTypeCode() {
        Question input = new Question();
        input.setId(1L);
        String questionType = "AA";
        assertThrows(IllegalArgumentException.class, () -> questionController.createQuestion(input, questionType, 1L));
    }

    // Test Case ID: UT_QBM_020
    // Kiểm tra không tìm thấy Part theo partId
    @Test
    void createQuestion_PartNotFound() {
        Question inputQuestion = new Question();
        inputQuestion.setId(1L);
        inputQuestion.setQuestionText("Nội dung câu hỏi");

        QuestionType questionTypeEntity = new QuestionType();
        questionTypeEntity.setId(1L);
        questionTypeEntity.setTypeCode(EQTypeCode.MC);

        Part partEntity = new Part();
        partEntity.setId(100L);
        when(mockQuestionTypeService.getQuestionTypeByCode(EQTypeCode.MS)).thenReturn(Optional.of(questionTypeEntity));
        when(mockPartService.findPartById(100L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> questionController.createQuestion(inputQuestion, "MS", 100L));
    }

    // Test Case ID: UT_QBM_021
    // Kiểm tra save xong nhưng không load lại được câu hỏi theo id
    @Test
    void createQuestion_ReloadByIdEmpty() {
        Question inputQuestion = new Question();
        inputQuestion.setId(1L);
        inputQuestion.setQuestionText("Nội dung câu hỏi");

        QuestionType questionTypeEntity = new QuestionType();
        questionTypeEntity.setId(1L);
        questionTypeEntity.setTypeCode(EQTypeCode.MC);

        Part partEntity = new Part();
        partEntity.setId(1L);
        when(mockQuestionTypeService.getQuestionTypeByCode(EQTypeCode.MC)).thenReturn(Optional.of(questionTypeEntity));
        when(mockPartService.findPartById(1L)).thenReturn(Optional.of(partEntity));
        when(mockQuestionService.getQuestionById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.createQuestion(inputQuestion, "MC", 1L));
        verify(mockQuestionService).save(inputQuestion);
    }

    // Test Case ID: UT_QBM_022
    // Kiểm tra tìm thấy id, cập nhật câu hỏi thành công
    @Test
    void updateQuestion_Success() {
        Long id = 1L;
        Question oldQuestion = new Question();
        oldQuestion.setId(id);
        oldQuestion.setQuestionText("Cũ");

        Question newQuestion = new Question();
        newQuestion.setQuestionText("Mới");

        when(mockQuestionService.getQuestionById(id)).thenReturn(Optional.of(oldQuestion));

        ResponseEntity<?> response = questionController.updateQuestion(newQuestion, id);

        verify(mockQuestionService).save(newQuestion);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);
        ServiceResult result = (ServiceResult) response.getBody();
        assertNotNull(result);
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Get question with id: " + id, result.getMessage());
        assertTrue(result.getData() instanceof Question);
        Question saved = (Question) result.getData();
        assertEquals(id, saved.getId());
        assertEquals("Mới", saved.getQuestionText());

        log.info(result.getStatusCode() + "");
        log.info(result.getMessage());
        log.info(saved.toString());
    }

    // Test Case ID: UT_QBM_023
    // Kiểm tra không tìm thấy câu hỏi theo id
    @Test
    void updateQuestion_IdNotFound() {
        Long id = 100L;
        Question oldQuestion = new Question();
        oldQuestion.setId(id);
        oldQuestion.setQuestionText("Cũ");

        Question newQuestion = new Question();
        newQuestion.setQuestionText("Mới");

        when(mockQuestionService.getQuestionById(id)).thenReturn(Optional.empty());

        ResponseEntity<?> response = questionController.updateQuestion(newQuestion, id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);
        ServiceResult result = (ServiceResult) response.getBody();
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        assertEquals("Not found with id: " + id, result.getMessage());
        assertEquals(null, result.getData());
        verify(mockQuestionService).getQuestionById(id);

        log.info(result.getStatusCode() + "");
        log.info(result.getMessage());
    }

    // Test Case ID: UT_QBM_024
    // Kiểm tra tìm thấy id và ẩn câu hỏi thành công
    @Test
    void deleteTempQuestion_HideSuccess() {
        Long id = 1L;

        Question question = new Question();
        question.setId(id);
        question.setDeleted(false);

        when(mockQuestionService.getQuestionById(id)).thenReturn(Optional.of(question));

        ResponseEntity<?> response = questionController.deleteTempQuestion(id, true);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(true, question.isDeleted());
        verify(mockQuestionService).update(question);
    }

    // Test Case ID: UT_QBM_025
    // Kiểm tra tìm thấy id và hiện câu hỏi thành công
    @Test
    void deleteTempQuestion_ShowSuccess() {
        Long id = 1L;

        Question question = new Question();
        question.setId(id);
        question.setDeleted(true);

        when(mockQuestionService.getQuestionById(id)).thenReturn(Optional.of(question));

        ResponseEntity<?> response = questionController.deleteTempQuestion(id, false);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(false, question.isDeleted());
        verify(mockQuestionService).update(question);
    }

    // Test Case ID: UT_QBM_026
    // Kiểm tra không tìm thấy id của câu hỏi
    @Test
    void deleteTempQuestion_IdNotFound() {
        Long id = 100L;
        when(mockQuestionService.getQuestionById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> questionController.deleteTempQuestion(id, true));
    }

}