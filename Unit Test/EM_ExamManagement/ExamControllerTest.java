package com.thanhtam.backend.controller;

import com.amazonaws.services.workdocs.model.EntityNotExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhtam.backend.dto.*;
import com.thanhtam.backend.entity.*;
import com.thanhtam.backend.service.*;
import com.thanhtam.backend.ultilities.DifficultyLevel;
import com.thanhtam.backend.ultilities.ERole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.DisplayName;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExamControllerTest {

    @Mock
    private ExamService examService;

    @Mock
    private QuestionService questionService;

    @Mock
    private UserService userService;

    @Mock
    private IntakeService intakeService;

    @Mock
    private PartService partService;

    @Mock
    private ExamUserService examUserService;

    private ObjectMapper mapper;
    private ExamController examController;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        examController = new ExamController(
                examService,
                questionService,
                userService,
                intakeService,
                partService,
                examUserService,
                mapper
        );

    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("UM_EM_001: Trường hợp user là admin --> trả về toàn bộ exam ")
    public void getExamByPage_forAdmin() {
        String username = "admin1";
        Pageable pageable = PageRequest.of(0, 10);

        //arrange
        when(userService.getUserName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(userWithRoles(ERole.ROLE_ADMIN)));

        Page<Exam> allPage = new PageImpl<>(Arrays.asList(new Exam(), new Exam()), pageable, 2);
        Page<Exam> ownPage = new PageImpl<>(Arrays.asList(new Exam()), pageable, 1);

        when(examService.findAll(pageable)).thenReturn(allPage);
        when(examService.findAllByCreatedBy_Username(pageable, username)).thenReturn(ownPage);

        //act
        PageResult result = examController.getExamsByPage(pageable);

        //assert
        assertEquals(2, result.getData().size());
        verify(examService).findAll(pageable);
    }

    @Test
    @DisplayName("UM_EM_002: Trường hợp user là lecture --> trả về những exam mà lecture đã tạo ")
    public void getExamByPage_forLecture() {
        String username = "lecture1";
        Pageable pageable = PageRequest.of(0, 10);

        //arrange
        when(userService.getUserName()).thenReturn(username);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(userWithRoles(ERole.ROLE_LECTURER)));

        Page<Exam> ownPage = new PageImpl<>(Arrays.asList(new Exam(), new Exam()), pageable, 2);

        when(examService.findAllByCreatedBy_Username(pageable, username)).thenReturn(ownPage);

        //act
        PageResult result = examController.getExamsByPage(pageable);

        //assert
        assertEquals(2, result.getData().size());
        verify(examService).findAllByCreatedBy_Username(pageable, username);
    }

    @Test
    @DisplayName("UM_EM_003: Trường hợp username là null --> trả về NullPointerException")
    public void getExamByPage_usernameIsNull() {
        String username = null;
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(NullPointerException.class, () -> examController.getExamsByPage(pageable));

    }

    @Test
    @DisplayName("UM_EM_004: Trường hợp user không tồn tại --> trả về EntityNotExistsException>")
    public void getExamByPage_userNotExisted() {
        String username = "userNotFound";
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserName()).thenReturn(username);

        assertThrows(EntityNotExistsException.class, () -> examController.getExamsByPage(pageable));

    }

    @Test
    @DisplayName("UM_EM_005: Trường hợp 1 bài thi chưa bắt đầu --> trạng thái của exam user là khóa, không thể làm bài")
    public void getAllByUser_CurrentDateGreaterThanBeginExam() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        Exam futureExam = createExamWithTime(true, false, 600000L, 3600000L, null, 60);
        ExamUser futureExamUser = createExamUser(futureExam, new User(), false, false, 1000, "", -1.0);

        when(examUserService.getExamListByUsername("student1")).thenReturn(Arrays.asList(futureExamUser));

        ResponseEntity<List<ExamUser>> response = examController.getAllByUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().get(0).getExam().isLocked());

        verify(examUserService).getExamListByUsername("student1");
    }

    @Test
    @DisplayName("UM_EM_006: Trường hợp 1 bài thi đã bắt đầu --> trạng thái bài thi không khóa, có thể làm bài")
    public void getAllByUser_CurrentDateSmallerOrEqualThanBeginExam() {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        Exam pastExam = createExamWithTime(false, false, -600000L, 3600000L, null, 60);
        ExamUser pastExamUser = createExamUser(pastExam, new User(), false, false, 1000, "", -1.0);

        when(examUserService.getExamListByUsername("student1")).thenReturn(Arrays.asList(pastExamUser));

        //act
        ResponseEntity<List<ExamUser>> response = examController.getAllByUser();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().get(0).getExam().isLocked());

        SecurityContextHolder.clearContext();
        verify(examUserService).getExamListByUsername("student1");
    }

    @Test
    @DisplayName("UM_EM_007: Trường hợp user không có bài thi nào --> trả về danh sách rỗng, không ném ra exception")
    public void getAllByUser_userNoExamUser() {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";
        List<ExamUser> emptyList = new ArrayList<>();

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        when(examUserService.getExamListByUsername("student1")).thenReturn(emptyList);

        ResponseEntity<List<ExamUser>> response = examController.getAllByUser();
        //assert
        assertTrue(response.getBody().isEmpty());

        SecurityContextHolder.clearContext();
        verify(examUserService).getExamListByUsername("student1");
    }


    @Test
    @DisplayName("UM_EM_008: Trường hợp 5 bài thi chưa bắt đầu --> sau khi chạy xong thì 5 bài đều bị locked (chỉ để kiểm tra vòng lặp)")
    public void getAllByUser_5ExamUserCurrentDateGreaterThanBeginExam() {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        Exam futureExam1 = createExamWithTime(true, false, 600000L, 3600000L, null, 60);
        ExamUser futureExamUser1 = createExamUser(futureExam1, new User(), false, false, 1000, "", -1.0);
        Exam futureExam2 = createExamWithTime(true, false, 600000L, 3600000L, null, 60);
        ExamUser futureExamUser2 = createExamUser(futureExam2, new User(), false, false, 1000, "", -1.0);
        Exam futureExam3 = createExamWithTime(true, false, 600000L, 3600000L, null, 60);
        ExamUser futureExamUser3 = createExamUser(futureExam3, new User(), false, false, 1000, "", -1.0);
        Exam futureExam4 = createExamWithTime(true, false, 600000L, 3600000L, null, 60);
        ExamUser futureExamUser4 = createExamUser(futureExam4, new User(), false, false, 1000, "", -1.0);
        Exam futureExam5 = createExamWithTime(true, false, 600000L, 3600000L, null, 60);
        ExamUser futureExamUser5 = createExamUser(futureExam5, new User(), false, false, 1000, "", -1.0);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);
        when(examUserService.getExamListByUsername("student1")).thenReturn(Arrays.asList(futureExamUser1, futureExamUser2, futureExamUser3, futureExamUser4, futureExamUser5));

        //act
        ResponseEntity<List<ExamUser>> response = examController.getAllByUser();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(response.getBody().get(0).getExam().isLocked(), true);
        assertEquals(response.getBody().get(4).getExam().isLocked(), true);

        SecurityContextHolder.clearContext();
        verify(examUserService).getExamListByUsername("student1");
    }

    @Test
    @DisplayName("UM_EM_009: Trường hợp tìm thấy exam user --> trả về response có status thành công, trong response chứa exam user đúng với exam id truyền vào")
    public void getExamUserById_hasFoundExamUser() throws ParseException {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        ExamUser examUser = new  ExamUser();
        examUser.setExam(new Exam());
        when(examUserService.findByExamAndUser(1L,  username)).thenReturn(examUser);

        //act
        ResponseEntity<ExamUser> response = examController.getExamUserById(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(response.getBody(), examUser);

    }

    @Test
    @DisplayName("UM_EM_010: Trường hợp không tìm thấy exan user --> trả về EntityNotFoundException")
    public void getExamUserById_hasNotFoundException() throws ParseException {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        when(examUserService.findByExamAndUser(9999L,  username)).thenReturn(null);

        //assert
        assertThrows(EntityNotFoundException.class, () -> examController.getExamUserById(9999L));
    }

    @Test
    @DisplayName("UM_EM_011: Trường hợp lấy ra bài kiểm tra đã bắt đầu làm rồi, " +
            "có 1 câu hỏi, người dùng đã chọn đáp án 1 câu hỏi " +
            "--> trả ra danh sách câu hỏi có chứa câu hỏi có đáp án mà người dùng đã chọn," +
            " status code thành công")
    public void getAllQuestions_ExamHasStartedAnd1Question() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = createExamWithTime(false, false, -600000L, 3600000L, null, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        String answerSheetJson = generateAnswerSheetString(1);
        ExamUser examUser = createExamUser(exam, new User(), true, false, 1700, answerSheetJson, -1.0);
        when(examUserService.findByExamAndUser(1L, "student1")).thenReturn(examUser);

        Question question = new Question();
        question.setId(1L);
        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(question));

        //act
        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1700, response.getBody().getRemainingTime());
        assertEquals(1, response.getBody().getQuestions().size());
        assertEquals(10, response.getBody().getQuestions().get(0).getPoint());
        assertEquals(question.getChoices(), response.getBody().getQuestions().get(0).getChoices());
        verify(examUserService, never()).update(any(ExamUser.class));
    }

    @Test
    @DisplayName("UM_EM_012: Trường hợp lấy ra bài kiểm tra đã bắt đầu làm rồi, " +
            "có 1 câu hỏi, chưa chọn đáp án câu nào " +
            "--> trả ra bài thi không có câu hỏi đã chon đáp án, " +
            "status code thành công")
    public void getAllQuestions_ExamHasStartedAndNoQuestion() throws IOException {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = createExamWithTime(false, false, -600000L, 3600000L, null, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        String answerSheetJson = "[]";
        ExamUser examUser = createExamUser(exam, new User(), true, false, 1700, answerSheetJson, -1.0);
        when(examUserService.findByExamAndUser(1L, "student1")).thenReturn(examUser);

        //act
        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1700, response.getBody().getRemainingTime());
        assertEquals(0, response.getBody().getQuestions().size());
        verify(questionService, never()).getQuestionById(1L);
        verify(examUserService, never()).update(any(ExamUser.class));
    }

    @Test
    @DisplayName("UM_EM_013: Trường hợp bài thi có 5 câu hỏi, đã chọn đáp án 5 câu" +
            " --> trả ra đầy đủ thông tin 5 câu hỏi và đáp án đã chọn")
    public void getAllQuestions_ExamHasStartAnd5Questions() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = createExamWithTime(false, false, -600000L, 3600000L, null, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        String answerSheetJson = generateAnswerSheetString(5);
        ExamUser examUser = createExamUser(exam, new User(), true, false, 1700, answerSheetJson, -1.0);
        when(examUserService.findByExamAndUser(1L, "student1")).thenReturn(examUser);

        for (long i = 1; i <= 5; i++) {
            Question q = new Question();
            q.setId(i);

            when(questionService.getQuestionById(i)).thenReturn(Optional.of(q));
        }

        //act
        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1700, response.getBody().getRemainingTime());
        assertEquals(5, response.getBody().getQuestions().size());
        assertEquals(10, response.getBody().getQuestions().get(0).getPoint());
        assertEquals(10, response.getBody().getQuestions().get(4).getPoint());
        verify(questionService, times(5)).getQuestionById(anyLong());
        verify(examUserService, never()).update(any(ExamUser.class));
    }

    @Test
    @DisplayName("UM_EM_014: Trường hợp bài thi chưa làm, có trộn câu hỏi, có 5 câu hỏi" +
            " --> trả ra đầy đủ thông tin bài thi, 5 câu hỏi tráo vị trí ")
    public void getAllQuestions_ExamNotStartedAndHas5QuestionsAndHasShuffle() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        String questionData = generateExamQuestionPointString(5);

        Exam exam = createExamWithTime(false, true, -600000L, 3600000L, questionData, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        String answerSheetJson = null;
        ExamUser examUser = createExamUser(exam, new User(), false, false, 1700, answerSheetJson, -1.0);
        when(examUserService.findByExamAndUser(1L, "student1")).thenReturn(examUser);

        List<Question> questions = new ArrayList<>();
        List<AnswerSheet> answerSheets = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            Question q = new Question();
            q.setId(i);
            questions.add(q);

            AnswerSheet as = new AnswerSheet(i, new ArrayList<>(), 10);
            answerSheets.add(as);

            when(questionService.getQuestionById(i)).thenReturn(Optional.of(q));
        }

        when(questionService.getQuestionPointList(anyList())).thenReturn(questions);
        when(questionService.convertFromQuestionList(anyList())).thenReturn(answerSheets);

        //act
        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<Long> actualIds = new ArrayList<>();
        for  (Question q : response.getBody().getQuestions()) {
            actualIds.add(q.getId());
        }
        assertThat(actualIds)
                .hasSize(5)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);

        assertTrue(examUser.getIsStarted());
        assertNotNull(examUser.getAnswerSheet());
        assertNotNull(examUser.getTimeStart());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(anyLong());
        verify(examUserService, times(1)).findByExamAndUser(anyLong(), anyString());
        verify(examUserService, times(2)).update(examUser);
        verify(questionService, times(5)).getQuestionById(anyLong());
    }

    @Test
    @DisplayName("UM_EM_015: Trường hợp bài thi chưa làm, không trộn câu hỏi, có 5 câu hỏi --> trả ra đầy đủ thông tin bài thi, 5 câu hỏi giữ nguyên vị trí ")
    public void getAllQuestions_ExamNotStartedAndHas5QuestionsAndNoShuffle() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        String questionData = generateExamQuestionPointString(5);

        Exam exam = createExamWithTime(false, false, -600000L, 3600000L, questionData, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        String answerSheetJson = null;
        ExamUser examUser = createExamUser(exam, new User(), false, false, 1700, answerSheetJson, -1.0);
        when(examUserService.findByExamAndUser(1L, "student1")).thenReturn(examUser);

        List<Question> questions = new ArrayList<>();
        List<AnswerSheet> answerSheets = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            Question q = new Question();
            q.setId(i);
            questions.add(q);

            AnswerSheet as = new AnswerSheet(i, new ArrayList<>(), 10);
            answerSheets.add(as);

            when(questionService.getQuestionById(i)).thenReturn(Optional.of(q));
        }

        when(questionService.getQuestionPointList(anyList())).thenReturn(questions);
        when(questionService.convertFromQuestionList(anyList())).thenReturn(answerSheets);

        //act
        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(questions, response.getBody().getQuestions());

        assertTrue(examUser.getIsStarted());
        assertNotNull(examUser.getAnswerSheet());
        assertNotNull(examUser.getTimeStart());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(anyLong());
        verify(examUserService, times(1)).findByExamAndUser(anyLong(), anyString());
        verify(examUserService, times(1)).update(examUser);
        verify(questionService, times(5)).getQuestionById(anyLong());
    }

    @Test
    @DisplayName("UM_EM_016: Trường hợp bài thi chưa làm, không có câu hỏi " +
            "--> trả ra đầy đủ thông tin bài thi, không có câu hỏi ")
    public void getAllQuestions_ExamNotStartedAndNoQuestionsAndNoShuffle() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        String questionData = generateExamQuestionPointString(0);

        Exam exam = createExamWithTime(false, false, -600000L, 3600000L, questionData, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        String answerSheetJson = null;
        ExamUser examUser = createExamUser(exam, new User(), false, false, 1700, answerSheetJson, -1.0);
        when(examUserService.findByExamAndUser(1L, "student1")).thenReturn(examUser);

        List<Question> questions = new ArrayList<>();
        List<AnswerSheet> answerSheets = new ArrayList<>();

        when(questionService.getQuestionPointList(anyList())).thenReturn(questions);
        when(questionService.convertFromQuestionList(anyList())).thenReturn(answerSheets);

        //act
        ResponseEntity<ExamQuestionList> response = examController.getAllQuestions(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        assertTrue(examUser.getIsStarted());
        assertNotNull(examUser.getAnswerSheet());
        assertNotNull(examUser.getTimeStart());
        assertEquals(questions, response.getBody().getQuestions());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(anyLong());
        verify(examUserService, times(1)).findByExamAndUser(anyLong(), anyString());
        verify(examUserService, times(2)).update(examUser);
        verify(questionService, never()).getQuestionById(anyLong());
    }

    @Test
    @DisplayName("UT_EM_017: Trường hợp không tìm thấy bài kiểm tra --> Trả về EntityNotExistsException")
    public void getAllQuestions_ExamNotFound() {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.getAllQuestions(999L));
        verify(userService, times(1)).getUserName();
    }

    @Test
    @DisplayName("UT_EM_018: Trường hợp bài kiểm tra bị locked --> Trả về IllegalStateException")
    public void getAllQuestions_ExamIsLocked() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        String questionData = generateExamQuestionPointString(5);

        Exam exam = createExamWithTime(true, false, -600000L, 3600000L, questionData, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));


        //assert
        assertThrows(IllegalStateException.class, () -> examController.getAllQuestions(1L));
        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(anyLong());
    }

    @Test
    @DisplayName("UT_EM_019: Trường hợp chưa đến giờ làm bài --> Trả về IllegalStateException")
    public void getAllQuestions_ExamCurrentTimeSmallerThanBeginExam() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        String questionData = generateExamQuestionPointString(5);

        Exam exam = createExamWithTime(false, false, 600000L, 3600000L, questionData, 60);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));


        //assert
        assertThrows(IllegalStateException.class, () -> examController.getAllQuestions(1L));
        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(anyLong());
    }

    @Test
    @DisplayName("UT_EM_020: Trường hợp có user, có intake, có part, tạo exam thành công " +
            "--> trả về response thành công, có thông tin exam")
    public void createExam_ValidDataAndSucceess() throws Exception {
        //arrange
        String username = "admin1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Intake intake = new Intake();
        intake.setId(1L);
        when(intakeService.findById(1L)).thenReturn(Optional.of(intake));

        Part part = new Part();
        part.setId(1L);
        when(partService.findPartById(1L)).thenReturn(Optional.of(part));

        Exam exam = new Exam();
        exam.setQuestionData("[]");
        when(examService.saveExam(exam)).thenReturn(exam);

        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setId(2L);
        user1.setIntake(intake);
        users.add(user1);
        User user2 = new User();
        user2.setId(3L);
        user2.setIntake(intake);
        users.add(user2);
        when(userService.findAllByIntakeId(1L)).thenReturn(users);

        //act
        ResponseEntity<?> response = examController.createExam(exam, 1L, 1L, false, false);

        //assert
        assertEquals(user, exam.getCreatedBy());
        assertEquals(intake, exam.getIntake());
        assertEquals(part, exam.getPart());
        assertFalse(exam.isCanceled());
        assertFalse(exam.isShuffle());

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(1L);
        verify(partService, times(1)).findPartById(1L);
        verify(examService, times(1)).saveExam(exam);
        verify(userService, times(1)).findAllByIntakeId(1L);
        verify(examUserService, times(1)).create(exam, users);

    }

    @Test
    @DisplayName("UT_EM_021: Trường hợp chuyển đổi dữ liệu câu hỏi từ String sang Array thất bại" +
            " --> ném ra IllegalArgumentException")
    public void createExam_convertQuestionDataFail() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Intake intake = new Intake();
        intake.setId(1L);
        when(intakeService.findById(1L)).thenReturn(Optional.of(intake));

        Part part = new Part();
        part.setId(1L);
        when(partService.findPartById(1L)).thenReturn(Optional.of(part));

        Exam exam = new Exam();
        exam.setQuestionData("[][][]");
        when(examService.saveExam(exam)).thenReturn(exam);

        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setId(2L);
        user1.setIntake(intake);
        users.add(user1);
        User user2 = new User();
        user2.setId(3L);
        user2.setIntake(intake);
        users.add(user2);
        when(userService.findAllByIntakeId(1L)).thenReturn(users);

        //assert
        assertThrows(IllegalArgumentException.class, () -> examController.createExam(exam, 1L, 1L, false, false));

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(1L);
        verify(partService, times(1)).findPartById(1L);
        verify(examService, times(1)).saveExam(exam);

    }

    @Test
    @DisplayName("UT_EM_022: Trường hợp tạo exam user fail --> trả về RuntimeException")
    public void createExam_createExamUserFail() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Intake intake = new Intake();
        intake.setId(1L);
        when(intakeService.findById(1L)).thenReturn(Optional.of(intake));

        Part part = new Part();
        part.setId(1L);
        when(partService.findPartById(1L)).thenReturn(Optional.of(part));

        Exam exam = new Exam();
        exam.setQuestionData("[]");
        when(examService.saveExam(exam)).thenReturn(exam);

        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setId(2L);
        user1.setIntake(intake);
        users.add(user1);
        User user2 = new User();
        user2.setId(3L);
        user2.setIntake(intake);
        users.add(user2);
        when(userService.findAllByIntakeId(1L)).thenReturn(users);

        doThrow(new RuntimeException("Create exam user fail"))
                .when(examUserService).create(any(Exam.class), anyList());


        //assert
        assertThrows(RuntimeException.class, () -> examController.createExam(exam, 1L, 1L, false, false));

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(1L);
        verify(partService, times(1)).findPartById(1L);
        verify(examService, times(1)).saveExam(exam);
        verify(userService, times(1)).findAllByIntakeId(1L);
        verify(examUserService, times(1)).create(exam, users);

    }

    @Test
    @DisplayName("UT_EM_023: Trường hợp tìm users fail --> trả về RuntimeException")
    public void createExam_findUserByIntakeFail() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Intake intake = new Intake();
        intake.setId(1L);
        when(intakeService.findById(1L)).thenReturn(Optional.of(intake));

        Part part = new Part();
        part.setId(1L);
        when(partService.findPartById(1L)).thenReturn(Optional.of(part));

        Exam exam = new Exam();
        exam.setQuestionData("[]");
        when(examService.saveExam(exam)).thenReturn(exam);

        doThrow(new RuntimeException("Find users fail"))
                .when(userService).findAllByIntakeId(1L);


        //assert
        assertThrows(RuntimeException.class, () -> examController.createExam(exam, 1L, 1L, false, false));

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(1L);
        verify(partService, times(1)).findPartById(1L);
        verify(examService, times(1)).saveExam(exam);
        verify(userService, times(1)).findAllByIntakeId(1L);
        verify(examUserService, never()).create(exam, anyList());
    }

    @Test
    @DisplayName("UT_EM_024: Trường hợp lưu Exam fail --> trả về RuntimeException")
    public void createExam_saveExamFail() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Intake intake = new Intake();
        intake.setId(1L);
        when(intakeService.findById(1L)).thenReturn(Optional.of(intake));

        Part part = new Part();
        part.setId(1L);
        when(partService.findPartById(1L)).thenReturn(Optional.of(part));

        Exam exam = new Exam();
        exam.setQuestionData("[]");

        doThrow(new RuntimeException("Save exam fail"))
                .when(examService).saveExam(exam);


        //assert
        assertThrows(RuntimeException.class, () -> examController.createExam(exam, 1L, 1L, false, false));

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(1L);
        verify(partService, times(1)).findPartById(1L);
        verify(examService, times(1)).saveExam(exam);
        verify(userService, never()).findAllByIntakeId(1L);
        verify(examUserService, never()).create(exam, anyList());
    }

    @Test
    @DisplayName("UT_EM_025: Trường hợp không tìm thấy part--> trả về EntityNotExistsException")
    public void createExam_partNotExisted() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Intake intake = new Intake();
        intake.setId(1L);
        when(intakeService.findById(1L)).thenReturn(Optional.of(intake));

        Exam exam = new Exam();
        exam.setQuestionData("[]");


        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.createExam(exam, 1L, 999L, false, false));

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(1L);
        verify(partService, times(1)).findPartById(999L);
        verify(examService, never()).saveExam(exam);
        verify(userService, never()).findAllByIntakeId(1L);
        verify(examUserService, never()).create(exam, anyList());

    }

    @Test
    @DisplayName("UT_EM_026: Trường hợp không tìm thấy Intake--> trả về EntityNotExistsException")
    public void createExam_intakeNotExisted() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        user.setId(1L);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));


        Exam exam = new Exam();
        exam.setQuestionData("[]");


        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.createExam(exam, 999L, 999L, false, false));

        verify(userService, times(1)).getUserByUsername(username);
        verify(userService, times(1)).getUserName();
        verify(intakeService, times(1)).findById(999L);
        verify(partService, never()).findPartById(999L);
        verify(examService, never()).saveExam(exam);
        verify(userService, never()).findAllByIntakeId(1L);
        verify(examUserService, never()).create(exam, anyList());

    }

    @Test
    @DisplayName("UT_EM_027: Trường hợp lỗi khi lấy thông tin user (người tạo đề thi) " +
            "--> trả về RuntimeException")
    public void createExam_getUserInfoFail() throws Exception {
        //arrange
        String username = "student1";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        exam.setQuestionData("[]");

        //assert
        assertThrows(RuntimeException.class, () -> examController.createExam(exam, 999L, 999L, false, false));

        verify(userService, times(1)).getUserName();
        verify(userService, times(1)).getUserByUsername(username);
        verify(intakeService, never()).findById(1L);
        verify(partService, never()).findPartById(999L);
        verify(examService, never()).saveExam(exam);
        verify(userService, never()).findAllByIntakeId(1L);
        verify(examUserService, never()).create(exam, anyList());

    }

    @Test
    @DisplayName("UT_EM_028: Trường hợp lỗi khi lấy username --> trả về RuntimeException")
    public void createExam_getUsernameFail() throws Exception {
        //arrange
        Exam exam = new Exam();
        exam.setQuestionData("[]");

        //assert
        assertThrows(RuntimeException.class, () -> examController.createExam(exam, 999L, 999L, false, false));

        verify(userService, times(1)).getUserName();
        verify(userService, never()).getUserByUsername(anyString());
        verify(intakeService, never()).findById(1L);
        verify(partService, never()).findPartById(999L);
        verify(examService, never()).saveExam(exam);
        verify(userService, never()).findAllByIntakeId(1L);
        verify(examUserService, never()).create(exam, anyList());

    }

    @Test
    @DisplayName("UT_EM_029: Trường hợp lấy thông tin Exam thành công --> Trả về response status thành công, chứa thông tin Exam")
    public void getExamById_foundExam() {
        //arrange
        Exam exam = new Exam();
        exam.setId(1L);
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        //act
        ResponseEntity<Exam> response = examController.getExamById(1L);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(exam, response.getBody());
        verify(examService, times(1)).getExamById(1L);
    }

    @Test
    @DisplayName("UT_EM_030: Trường hợp không tìm thấy exam --> Trả về EntityNotExistsException")
    public void getExamById_notFoundExam() {

        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.getExamById(9999L));
        verify(examService, times(1)).getExamById(9999L);
    }

    @Test
    @DisplayName("UT_EM_031: Trường hợp lưu user exam khi chưa nộp bài " +
            "-->  lưu user exam thành công, thuộc tính isFinished của user exam là false," +
            " timeFinish là null")
    public void saveUserExamAnswer_saveSuccessWhenNotFinish() throws Exception {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        Exam exam = new Exam();
        exam.setId(1L);

        ExamUser examUser = new ExamUser();
        examUser.setId(1L);
        examUser.setIsFinished(false);

        when(examUserService.findByExamAndUser(1L, username)).thenReturn(examUser);

        List<AnswerSheet> answerSheets = generateAnswerSheet(5);
        Long examId = 1L;
        boolean isFinish = false;
        int remainingTime = 1000000;

        //act
        examController.saveUserExamAnswer(answerSheets, examId, isFinish, remainingTime);

        //assert
        ArgumentCaptor<ExamUser> captor = ArgumentCaptor.forClass(ExamUser.class);
        verify(examUserService, times(1)).update(captor.capture());

        ExamUser updatedUser = captor.getValue();
        assertEquals(remainingTime, updatedUser.getRemainingTime());
        assertFalse(updatedUser.getIsFinished());
        assertNull(updatedUser.getTimeFinish());
        assertEquals(generateAnswerSheetString(5),  updatedUser.getAnswerSheet());
    }

    @Test
    @DisplayName("UT_EM_032: Trường hợp lưu user exam khi nộp bài " +
            "--> Lưu user exam thành công, thuộc tính isFinish là true, có timeFinish")
    public void saveUserExamAnswer_saveSuccessWhenFinish() throws Exception {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        Exam exam = new Exam();
        exam.setId(1L);

        ExamUser examUser = new ExamUser();
        examUser.setId(1L);
        examUser.setIsFinished(false);

        when(examUserService.findByExamAndUser(1L, username)).thenReturn(examUser);

        List<AnswerSheet> answerSheets = generateAnswerSheet(5);
        Long examId = 1L;
        boolean isFinish = true;
        int remainingTime = 1000000;

        //act
        examController.saveUserExamAnswer(answerSheets, examId, isFinish, remainingTime);

        //assert
        ArgumentCaptor<ExamUser> captor = ArgumentCaptor.forClass(ExamUser.class);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examUserService, times(1)).update(captor.capture());

        ExamUser updatedUser = captor.getValue();
        assertEquals(remainingTime, updatedUser.getRemainingTime());
        assertTrue(updatedUser.getIsFinished());
        assertNotNull(updatedUser.getTimeFinish());
        assertEquals(generateAnswerSheetString(5),  updatedUser.getAnswerSheet());

    }

    @Test
    @DisplayName("UT_EM_033: Trường hợp lưu user exam khi user exam đã finished rồi " +
            " --> trả về IllegalStateException")
    public void saveUserExamAnswer_userExamHasFinished() throws Exception {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        Exam exam = new Exam();
        exam.setId(1L);

        ExamUser examUser = new ExamUser();
        examUser.setId(1L);
        examUser.setIsFinished(true);

        when(examUserService.findByExamAndUser(1L, username)).thenReturn(examUser);

        List<AnswerSheet> answerSheets = generateAnswerSheet(5);
        Long examId = 1L;
        boolean isFinish = true;
        int remainingTime = 1000000;

        //assert
        assertThrows(IllegalStateException.class, () -> examController.saveUserExamAnswer(answerSheets, examId, isFinish, remainingTime));
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_034: Trường hợp không tìm thấy exam user --> trả về EntityNotFoundException")
    public void saveUserExamAnswer_notFoundExamUser() throws Exception {
        //arrange
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        String username = "student1";

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);

        Exam exam = new Exam();
        exam.setId(1L);

        List<AnswerSheet> answerSheets = generateAnswerSheet(5);
        Long examId = 1L;
        boolean isFinish = true;
        int remainingTime = 1000000;

        //assert
        assertThrows(EntityNotFoundException.class, () -> examController.saveUserExamAnswer(answerSheets, examId, isFinish, remainingTime));
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
    }

    @Test
    @DisplayName("UT_EM_035: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult" +
            " chứa 1 examUser hết giờ mà chưa bắt đầu làm bài" +
            " --> Trả về response chứa danh sách ExamResult có 1 examResult với status -2")
    public void getResultExamAll_1ExamUserStatus_2() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 0L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);
        ExamUser examUser = createExamUser(exam, user, false, false,
                600000, null, -1.0);
        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(-2, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertNull(examResult.getTotalPoint());
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
    }

    @Test
    @DisplayName("UT_EM_036: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult " +
            "chưa bắt đầu làm bài nhưng còn thời gian " +
            "--> Trả về response chứa examResult với status 0")
    public void getResultExamAll_1ExamUserStatus0() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);
        ExamUser examUser = createExamUser(exam, user, false, false,
                600000, null, -1.0);
        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(0, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertNull(examResult.getTotalPoint());
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
    }

    @Test
    @DisplayName("UT_EM_037: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đã nộp bài," +
            " lần đầu xem kết quả, chứa 5 câu trả lời đúng --> " +
            "Trả về response chứa examResult với status -1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus_1_5ChoiceCorrect() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, true,
                600000, null, -1.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, true));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(-1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(50, examResult.getTotalPoint());
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examUserService, times(1)).update(examUser);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_038: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đã nộp bài," +
            " lần đầu xem kết quả, chứa 5 câu trả lời sai --> " +
            "Trả về response chứa examResult với status -1, tính tổng điểm chính xác bằng 0," +
            "cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus_1_5ChoiceWrong() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, true,
                600000, null, -1.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, false));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(-1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(0, examResult.getTotalPoint());
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examUserService, times(1)).update(examUser);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_039: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đã nộp bài, " +
            "không phải lần đầu xem kết quả, chứa 5 câu trả lời đúng --> " +
            "Trả về response chứa examResult với status -1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "không cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus_1_5ChoiceCorrect_NotFirst() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, true,
                600000, null, 50.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 50, true));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(-1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(50, examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, never()).update(examUser);
    }

    @Test
    @DisplayName("UT_EM_040: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đã nộp bài, " +
            "không phải lần đầu xem kết quả, chứa 5 câu trả lời sai --> " +
            "Trả về response chứa examResult với status -1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "không cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus_1_5ChoiceWrong_NotFirst() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, true,
                600000, null, 0.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, false));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(-1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(0, examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);
    }

    @Test
    @DisplayName("UT_EM_041: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đang làm bài," +
            " lần đầu xem kết quả, chứa 5 câu trả lời đúng --> " +
            "Trả về response chứa examResult với status 1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus1_5ChoiceCorrect() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, false,
                600000, null, -1.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, true));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(50, examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());

        ArgumentCaptor<ExamUser> examUserCaptor = ArgumentCaptor.forClass(ExamUser.class);
        verify(examUserService, times(1)).update(examUserCaptor.capture());
        assertEquals(50, examUserCaptor.getValue().getTotalPoint());
    }

    @Test
    @DisplayName("UT_EM_042: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đang làm bài," +
            " lần đầu xem kết quả, chứa 5 câu trả lời sai --> " +
            "Trả về response chứa examResult với status 1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus1_5ChoiceWrong() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, false,
                600000, null, -1.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, false));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(0, examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());

        ArgumentCaptor<ExamUser> examUserCaptor = ArgumentCaptor.forClass(ExamUser.class);
        verify(examUserService, times(1)).update(examUserCaptor.capture());
        assertEquals(0, examUserCaptor.getValue().getTotalPoint());
    }

    @Test
    @DisplayName("UT_EM_043: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đang làm bài, " +
            "không phải lần đầu xem kết quả, chứa 5 câu trả lời đúng --> " +
            "Trả về response chứa examResult với status 1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus1_5ChoiceCorrect_notFirst() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, false,
                600000, null, 10.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, true));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(50, examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());

        ArgumentCaptor<ExamUser> examUserCaptor = ArgumentCaptor.forClass(ExamUser.class);
        verify(examUserService, times(1)).update(examUserCaptor.capture());
        assertEquals(50, examUserCaptor.getValue().getTotalPoint());
    }

    @Test
    @DisplayName("UT_EM_044: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đang làm bài, " +
            "không phải lần đầu xem kết quả, chứa 5 câu trả lời sai --> " +
            "Trả về response chứa examResult với status 1, tính tổng điểm chính xác bằng điểm 5 câu hỏi," +
            "cập nhật lại điểm của examUser")
    public void getResultExamAll_1ExamUserStatus1_5ChoiceWrong_notFirst() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, false,
                600000, null, 10.0);
        String answerData = generateAnswerSheetString(5);
        examUser.setAnswerSheet(answerData);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            choiceLists.add(createChoiceList((long) i, 10, false));
        }

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(0, examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());

        ArgumentCaptor<ExamUser> examUserCaptor = ArgumentCaptor.forClass(ExamUser.class);
        verify(examUserService, times(1)).update(examUserCaptor.capture());
        assertEquals(0, examUserCaptor.getValue().getTotalPoint());
    }

    @Test
    @DisplayName("UT_EM_045: Trưòng hợp lấy ra danh sách ExamResult có 1 ExamResult đang làm bài, " +
            "chưa lưu đáp án --> " +
            "Trả về response chứa examResult với status 1, tổng điểm bằng null,")
    public void getResultExamAll_1ExamUserStatus1_notSave() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = createExamWithTime(false, false, -600000L, 600000L, null, 600000);
        exam.setId(examId);
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        User user = userWithRoles(ERole.ROLE_ADMIN);
        user.setId(1L);

        ExamUser examUser = createExamUser(exam, user, true, false,
                600000, null, -1.0);

        when(examUserService.findAllByExam_Id(examId)).thenReturn(Arrays.asList(examUser));


        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof ExamResult);
        ExamResult examResult = (ExamResult) ((List<?>) response.getBody()).get(0);
        assertEquals(examResult.getUser().getId(), user.getId());
        assertEquals(1, examResult.getExamStatus());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertNull(examResult.getTotalPoint());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
        verify(examService, never()).getChoiceList(anyList(), anyList());
        verify(examUserService, never()).update(examUser);
    }

    @Test
    @DisplayName("UT_EM_046:  Trưòng hợp lấy ra danh sách ExamResult có 5 ExamResult không làm bài " +
            "--> Trả về response chứa 5 examResult với status -2, tổng điểm bằng -1.0")
    public void getResultExamAll_With5ExamUsers() throws Exception {
        // arrange
        Long examId = 1L;

        Exam exam = createExamWithTime(false, false, -1200000L, -600000L, null, 600000);
        exam.setId(examId);
        exam.setQuestionData(generateExamQuestionPointString(5));
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));


        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            User user = userWithRoles(ERole.ROLE_STUDENT);
            user.setId((long) i);
            users.add(user);
        }

        List<ExamUser> examUsers = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            ExamUser examUser = createExamUser(exam, users.get(i), false, false, 60000, null, null);
            examUsers.add(examUser);
        }

        when(examUserService.findAllByExam_Id(examId)).thenReturn(examUsers);

        //act
        ResponseEntity response = examController.getResultExamAll(examId);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ExamResult> results = (List<ExamResult>) response.getBody();

        assertNotNull(results);
        assertEquals(5, results.size());

        assertEquals(-2, results.get(0).getExamStatus());
        assertEquals(-2, results.get(4).getExamStatus());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findAllByExam_Id(examId);
    }

    @Test
    @DisplayName("UT_EM_047: Trưòng hợp không tìm thấy Exam --> Trả về EntityNotFoundException")
    public void getResultExamAll_notFoundExam() throws Exception {
        // arrange
        Long examId = 999L;

        // assert
        assertThrows(EntityNotFoundException.class, () -> examController.getResultExamAll(examId));

        verify(examService, times(1)).getExamById(examId);
    }

    @Test
    @DisplayName("UT_EM_048: Trường hợp có 1 user exam, 1 choiceList, và choice đúng " +
            "--> Trả về response chứa 1 question exam report, tổng số trả lời đúng là 1")
    public void getResultExamQuestionReport_1ExamUser_Has1ChoiceList_choiceCorrect() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        String answerSheet = generateAnswerSheetString(1);
        ExamUser examUser = createExamUser(exam, new User(), true, true, 0, answerSheet, 10.0);
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        ChoiceList choiceList = createChoiceList(1L, 10, true);
        List<ChoiceList> choiceLists = Arrays.asList(choiceList);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);

        //act
        ResponseEntity response = examController.getResultExamQuestionsReport(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof QuestionExamReport);

        List<QuestionExamReport> questionExamReports = (List<QuestionExamReport>) response.getBody();
        QuestionExamReport questionExamReport = questionExamReports.get(0);
        assertEquals(1, questionExamReports.size());
        assertEquals(1, questionExamReport.getCorrectTotal());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findExamUsersByIsFinishedIsTrueAndExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_049: Trường hợp có 1 user exam, 1 choiceList, và choice sai " +
            "--> Trả về response chứa 1 question exam report, tổng số trả lời đúng là 0")
    public void getResultExamQuestionReport_1ExamUser_Has1ChoiceList_choiceWrong() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        String answerSheet = generateAnswerSheetString(1);
        ExamUser examUser = createExamUser(exam, new User(), true, true, 0, answerSheet, 10.0);
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        ChoiceList choiceList = createChoiceList(1L, 10, false);
        List<ChoiceList> choiceLists = Arrays.asList(choiceList);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);

        //act
        ResponseEntity response = examController.getResultExamQuestionsReport(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof QuestionExamReport);

        List<QuestionExamReport> questionExamReports = (List<QuestionExamReport>) response.getBody();
        QuestionExamReport questionExamReport = questionExamReports.get(0);
        assertEquals(1, questionExamReports.size());
        assertEquals(0, questionExamReport.getCorrectTotal());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findExamUsersByIsFinishedIsTrueAndExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_050: Trường hợp có 5 user exam, mỗi user exam có 1 choiceList," +
            " và 3 choice đúng, 2 choice sai " +
            "--> Trả về response chứa 1 question exam report, tổng số trả lời đúng là 3")
    public void getResultExamQuestionReport_5ExamUser_Has1ChoiceList_3Correcr2Wrong() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        String answerSheet = generateAnswerSheetString(1);

        List<ExamUser> examUsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            examUsers.add(createExamUser(exam, new User(), true, true, 0, generateAnswerSheetString(1), 10.0));
        }
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(examId)).thenReturn(examUsers);

        ChoiceList correctChoice = createChoiceList(1L, 10, true);
        ChoiceList wrongChoice = createChoiceList(1L, 10, false);

        when(examService.getChoiceList(anyList(), anyList()))
                .thenReturn(Arrays.asList(correctChoice))
                .thenReturn(Arrays.asList(correctChoice))
                .thenReturn(Arrays.asList(correctChoice))
                .thenReturn(Arrays.asList(wrongChoice))
                .thenReturn(Arrays.asList(wrongChoice));

        //act
        ResponseEntity response = examController.getResultExamQuestionsReport(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof QuestionExamReport);

        List<QuestionExamReport> questionExamReports = (List<QuestionExamReport>) response.getBody();
        QuestionExamReport questionExamReport = questionExamReports.get(0);
        assertEquals(1, questionExamReports.size());
        assertEquals(3, questionExamReport.getCorrectTotal());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findExamUsersByIsFinishedIsTrueAndExam_Id(examId);
        verify(examService, times(5)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_051: Trường hợp có 1 user exam, mỗi user exam có 5 choiceList," +
            " và 3 choice đúng, 2 choice sai " +
            "--> Trả về response chứa 5 question exam report," +
            " 3 question exam report có 1 câu đúng," +
            " 2 question exam report có 0 câu đúng")
    public void getResultExamQuestionReport_1ExamUser_Has5ChoiceList_3Correcr2Wrong() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        String answerSheet = generateAnswerSheetString(1);

        ExamUser examUser = createExamUser(exam, new User(), true, true, 0, answerSheet, 10.0);
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> lists = new  ArrayList<>();
        for (int i = 0; i < 3; i++) {
            lists.add(createChoiceList((long) i, 10, true));
        }
        for (int i = 3; i < 5; i++) {
            lists.add(createChoiceList((long) i, 10, false));
        }

        when(examService.getChoiceList(anyList(), anyList()))
                .thenReturn(lists);

        //act
        ResponseEntity response = examController.getResultExamQuestionsReport(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        assertTrue(((List)response.getBody()).get(0) instanceof QuestionExamReport);

        List<QuestionExamReport> questionExamReports = (List<QuestionExamReport>) response.getBody();
        assertEquals(5, questionExamReports.size());
        assertEquals(1, questionExamReports.get(0).getCorrectTotal());
        assertEquals(1, questionExamReports.get(1).getCorrectTotal());
        assertEquals(1, questionExamReports.get(2).getCorrectTotal());
        assertEquals(0, questionExamReports.get(3).getCorrectTotal());
        assertEquals(0, questionExamReports.get(4).getCorrectTotal());

        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findExamUsersByIsFinishedIsTrueAndExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_052: Trường hợp có 1 user exam, mỗi user exam có 0 choiceList," +
            "--> Trả về response chứa 0 question exam report")
    public void getResultExamQuestionReport_1ExamUser_Has0ChoiceList() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        String answerSheet = generateAnswerSheetString(1);

        ExamUser examUser = createExamUser(exam, new User(), true, true, 0, answerSheet, 10.0);
        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(examId)).thenReturn(Arrays.asList(examUser));

        List<ChoiceList> lists = new  ArrayList<>();
        when(examService.getChoiceList(anyList(), anyList()))
                .thenReturn(lists);

        //act
        ResponseEntity response = examController.getResultExamQuestionsReport(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);

        List<QuestionExamReport> questionExamReports = (List<QuestionExamReport>) response.getBody();
        assertEquals(0, questionExamReports.size());


        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findExamUsersByIsFinishedIsTrueAndExam_Id(examId);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
    }

    @Test
    @DisplayName("UT_EM_053: Trường hợp có 0 user exam" +
            "--> Trả về questionExamReports rỗng")
    public void getResultExamQuestionReport_0ExamUser() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));


        when(examUserService.findExamUsersByIsFinishedIsTrueAndExam_Id(examId)).thenReturn(new ArrayList<>());


        //act
        ResponseEntity response = examController.getResultExamQuestionsReport(examId);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);

        List<QuestionExamReport> questionExamReports = (List<QuestionExamReport>) response.getBody();
        assertEquals(0, questionExamReports.size());


        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findExamUsersByIsFinishedIsTrueAndExam_Id(examId);
    }

    @Test
    @DisplayName("UT_EM_054: Trường hợp có không tìm thấy exam" +
            "--> Trả về EntityNotExistedException")
    public void getResultExamQuestionReport_notFoundExam() throws Exception {
        //arrange
        Exam exam = new Exam();
        Long examId = 999L;

        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        //assert
        assertThrows(EntityNotFoundException.class, () -> examController.getResultExamQuestionsReport(examId));

        verify(examService, times(1)).getExamById(examId);

    }

    @Test
    @DisplayName("UT_EM_055: Trường hợp có 1 choice list, 1 choice đúng, lần đầu xem kết quả" +
            "--> Trả về response chứa exam result, điểm bằng điểm câu trả lời đúng," +
            " cập nhật exam user")
    public void getResultExamByUser_1ChoiceList_1ChoiceCorrect() throws Exception {
        //arrange
        User user =  new User();
        Long userId = 1L;
        String username = "user01";
        user.setId(userId);
        user.setUsername(username);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        Exam exam = createExamWithTime(false, false, -600000L,
                600000L, questionData, 60000);
        exam.setId(examId);
        when(examService.getExamById(exam.getId())).thenReturn(Optional.of(exam));

        ExamUser examUser = createExamUser(exam, user, true, true, 0, questionData, -1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        List<ChoiceList> lists = new  ArrayList<>();
        ChoiceList choiceList = createChoiceList(1L, 10, true);
        lists.add(choiceList);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(lists);

        //act
        ResponseEntity response = examController.getResultExamByUser(userId, username);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ExamResult);

        ExamResult examResult = (ExamResult) response.getBody();
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(exam.getDurationExam() * 60 - examUser.getRemainingTime(), examResult.getRemainingTime());
        assertEquals(10, examResult.getTotalPoint());
        assertSame(choiceList, examResult.getChoiceList().get(0));

        verify(examService, times(1)).getExamById(examId);
        verify(userService, times(1)).getUserByUsername(username);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_056: Trường hợp có 1 choice list, 1 choice sai, lần đầu xem kết quả" +
            "--> Trả về response chứa exam result, điểm bằng 0, " +
            "cập nhật exam user")
    public void getResultExamByUser_1ChoiceList_1ChoiceWrong() throws Exception {
        //arrange
        User user =  new User();
        Long userId = 1L;
        String username = "user01";
        user.setId(userId);
        user.setUsername(username);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        Exam exam = createExamWithTime(false, false, -600000L,
                600000L, questionData, 60000);
        exam.setId(examId);
        when(examService.getExamById(exam.getId())).thenReturn(Optional.of(exam));

        ExamUser examUser = createExamUser(exam, user, true, true, 0, questionData, -1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        List<ChoiceList> lists = new  ArrayList<>();
        ChoiceList choiceList = createChoiceList(1L, 10, false);
        lists.add(choiceList);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(lists);

        //act
        ResponseEntity response = examController.getResultExamByUser(userId, username);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ExamResult);

        ExamResult examResult = (ExamResult) response.getBody();
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(exam.getDurationExam() * 60 - examUser.getRemainingTime(), examResult.getRemainingTime());
        assertEquals(0, examResult.getTotalPoint());
        assertSame(choiceList, examResult.getChoiceList().get(0));

        verify(examService, times(1)).getExamById(examId);
        verify(userService, times(1)).getUserByUsername(username);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_057: Trường hợp có 1 choice list, 1 choice đúng, không phải lần đầu xem kết quả" +
            "--> Trả về response chứa exam result, điểm bằng 10, " +
            "cập nhật exam user")
    public void getResultExamByUser_1ChoiceList_1ChoiceCorrect_notFirst() throws Exception {
        //arrange
        User user =  new User();
        Long userId = 1L;
        String username = "user01";
        user.setId(userId);
        user.setUsername(username);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        Exam exam = createExamWithTime(false, false, -600000L,
                600000L, questionData, 60000);
        exam.setId(examId);
        when(examService.getExamById(exam.getId())).thenReturn(Optional.of(exam));

        ExamUser examUser = createExamUser(exam, user, true, true, 0, questionData, 20.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        List<ChoiceList> lists = new  ArrayList<>();
        ChoiceList choiceList = createChoiceList(1L, 10, true);
        lists.add(choiceList);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(lists);

        //act
        ResponseEntity response = examController.getResultExamByUser(userId, username);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ExamResult);

        ExamResult examResult = (ExamResult) response.getBody();
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(exam.getDurationExam() * 60 - examUser.getRemainingTime(), examResult.getRemainingTime());
        assertEquals(10, examResult.getTotalPoint());
        assertSame(choiceList, examResult.getChoiceList().get(0));

        verify(examService, times(1)).getExamById(examId);
        verify(userService, times(1)).getUserByUsername(username);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_058: Trường hợp có 5 choice list, 3 choice đúng, 2 choice sai" +
            "--> Trả về response chứa exam result, điểm bằng tồng điểm 3 câu đúng, " +
            "cập nhật exam user")
    public void getResultExamByUser_5ChoiceList_3Correct_2Wrong() throws Exception {
        //arrange
        User user =  new User();
        Long userId = 1L;
        String username = "user01";
        user.setId(userId);
        user.setUsername(username);
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        Exam exam = createExamWithTime(false, false, -600000L,
                600000L, questionData, 60000);
        exam.setId(examId);
        when(examService.getExamById(exam.getId())).thenReturn(Optional.of(exam));

        ExamUser examUser = createExamUser(exam, user, true, true, 0, questionData, -1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        List<ChoiceList> lists = new  ArrayList<>();
        for (int i = 0; i < 3; i++) {
            lists.add(createChoiceList((long) i, 10, true));
        }
        for (int i = 3; i < 5; i++) {
            lists.add(createChoiceList((long) i, 10, false));
        }

        when(examService.getChoiceList(anyList(), anyList()))
                .thenReturn(lists);

        //act
        ResponseEntity response = examController.getResultExamByUser(userId, username);

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ExamResult);

        ExamResult examResult = (ExamResult) response.getBody();
        assertEquals(examUser.getTimeFinish(), examResult.getUserTimeFinish());
        assertEquals(examUser.getTimeStart(), examResult.getUserTimeBegin());
        assertEquals(exam.getDurationExam() * 60 - examUser.getRemainingTime(), examResult.getRemainingTime());
        assertEquals(30, examResult.getTotalPoint());
        assertSame(lists, examResult.getChoiceList());

        verify(examService, times(1)).getExamById(examId);
        verify(userService, times(1)).getUserByUsername(username);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_059: Trường hợp không tìm thấy exam" +
            " --> Trả về EntityNotFoundException")
    public void getResultExamByUser_notFoundExam() throws Exception {
        //arrange
        Long userId = 1L;
        String username = "user01";

        Long examId = 999L;
        String questionData = generateExamQuestionPointString(1);
        Exam exam = createExamWithTime(false, false, -600000L,
                600000L, questionData, 60000);
        exam.setId(examId);

        //assert
        assertThrows(EntityNotFoundException.class, () -> examController.getResultExamByUser(examId, username));

        verify(examService, times(1)).getExamById(examId);
    }

    @Test
    @DisplayName("UT_EM_060: Trường hợp lấy thông tin user thất bại " +
            "--> trả về EntityNotFoundException")
    public void getResultExamByUser_getUserFail() throws Exception {
        //arrange
        User user =  new User();
        Long userId = 1L;
        String username = "user01";
        user.setId(userId);
        user.setUsername(username);

        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        Exam exam = createExamWithTime(false, false, -600000L,
                600000L, questionData, 60000);
        exam.setId(examId);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));


        //assert
        assertThrows(EntityNotFoundException.class, () -> examController.getResultExamByUser(userId, username));

        verify(examService, times(1)).getExamById(examId);
        verify(userService, times(1)).getUserByUsername(username);
    }

    @Test
    @DisplayName("UT_EM_061: Trường hợp dữ liệu hợp lệ, chuyển đổi thành công " +
            "--> Trả về List<AnswerSheet>")
    public void convertAnswerJsonToObject_convertSuccess() throws Exception {
        //arrange
        ExamUser examUser = new ExamUser();
        String answerSheetString = generateAnswerSheetString(2);
        examUser.setAnswerSheet(answerSheetString);

        //act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(examUser);

        //assert
        assertEquals(2, result.size());

        List<AnswerSheet> correct = generateAnswerSheet(2);
        assertEquals(correct.get(0).getChoices(), result.get(0).getChoices());
        assertEquals(correct.get(1).getChoices(), result.get(1).getChoices());

    }

    @Test
    @DisplayName("UT_EM_062: Trường hợp dữ liệu rỗng " +
            "--> Trả về List<AnswerSheet> rỗng")
    public void convertAnswerJsonToObject_answerEmpty() throws Exception {
        //arrange
        ExamUser examUser = new ExamUser();
        String answerSheetString = "";
        examUser.setAnswerSheet(answerSheetString);

        //act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(examUser);

        //assert
        assertEquals(0, result.size());

    }

    @Test
    @DisplayName("UT_EM_063: Trường hợp dữ liệu là Null " +
            "--> Trả về List<AnswerSheet> rỗng")
    public void convertAnswerJsonToObject_answerNull() throws Exception {
        //arrange
        ExamUser examUser = new ExamUser();
        String answerSheetString = null;
        examUser.setAnswerSheet(answerSheetString);

        //act
        List<AnswerSheet> result = examController.convertAnswerJsonToObject(examUser);

        //assert
        assertEquals(0, result.size());

    }

    @Test
    @DisplayName("UT_EM_064: Trường hợp dữ liệu không hợp lệ " +
            "--> Trả về InvalidPropertiesFormatException")
    public void convertAnswerJsonToObject_invalidAnswerJson() throws Exception {
        //arrange
        ExamUser examUser = new ExamUser();
        String answerSheetString = "[]][][]";
        examUser.setAnswerSheet(answerSheetString);

        //assert
        assertThrows(InvalidPropertiesFormatException.class,
                () -> examController.convertAnswerJsonToObject(examUser));

    }

    @Test
    @DisplayName("UT_EM_065: Trường hợp lấy được dữ liệu câu hỏi của exam thành công," +
            " exam có 1 question" +
            " --> Trả về List ExamDetail có 1 câu hỏi")
    public void getQuestionTextByExamId_getExamDetailSuccess() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = new  Exam();
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);

        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        long questionId = 1L;
        String questionText = "question 1";
        int point = 10;
        DifficultyLevel level = DifficultyLevel.MEDIUM;
        QuestionType type = new QuestionType();
        type.setDescription("MC");

        Question question = createQuestion(questionId, questionText, point, level, type);

        ExamDetail examDetail = new ExamDetail();
        examDetail.setQuestionText(questionText);
        examDetail.setPoint(point);
        examDetail.setQuestionType("MC");
        examDetail.setDifficultyLevel("MEDIUM");

        when(questionService.getQuestionById(questionId)).thenReturn(Optional.of(question));

        //act
        List<ExamDetail> result = examController.getQuestionTextByExamId(examId);

        //assert
        assertEquals(1, result.size());
        assertEquals(examDetail, result.get(0));
        verify(examService, times(1)).getExamById(examId);
        verify(questionService, times(1)).getQuestionById(questionId);

    }

    @Test
    @DisplayName("UT_EM_066: Trường hợp lấy được dữ liệu câu hỏi của exam thành công," +
            " exam có 5 question" +
            " --> Trả về List ExamDetail có 5 câu hỏi")
    public void getQuestionTextByExamId_getExamDetailSuccess_5Question() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = new  Exam();
        String questionData = generateExamQuestionPointString(5);
        exam.setId(examId);
        exam.setQuestionData(questionData);

        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        List<Question> questions = new ArrayList<>();
        List<ExamDetail> examDetails = new ArrayList<>();

        for (int i = 0; i < 5; ++i) {
            Long id = (long) i + 1;
            String questionText = "question " + (i + 1);
            int point = 10;
            DifficultyLevel level = DifficultyLevel.EASY;
            QuestionType type = new QuestionType();
            type.setDescription("MC");
            questions.add(createQuestion(id, questionText, point, level, type));
            when(questionService.getQuestionById(id)).thenReturn(Optional.of(questions.get(i)));

            ExamDetail examDetail = new ExamDetail();
            examDetail.setQuestionText(questionText);
            examDetail.setPoint(point);
            examDetail.setQuestionType("MC");
            examDetail.setDifficultyLevel("EASY");
            examDetails.add(examDetail);
        }


        //act
        List<ExamDetail> result = examController.getQuestionTextByExamId(examId);

        //assert
        assertEquals(5, result.size());
        assertEquals(examDetails.get(0), result.get(0));
        assertEquals(examDetails.get(4), result.get(4));

        verify(examService, times(1)).getExamById(examId);
        verify(questionService, times(5)).getQuestionById(anyLong());

    }

    @Test
    @DisplayName("UT_EM_067: Trường hợp lấy được dữ liệu câu hỏi thất bại," +
            " --> Trả về EntityNotExistsException")
    public void getQuestionTextByExamId_getQuestionFail() throws Exception {
        //arrange
        Long examId = 1L;
        Exam exam = new  Exam();
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);

        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.getQuestionTextByExamId(examId));
        verify(examService, times(1)).getExamById(examId);
        verify(questionService, times(1)).getQuestionById(anyLong());

    }

    @Test
    @DisplayName("UT_EM_068: Trường hợp không tìm thấy exam," +
            " --> Trả về EntityNotExistsException")
    public void getQuestionTextByExamId_getExamFail() throws Exception {
        //arrange
        Long examId = 999L;

        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.getQuestionTextByExamId(examId));
        verify(examService, times(1)).getExamById(examId);

    }

    @Test
    @DisplayName("UT_EM_069: Trường hợp chuyển đổi question thành công, exam có 1 question " +
            "--> Trả về List ExamQuestionPoint có 1 question")
    public void convertQuestionJsonToObject_convertSuccess() throws Exception {
        //arrange
        Exam exam = new Exam();
        String questionData = generateExamQuestionPointString(1);
        exam.setQuestionData(questionData);

        ExamQuestionPoint question = new ExamQuestionPoint();
        question.setQuestionId(1L);
        question.setPoint(10);

        //act
        List<ExamQuestionPoint> examQuestionPoints = examController.convertQuestionJsonToObject(Optional.of(exam));

        //assert
        assertEquals(1, examQuestionPoints.size());
        assertEquals(question, examQuestionPoints.get(0));
    }

    @Test
    @DisplayName("UT_EM_070: Trường hợp chuyển đổi question thất bại, lỗi định dạng " +
            "--> Trả về InvalidPropertiesFormatException")
    public void convertQuestionJsonToObject_invalidFormatQuestionString() throws Exception {
        //arrange
        Exam exam = new Exam();
        String questionData = "[][[[]";
        exam.setQuestionData(questionData);

        //assert
        assertThrows(InvalidPropertiesFormatException.class,
                () -> examController.convertQuestionJsonToObject(Optional.of(exam)));
    }

    @Test
    @DisplayName("UT_EM_071: Trường hợp optional<exam> truyền vào rỗng" +
            "--> Trả về IllegalArgumentException")
    public void convertQuestionJsonToObject_optionalEmpty() throws Exception {

        //assert
        assertThrows(IllegalArgumentException.class,
                () -> examController.convertQuestionJsonToObject(Optional.empty()));
    }

    @Test
    @DisplayName("UT_EM_072: Trường hợp chuyển đổi question thành công, exam có 5 question " +
            "--> Trả về List ExamQuestionPoint có 5 question")
    public void convertQuestionJsonToObject_convertSuccess_5Question() throws Exception {
        //arrange
        Exam exam = new Exam();
        String questionData = generateExamQuestionPointString(5);
        exam.setQuestionData(questionData);

        ExamQuestionPoint question1 = new ExamQuestionPoint();
        question1.setQuestionId(1L);
        question1.setPoint(10);

        List<ExamQuestionPoint> questions = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            ExamQuestionPoint question = new ExamQuestionPoint();
            question.setQuestionId(i);
            question.setPoint(10);
            questions.add(question);
        }

        //act
        List<ExamQuestionPoint> examQuestionPoints = examController.convertQuestionJsonToObject(Optional.of(exam));

        //assert
        assertEquals(5, examQuestionPoints.size());
        assertEquals(questions.get(0), examQuestionPoints.get(0));
        assertEquals(questions.get(4), examQuestionPoints.get(4));
    }

    @Test
    @DisplayName("UT_EM_073: Trường hợp user có 1 userexam không làm " +
            "--> Trả về List<ExamCalendar> có 1 examCalendar trạng thái Missed")
    public void getExamCalender_1ExamUserMissed() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Long examId = 1L;
        String title = "Exam " + examId;
        Part part = createPartWithCourse("Course", "001", "Part 1");
        Exam exam = createExamForCalendar(examId, part, title,
                -600000L, -500000L, 60000);
        ExamUser examUser = createExamUserForCalendar(exam, false, false);

        when(examUserService.getExamListByUsername(username)).thenReturn(Arrays.asList(examUser));

        //act
        List<ExamCalendar> examCalendars = examController.getExamCalendar();

        //assert
        assertEquals(1, examCalendars.size());
        ExamCalendar examCalendar = examCalendars.get(0);
        assertEquals("Missed", examCalendar.getCompleteString());
        assertEquals(-2, examCalendar.getIsCompleted());

        verify(userService, times(1)).getUserName();
        verify(examUserService, times(1)).getExamListByUsername(username);

    }

    @Test
    @DisplayName("UT_EM_074: Trường hợp user có 1 exam user chưa đến giờ làm" +
            "--> Trả về List<ExamCalendar> có 1 examCalendar trạng thái Not yet started")
    public void getExamCalender_1ExamUserNotYetStarted() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Long examId = 1L;
        String title = "Exam " + examId;
        Part part = createPartWithCourse("Course", "001", "Part 1");
        Exam exam = createExamForCalendar(examId, part, title,
                600000L, 650000L, 60000);
        ExamUser examUser = createExamUserForCalendar(exam, false, false);

        when(examUserService.getExamListByUsername(username)).thenReturn(Arrays.asList(examUser));

        //act
        List<ExamCalendar> examCalendars = examController.getExamCalendar();

        //assert
        assertEquals(1, examCalendars.size());
        ExamCalendar examCalendar = examCalendars.get(0);
        assertEquals("Not yet started", examCalendar.getCompleteString());
        assertEquals(0, examCalendar.getIsCompleted());

        verify(userService, times(1)).getUserName();
        verify(examUserService, times(1)).getExamListByUsername(username);

    }

    @Test
    @DisplayName("UT_EM_075: Trường hợp user có 1 exam user đã nộp bài rồi" +
            "--> Trả về List<ExamCalendar> có 1 examCalendar trạng thái Completed")
    public void getExamCalender_1ExamUserCompleted() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Long examId = 1L;
        String title = "Exam " + examId;
        Part part = createPartWithCourse("Course", "001", "Part 1");
        Exam exam = createExamForCalendar(examId, part, title,
                600000L, 650000L, 60000);
        ExamUser examUser = createExamUserForCalendar(exam, true, true);

        when(examUserService.getExamListByUsername(username)).thenReturn(Arrays.asList(examUser));

        //act
        List<ExamCalendar> examCalendars = examController.getExamCalendar();

        //assert
        assertEquals(1, examCalendars.size());
        ExamCalendar examCalendar = examCalendars.get(0);
        assertEquals("Completed", examCalendar.getCompleteString());
        assertEquals(-1, examCalendar.getIsCompleted());

        verify(userService, times(1)).getUserName();
        verify(examUserService, times(1)).getExamListByUsername(username);

    }

    @Test
    @DisplayName("UT_EM_076: Trường hợp user có 1 exam user trong thời gian làm bài" +
            "--> Trả về List<ExamCalendar> có 1 examCalendar trạng thái Doing")
    public void getExamCalender_1ExamUserDoing() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Long examId = 1L;
        String title = "Exam " + examId;
        Part part = createPartWithCourse("Course", "001", "Part 1");
        Exam exam = createExamForCalendar(examId, part, title,
                -600000L, 650000L, 60000);
        ExamUser examUser = createExamUserForCalendar(exam, false, false);

        when(examUserService.getExamListByUsername(username)).thenReturn(Arrays.asList(examUser));

        //act
        List<ExamCalendar> examCalendars = examController.getExamCalendar();

        //assert
        assertEquals(1, examCalendars.size());
        ExamCalendar examCalendar = examCalendars.get(0);
        assertEquals("Doing", examCalendar.getCompleteString());
        assertEquals(1, examCalendar.getIsCompleted());

        verify(userService, times(1)).getUserName();
        verify(examUserService, times(1)).getExamListByUsername(username);

    }

    @Test
    @DisplayName("UT_EM_077: Trường hợp user có 5 userexam trong thời gian làm bài" +
            "--> Trả về List<ExamCalendar> có 5 examCalendar trạng thái Doing")
    public void getExamCalender_5ExamUserMissed() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        List<ExamUser> examUsers = new ArrayList<>();

        for (long i = 1; i <= 5; ++i) {
            String title = "Exam " + i;
            Part part = createPartWithCourse("Course" + i, "Course" + i, "Part" + i);
            Exam exam = createExamForCalendar(i, part, title,
                    -600000L, -60000L, 60000);
            ExamUser examUser = createExamUserForCalendar(exam, false, false);
            examUsers.add(examUser);
        }

        when(examUserService.getExamListByUsername(username)).thenReturn(examUsers);

        //act
        List<ExamCalendar> examCalendars = examController.getExamCalendar();

        //assert
        assertEquals(5, examCalendars.size());
        ExamCalendar examCalendar1 = examCalendars.get(0);
        assertEquals("Missed", examCalendar1.getCompleteString());
        assertEquals(-2, examCalendar1.getIsCompleted());
        ExamCalendar examCalendar5 = examCalendars.get(4);
        assertEquals("Missed", examCalendar5.getCompleteString());
        assertEquals(-2, examCalendar5.getIsCompleted());

        verify(userService, times(1)).getUserName();
        verify(examUserService, times(1)).getExamListByUsername(username);

    }

    @Test
    @DisplayName("UT_EM_078: Trường hợp không tìm thấy exam user" +
            "--> Trả về EntityNotExistsException")
    public void getExamCalender_notFoundExamUser() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        //assert
        assertThrows(EntityNotExistsException.class, () -> examController.getExamCalendar());

        verify(userService, times(1)).getUserName();
        verify(examUserService, times(1)).getExamListByUsername(username);

    }

    @Test
    @DisplayName("UT_EM_079: Trường hợp lấy username thất bại" +
            "--> Trả về RuntimeException")
    public void getExamCalender_getUsernameFail() {

        //assert
        assertThrows(RuntimeException.class, () -> examController.getExamCalendar());

        verify(userService, times(1)).getUserName();

    }

    @Test
    @DisplayName("UT_EM_080: Trường hợp cancel thành công " +
            "--> không ném ra Exception")
    public void cancelExam_cancelSuccess() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Exam exam = new  Exam();
        exam.setId(1L);
        exam.setBeginExam( new Date(System.currentTimeMillis() + 60000));
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        //assert
        assertDoesNotThrow(() -> examController.cancelExam(1L));

        verify(userService, times(1)).getUserName();
        verify(userService, times(1)).getUserByUsername(username);
        verify(examService, times(1)).getExamById(1L);
        verify(examService, times(1)).cancelExam(1L);
    }

    @Test
    @DisplayName("UT_EM_081: Trường hợp bài thi đã bắt đầu" +
            "--> Trả về IllegalStateException")
    public void cancelExam_beginExamSmallerThanNow() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        Exam exam = new  Exam();
        exam.setId(1L);
        exam.setBeginExam( new Date(System.currentTimeMillis() - 60000));
        when(examService.getExamById(1L)).thenReturn(Optional.of(exam));

        //assert
        assertThrows(IllegalStateException.class ,() -> examController.cancelExam(1L));

        verify(userService, times(1)).getUserName();
        verify(userService, times(1)).getUserByUsername(username);
        verify(examService, times(1)).getExamById(1L);
    }

    @Test
    @DisplayName("UT_EM_082: Trường hợp không tìm thấy exam" +
            "--> Trả về EntityNotExistsException")
    public void cancelExam_notFoundExam() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        User user = new User();
        when(userService.getUserByUsername(username)).thenReturn(Optional.of(user));

        //assert
        assertThrows(EntityNotExistsException.class ,() -> examController.cancelExam(1L));

        verify(userService, times(1)).getUserName();
        verify(userService, times(1)).getUserByUsername(username);
        verify(examService, times(1)).getExamById(999L);
    }

    @Test
    @DisplayName("UT_EM_083: Trường hợp tìm user thất bại" +
            "--> Trả về EntityNotExistsException")
    public void cancelExam_notFoundUser() {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        //assert
        assertThrows(EntityNotExistsException.class ,() -> examController.cancelExam(1L));

        verify(userService, times(1)).getUserName();
        verify(userService, times(1)).getUserByUsername(username);
    }

    @Test
    @DisplayName("UT_EM_084: Trường hợp lấy username thất bại" +
            "--> Trả về RuntimeException")
    public void cancelExam_getUsernameFail() {
        //assert
        assertThrows(RuntimeException.class ,() -> examController.cancelExam(1L));

        verify(userService, times(1)).getUserName();
    }

    @Test
    @DisplayName("UT_EM_085: Trường hợp lấy kết quả bài thi thành công," +
            " bài thi có 1 câu hỏi, 1 câu trả lời đúng," +
            " lần đầu lấy ra kết quả" +
            "--> Trả về response chứa kết quả bài thi, điểm bằng điểm câu hỏi trả lời đúng, " +
            "cập nhật kết quả bài thi")
    public void getResultExam_1ChoiceList_1Correct() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        ExamUser examUser = new  ExamUser();
        String answerSheet = generateAnswerSheetString(1);
        examUser.setExam(exam);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(-1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        ChoiceList choiceList = createChoiceList(1L, 10, true);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(Arrays.asList(choiceList));

        //act
        ResponseEntity response = examController.getResultExam(examId);
        ExamResult examResult = (ExamResult) response.getBody();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, examResult.getChoiceList().size());
        assertEquals(10, examResult.getTotalPoint());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_086: Trường hợp lấy kết quả bài thi thành công," +
            " bài thi có 1 câu hỏi, 1 câu trả lời đúng," +
            " không phải lần đầu lấy ra kết quả" +
            "--> Trả về response chứa kết quả bài thi, điểm bằng điểm câu hỏi trả lời đúng," +
            " cập nhật kết quả bài thi")
    public void getResultExam_1ChoiceList_1Correct_notFirst() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        ExamUser examUser = new  ExamUser();
        String answerSheet = generateAnswerSheetString(1);
        examUser.setExam(exam);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        ChoiceList choiceList = createChoiceList(1L, 10, true);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(Arrays.asList(choiceList));

        //act
        ResponseEntity response = examController.getResultExam(examId);
        ExamResult examResult = (ExamResult) response.getBody();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, examResult.getChoiceList().size());
        assertEquals(10, examResult.getTotalPoint());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_087: Trường hợp lấy kết quả bài thi thành công," +
            " bài thi có 1 câu hỏi, 1 câu trả lời sai," +
            " lần đầu lấy ra kết quả" +
            "--> Trả về response chứa kết quả bài thi, điểm bằng 0," +
            " cập nhật kết quả bài thi")
    public void getResultExam_1ChoiceList_1Wrong() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        ExamUser examUser = new  ExamUser();
        String answerSheet = generateAnswerSheetString(1);
        examUser.setExam(exam);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(-1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        ChoiceList choiceList = createChoiceList(1L, 10, false);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(Arrays.asList(choiceList));

        //act
        ResponseEntity response = examController.getResultExam(examId);
        ExamResult examResult = (ExamResult) response.getBody();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, examResult.getChoiceList().size());
        assertEquals(0, examResult.getTotalPoint());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_088: Trường hợp lấy kết quả bài thi thành công," +
            " bài thi có 1 câu hỏi, 1 câu trả lời sai," +
            " không phải lần đầu lấy ra kết quả" +
            "--> Trả về response chứa kết quả bài thi, điểm bằng 0," +
            " cập nhật kết quả bài thi")
    public void getResultExam_1ChoiceList_1Wrong_notFirst() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        ExamUser examUser = new  ExamUser();
        String answerSheet = generateAnswerSheetString(1);
        examUser.setExam(exam);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        ChoiceList choiceList = createChoiceList(1L, 10, false);
        when(examService.getChoiceList(anyList(), anyList())).thenReturn(Arrays.asList(choiceList));

        //act
        ResponseEntity response = examController.getResultExam(examId);
        ExamResult examResult = (ExamResult) response.getBody();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, examResult.getChoiceList().size());
        assertEquals(0, examResult.getTotalPoint());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_089: Trường hợp lấy kết quả bài thi thành công," +
            " bài thi có 1 câu hỏi, không có câu trả lời nào" +
            " lần đầu lấy ra kết quả" +
            "--> Trả về response chứa kết quả bài thi, điểm bằng 0" +
            "cập nhật kết quả bài thi")
    public void getResultExam_1ChoiceList_0Answer() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(1);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        ExamUser examUser = new  ExamUser();
        String answerSheet = generateAnswerSheetString(0);
        examUser.setExam(exam);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(-1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        List<ChoiceList> choiceLists = new ArrayList<>();

        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);

        //act
        ResponseEntity response = examController.getResultExam(examId);
        ExamResult examResult = (ExamResult) response.getBody();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, examResult.getChoiceList().size());
        assertEquals(0, examResult.getTotalPoint());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_090: Trường hợp lấy kết quả bài thi thành công," +
            " bài thi có 5 câu hỏi, 3 câu trả lời đúng, 2 câu sai" +
            " lần đầu lấy ra kết quả" +
            "--> Trả về response chứa kết quả bài thi, điểm bằng điểm 3 câu hỏi trả lời đúng, " +
            "cập nhật kết quả bài thi")
    public void getResultExam_5ChoiceList_3Correct_2Wrong() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        Exam exam = new Exam();
        Long examId = 1L;
        String questionData = generateExamQuestionPointString(5);
        exam.setId(examId);
        exam.setQuestionData(questionData);
        when(examService.getExamById(examId)).thenReturn(Optional.of(exam));

        ExamUser examUser = new  ExamUser();
        String answerSheet = generateAnswerSheetString(1);
        examUser.setExam(exam);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(-1.0);
        when(examUserService.findByExamAndUser(examId, username)).thenReturn(examUser);

        List<ChoiceList> choiceLists = new ArrayList<>();
        for (long i = 1; i <= 3; ++i) {
            ChoiceList choiceList = createChoiceList(i, 10, true);
            choiceLists.add(choiceList);
        }

        for (long i = 4; i <= 5; ++i) {
            ChoiceList choiceList = createChoiceList(i, 10, false);
            choiceLists.add(choiceList);
        }


        when(examService.getChoiceList(anyList(), anyList())).thenReturn(choiceLists);

        //act
        ResponseEntity response = examController.getResultExam(examId);
        ExamResult examResult = (ExamResult) response.getBody();

        //assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, examResult.getChoiceList().size());
        assertEquals(30, examResult.getTotalPoint());

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(examId);
        verify(examUserService, times(1)).findByExamAndUser(examId, username);
        verify(examService, times(1)).getChoiceList(anyList(), anyList());
        verify(examUserService, times(1)).update(examUser);

    }

    @Test
    @DisplayName("UT_EM_091: Trường hợp không tìm thấy exam," +
            "--> Trả về EntityNotExistsExeption ")
    public void getResultExam_notFoundExam() throws Exception {
        //arrange
        String username = "user01";
        when(userService.getUserName()).thenReturn(username);

        //assert
        assertThrows(EntityNotExistsException.class,  () -> examController.getResultExam(999L));

        verify(userService, times(1)).getUserName();
        verify(examService, times(1)).getExamById(999L);

    }

    private User userWithRoles(ERole... roleNames) {
        User user = new User();
        Set<Role> roles = new HashSet<Role>();
        for (ERole roleName : roleNames) {
            Role role = new Role();
            role.setName(roleName);
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }

    private Exam createExamForCalendar(Long id, Part part, String title, long beginOffsetMs,
                                       long finishOffsetMs, int durationMinutes) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setPart(part);
        exam.setTitle(title);
        exam.setBeginExam(new Date(System.currentTimeMillis() + beginOffsetMs));
        exam.setFinishExam(new Date(System.currentTimeMillis() + finishOffsetMs));
        exam.setDurationExam(durationMinutes);

        return exam;
    }

    private Exam createExamWithTime(boolean locked, boolean shuffle, long beginOffsetMs, long finishOffsetMs,
                                    String questionData, int durationMinutes) {
        Exam exam = new Exam();
        exam.setLocked(locked);
        exam.setShuffle(shuffle);
        exam.setBeginExam(new Date(System.currentTimeMillis() + beginOffsetMs));
        exam.setFinishExam(new Date(System.currentTimeMillis() + finishOffsetMs));
        exam.setDurationExam(durationMinutes);
        exam.setQuestionData(questionData);
        exam.setTitle("Mock Exam");
        return exam;
    }

    private ExamUser createExamUser(Exam exam, User user, boolean started, boolean finished,
                                    int remainingTime, String answerSheet, Double totalPoint) {
        ExamUser examUser = new ExamUser();
        examUser.setExam(exam);
        examUser.setUser(user);
        examUser.setIsStarted(started);
        examUser.setIsFinished(finished);
        examUser.setRemainingTime(remainingTime);
        examUser.setAnswerSheet(answerSheet);
        examUser.setTotalPoint(totalPoint);
        return examUser;
    }

    private ExamUser createExamUserForCalendar(Exam exam, boolean started, boolean finished) {
        ExamUser examUser = new ExamUser();
        examUser.setExam(exam);
        examUser.setIsStarted(started);
        examUser.setIsFinished(finished);
        return examUser;
    }

    private List<AnswerSheet> generateAnswerSheet(int count) throws Exception {
        List<AnswerSheet> list = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            Choice c = new Choice();
            c.setId(i);
            c.setChoiceText("Choice " + i);
            list.add(new AnswerSheet(i, Collections.singletonList(c), 10));
        }
        return list;
    }

    private String generateAnswerSheetString(int count) throws Exception {
        List<AnswerSheet> list = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            Choice c = new Choice();
            c.setId(i);
            c.setChoiceText("Choice " + i);
            list.add(new AnswerSheet(i, Collections.singletonList(c), 10));
        }
        return mapper.writeValueAsString(list);
    }

    private String generateExamQuestionPointString(int count) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("questionId", i);
            item.put("point", 10);
            list.add(item);
        }
        return mapper.writeValueAsString(list);
    }

    private ChoiceList createChoiceList(Long questionId, int point, boolean isCorrect) {
        ChoiceList choiceList = new ChoiceList();

        Question question = new Question();
        question.setId(questionId);
        question.setQuestionText("Câu hỏi số " + questionId);

        choiceList.setQuestion(question);
        choiceList.setPoint(point);
        choiceList.setIsSelectedCorrected(isCorrect);

        choiceList.setChoices(new ArrayList<>());

        return choiceList;
    }

    private Question createQuestion(Long id, String questionText, int point,
                                    DifficultyLevel difficultyLevel, QuestionType questionType) {
        Question question = new Question();
        question.setId(id);
        question.setQuestionText(questionText);
        question.setPoint(point);
        question.setDifficultyLevel(difficultyLevel);
        question.setQuestionType(questionType);

        return question;
    }

    private Part createPartWithCourse(String courseName, String courseCode, String partName) {
        Course course = new Course();
        course.setName(courseName);
        course.setCourseCode(courseCode);
        Part part = new Part();
        part.setName(partName);
        part.setCourse(course);

        return part;
    }
}

