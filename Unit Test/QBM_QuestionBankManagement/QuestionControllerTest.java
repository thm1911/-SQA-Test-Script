package com.thanhtam.backend;

import com.thanhtam.backend.controller.QuestionController;
import com.thanhtam.backend.dto.PageResult;
import com.thanhtam.backend.dto.ServiceResult;
import com.thanhtam.backend.entity.Choice;
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
import com.thanhtam.backend.ultilities.DifficultyLevel;
import com.thanhtam.backend.ultilities.EQTypeCode;
import com.thanhtam.backend.ultilities.ERole;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import com.thanhtam.backend.repository.PartRepository;
import com.thanhtam.backend.repository.QuestionRepository;
import com.thanhtam.backend.repository.QuestionTypeRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
@SpringBootTest
@Transactional
class QuestionControllerTest {

    QuestionService mockQuestionService = mock(QuestionService.class);
    PartService mockPartService = mock(PartService.class);
    QuestionTypeService mockQuestionTypeService = mock(QuestionTypeService.class);
    UserService mockUserService = mock(UserService.class);
    RoleService mockRoleService = mock(RoleService.class);
    @Autowired
    private QuestionService realQuestionService;
    @Autowired
    private PartService realPartService;
    @Autowired
    private QuestionTypeService realQuestionTypeService;
    @Autowired
    private UserService realUserService;
    @Autowired
    private RoleService realRoleService;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private PartRepository partRepository;
    @Autowired
    private QuestionTypeRepository questionTypeRepository;
    private QuestionController realQuestionController;

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
        reset(mockQuestionService, mockPartService, mockQuestionTypeService, mockUserService, mockRoleService);
        realQuestionController = new QuestionController(
                realQuestionService,
                realPartService,
                realQuestionTypeService,
                realUserService,
                realRoleService
        );
    }

    // ----getAllQuestion()---
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

    // ---getQuestionById()---
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

    //---getQuestionsByPart()---
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

    // ---getQuestionsByPartNotDeleted()---
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

    // ---getQuestionByQuestionType()---
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

    // ---createQuestion()---
    // Test Case ID: UT_QBM_018
    // Kiểm tra tạo câu hỏi thành công
    @Rollback
    @Test
    void createQuestion_Success() {
        Part partEntity = createPartTest();
        ensureQuestionType(EQTypeCode.MC);

        Question inputQuestion = new Question();
        inputQuestion.setQuestionText("Nội dung câu hỏi");
        inputQuestion.setDifficultyLevel(DifficultyLevel.EASY);
        Choice choice = new Choice();
        choice.setChoiceText("Đáp án A");
        choice.setIsCorrected(1);
        inputQuestion.setChoices(Collections.singletonList(choice));

        Question result = realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId());

        log.info("createQuestion_Success - created result: {}", result);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(EQTypeCode.MC, result.getQuestionType().getTypeCode());
        assertEquals(partEntity.getId(), result.getPart().getId());
        assertEquals(false, result.isDeleted());
        assertTrue(questionRepository.findById(result.getId()).isPresent());
    }

    // Test Case ID: UT_QBM_019
    // Kiểm tra thiếu nội dung câu hỏi (null, rỗng hoặc chỉ khoảng trắng)
    @Test
    void createQuestion_MissingQuestionText() {
        Part partEntity = createPartTest();
        ensureQuestionType(EQTypeCode.MC);
        Question inputQuestion = new Question();
        inputQuestion.setDifficultyLevel(DifficultyLevel.EASY);
        Choice choice = new Choice();
        choice.setChoiceText("Đáp án A");
        choice.setIsCorrected(1);
        inputQuestion.setChoices(Collections.singletonList(choice));

        // Null questionText
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));

        // Empty questionText
        inputQuestion.setQuestionText("");
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));

        // Blank questionText
        inputQuestion.setQuestionText("   ");
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));
    }

    // Test Case ID: UT_QBM_020
    // Kiểm tra danh sách đáp án rỗng
    @Test
    void createQuestion_EmptyOrNullChoicesList() {
        Part partEntity = createPartTest();
        ensureQuestionType(EQTypeCode.MC);
        Question inputQuestion = new Question();
        inputQuestion.setQuestionText("Nội dung câu hỏi");
        inputQuestion.setDifficultyLevel(DifficultyLevel.EASY);
        inputQuestion.setChoices(Collections.emptyList());
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));
    }

    // Test Case ID: UT_QBM_021
    // Kiểm tra nội dung đáp án null/rỗng/chỉ khoảng trắng
    @Test
    void createQuestion_EmptyChoiceText() {
        Part partEntity = createPartTest();
        ensureQuestionType(EQTypeCode.MC);
        Question inputQuestion = new Question();
        inputQuestion.setQuestionText("Nội dung câu hỏi");
        inputQuestion.setDifficultyLevel(DifficultyLevel.EASY);

        Choice choice = new Choice();
        choice.setIsCorrected(1);

        // choiceText null
        choice.setChoiceText(null);
        inputQuestion.setChoices(Collections.singletonList(choice));
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));

        // choiceText empty
        choice.setChoiceText("");
        inputQuestion.setChoices(Collections.singletonList(choice));
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));

        // choiceText blank
        choice.setChoiceText("   ");
        inputQuestion.setChoices(Collections.singletonList(choice));
        assertThrows(Exception.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId()));
    }

    // Test Case ID: UT_QBM_022
    // Kiểm tra questionType không phải giá trị enum EQTypeCode
    @Test
    void createQuestion_InvalidQuestionTypeCode() {
        Question input = new Question();
        String questionType = "AA";
        Part partEntity = createPartTest();
        assertThrows(IllegalArgumentException.class, () -> realQuestionController.createQuestion(input, questionType, partEntity.getId()));
    }

    // Test Case ID: UT_QBM_023
    // Kiểm tra không tìm thấy Part theo partId
    @Test
    void createQuestion_PartNotFound() {
        ensureQuestionType(EQTypeCode.MC);
        Question inputQuestion = new Question();
        inputQuestion.setQuestionText("Nội dung câu hỏi");
        inputQuestion.setDifficultyLevel(DifficultyLevel.EASY);

        Choice choice = new Choice();
        choice.setChoiceText("Đáp án A");
        choice.setIsCorrected(1);
        inputQuestion.setChoices(Collections.singletonList(choice));

        assertThrows(NoSuchElementException.class, () -> realQuestionController.createQuestion(inputQuestion, "MC", Long.MAX_VALUE));
    }

    // Test Case ID: UT_QBM_024
    // Kiểm tra save xong nhưng không load lại được câu hỏi theo id
    @Test
    void createQuestion_ReloadByIdEmpty() {
        Part partEntity = createPartTest();
        ensureQuestionType(EQTypeCode.MC);
        Question inputQuestion = new Question();
        inputQuestion.setQuestionText("Nội dung câu hỏi");
        inputQuestion.setDifficultyLevel(DifficultyLevel.EASY);
        Choice choice = new Choice();
        choice.setChoiceText("Đáp án A");
        choice.setIsCorrected(1);
        inputQuestion.setChoices(Collections.singletonList(choice));
        Question created = realQuestionController.createQuestion(inputQuestion, "MC", partEntity.getId());
        assertTrue(questionRepository.findById(created.getId()).isPresent());
    }

    // ---updateQuestion()---
    // Test Case ID: UT_QBM_025
    // Kiểm tra tìm thấy id, cập nhật câu hỏi thành công
    @Test
    void updateQuestion_Success() {
        Part part = createPartTest();
        QuestionType questionType = ensureQuestionType(EQTypeCode.MC);
        Question oldQuestion = saveQuestionFixture("Cũ", part, questionType, false);
        Long id = oldQuestion.getId();
        Question newQuestion = new Question();
        newQuestion.setQuestionText("Mới");
        newQuestion.setDifficultyLevel(DifficultyLevel.EASY);
        newQuestion.setPart(part);
        newQuestion.setQuestionType(questionType);
        Choice choice = new Choice();
        choice.setChoiceText("Dap an update");
        choice.setIsCorrected(1);
        newQuestion.setChoices(Collections.singletonList(choice));
        newQuestion.setDeleted(false);

        ResponseEntity<?> response = realQuestionController.updateQuestion(newQuestion, id);

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
        Question updated = questionRepository.findById(id).orElseThrow(NoSuchElementException::new);
        assertEquals("Mới", updated.getQuestionText());

        log.info(result.getStatusCode() + "");
        log.info(result.getMessage());
        log.info(saved.toString());
    }

    // Test Case ID: UT_QBM_026
    // Kiểm tra thiếu nội dung câu hỏi khi cập nhật (null, rỗng hoặc chỉ khoảng trắng)
    @Test
    void updateQuestion_MissingQuestionText() {
        Part part = createPartTest();
        QuestionType questionType = ensureQuestionType(EQTypeCode.MC);
        Long id = saveQuestionFixture("Cũ", part, questionType, false).getId();

        Question nullText = new Question();
        Exception exNull = assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(nullText, id));

        Question emptyText = new Question();
        emptyText.setQuestionText("");
        Exception exEmpty = assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(emptyText, id));

        Question blankText = new Question();
        blankText.setQuestionText("   ");
        Exception exBlank = assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(blankText, id));
    }

    // Test Case ID: UT_QBM_027
    // Kiểm tra danh sách đáp án rỗng khi cập nhật
    @Test
    void updateQuestion_EmptyOrNullChoicesList() {
        Part part = createPartTest();
        QuestionType questionType = ensureQuestionType(EQTypeCode.MC);
        Long id = saveQuestionFixture("Cũ", part, questionType, false).getId();

        Question emptyList = new Question();
        emptyList.setQuestionText("Mới");
        emptyList.setDifficultyLevel(DifficultyLevel.EASY);
        emptyList.setChoices(Collections.emptyList());
        emptyList.setPart(part);
        emptyList.setQuestionType(questionType);
        assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(emptyList, id));

    }

    // Test Case ID: UT_QBM_028
    // Kiểm tra nội dung đáp án null/rỗng/chỉ khoảng trắng
    @Test
    void updateQuestion_EmptyChoiceText() {
        Part part = createPartTest();
        QuestionType questionType = ensureQuestionType(EQTypeCode.MC);
        Long id = saveQuestionFixture("Cũ", part, questionType, false).getId();

        Choice nullText = new Choice();
        nullText.setChoiceText(null);
        nullText.setIsCorrected(0);

        Question qNullChoiceText = new Question();
        qNullChoiceText.setQuestionText("Mới");
        qNullChoiceText.setDifficultyLevel(DifficultyLevel.EASY);
        qNullChoiceText.setChoices(Collections.singletonList(nullText));
        qNullChoiceText.setPart(part);
        qNullChoiceText.setQuestionType(questionType);
        assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(qNullChoiceText, id));

        Choice emptyText = new Choice();
        emptyText.setChoiceText("");
        emptyText.setIsCorrected(0);
        Question qEmptyChoiceText = new Question();
        qEmptyChoiceText.setQuestionText("Mới");
        qEmptyChoiceText.setDifficultyLevel(DifficultyLevel.EASY);
        qEmptyChoiceText.setChoices(Collections.singletonList(emptyText));
        qEmptyChoiceText.setPart(part);
        qEmptyChoiceText.setQuestionType(questionType);
        assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(qEmptyChoiceText, id));

        Choice blankText = new Choice();
        blankText.setChoiceText("   ");
        blankText.setIsCorrected(0);
        Question qBlankChoiceText = new Question();
        qBlankChoiceText.setQuestionText("Mới");
        qBlankChoiceText.setDifficultyLevel(DifficultyLevel.EASY);
        qBlankChoiceText.setChoices(Collections.singletonList(blankText));
        qBlankChoiceText.setPart(part);
        qBlankChoiceText.setQuestionType(questionType);
        assertThrows(IllegalArgumentException.class, () -> realQuestionController.updateQuestion(qBlankChoiceText, id));
    }

    // Test Case ID: UT_QBM_029
    // Kiểm tra không tìm thấy câu hỏi theo id
    @Test
    void updateQuestion_IdNotFound() {
        Long id = Long.MAX_VALUE;
        Question newQuestion = new Question();
        newQuestion.setQuestionText("Mới");
        ResponseEntity<?> response = realQuestionController.updateQuestion(newQuestion, id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ServiceResult);
        ServiceResult result = (ServiceResult) response.getBody();
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatusCode());
        assertEquals("Not found with id: " + id, result.getMessage());
        assertEquals(null, result.getData());
        log.info(result.getStatusCode() + "");
        log.info(result.getMessage());
    }

    // ---deleteTempQuestion()---
    // Test Case ID: UT_QBM_030
    // Kiểm tra tìm thấy id và ẩn câu hỏi thành công
    @Test
    void deleteTempQuestion_HideSuccess() {
        Part part = createPartTest();
        QuestionType questionType = ensureQuestionType(EQTypeCode.MC);
        Question question = saveQuestionFixture("Delete hide", part, questionType, false);
        ResponseEntity<?> response = realQuestionController.deleteTempQuestion(question.getId(), true);

        log.info("deleteTempQuestion_HideSuccess - response status: {}", response.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        Question dbQuestion = questionRepository.findById(question.getId()).orElseThrow(NoSuchElementException::new);
        log.info("deleteTempQuestion_HideSuccess - db question after update: {}", dbQuestion);
        assertEquals(true, dbQuestion.isDeleted());
    }

    // Test Case ID: UT_QBM_031
    // Kiểm tra tìm thấy id và hiện câu hỏi thành công
    @Test
    void deleteTempQuestion_ShowSuccess() {
        Part part = createPartTest();
        QuestionType questionType = ensureQuestionType(EQTypeCode.MC);
        Question question = saveQuestionFixture("Delete show", part, questionType, true);
        ResponseEntity<?> response = realQuestionController.deleteTempQuestion(question.getId(), false);

        log.info("deleteTempQuestion_ShowSuccess - response status: {}", response.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        Question dbQuestion = questionRepository.findById(question.getId()).orElseThrow(NoSuchElementException::new);
        log.info("deleteTempQuestion_ShowSuccess - db question after update: {}", dbQuestion);
        assertEquals(false, dbQuestion.isDeleted());
    }

    // Test Case ID: UT_QBM_032
    // Kiểm tra không tìm thấy id của câu hỏi
    @Test
    void deleteTempQuestion_IdNotFound() {
        assertThrows(NoSuchElementException.class, () -> realQuestionController.deleteTempQuestion(Long.MAX_VALUE, true));
    }

    private QuestionType ensureQuestionType(EQTypeCode typeCode) {
        return questionTypeRepository.findAllByTypeCode(typeCode).orElseGet(() -> {
            QuestionType questionType = new QuestionType();
            questionType.setTypeCode(typeCode);
            questionType.setDescription("Auto test type " + typeCode.name());
            return questionTypeRepository.save(questionType);
        });
    }

    private Part createPartTest() {
        Long courseId = 12L;
        Long partId = 72L;
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay part id = " + partId));
        if (part.getCourse() == null || !courseId.equals(part.getCourse().getId())) {
            throw new IllegalStateException("Part " + partId + " khong thuoc course " + courseId);
        }
        return part;
    }

    private Question saveQuestionFixture(String questionText, Part part, QuestionType questionType, boolean deleted) {
        Question question = new Question();
        question.setQuestionText(questionText);
        question.setDifficultyLevel(DifficultyLevel.EASY);
        question.setPart(part);
        question.setQuestionType(questionType);
        question.setDeleted(deleted);

        Choice choice = new Choice();
        choice.setChoiceText("Dap an fixture");
        choice.setIsCorrected(1);
        question.setChoices(Collections.singletonList(choice));

        return questionRepository.save(question);
    }

}