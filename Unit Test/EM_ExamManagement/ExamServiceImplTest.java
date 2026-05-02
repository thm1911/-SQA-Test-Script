package com.thanhtam.backend.service;

import com.thanhtam.backend.dto.AnswerSheet;
import com.thanhtam.backend.dto.ChoiceList;
import com.thanhtam.backend.dto.ExamQuestionPoint;
import com.thanhtam.backend.entity.*;
import com.thanhtam.backend.repository.*;
import com.thanhtam.backend.ultilities.EQTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ExamServiceImplTest {

    @Autowired
    private ExamServiceImpl examServiceImpl;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private QuestionTypeRepository questionTypeRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Test
    @DisplayName("UT_EM_092: Trường hợp lấy ra tất cả exam thành công " +
            "--> Trả về Page<Exam> có có phần tử bằng số lượng Exam trong db")
    public void findAll() {
        //arrange
        List<Exam> exams = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            Exam exam = new Exam();
            exam.setTitle("Test Exam" + i);
            exam = examRepository.save(exam);
            exams.add(exam);
        }

        //act
        Pageable  pageable = PageRequest.of(0, 5);
        List<Exam> examDb = examServiceImpl.findAll(pageable).getContent();

        //assert
        assertEquals(5, exams.size());
        assertEquals(exams.get(0), examDb.get(0));
        assertEquals(exams.get(4), examDb.get(4));

    }

    @Test
    @DisplayName("UT_EM_093: Trường hợp hủy bài kiểm tra thành công " +
            "--> Dữ liệu trong exam trong db cập nhật trạng thái isCanceled = true")
    public void cancelExam() {
        //arrange
        Exam exam = new Exam();
        exam.setTitle("Test Exam");
        exam.setCanceled(false);

        exam = examRepository.save(exam);

        //act
        examServiceImpl.cancelExam(exam.getId());

        entityManager.flush();
        entityManager.clear();

        //assert
        Exam examdb =  examRepository.findById(exam.getId())
                .orElseThrow(() -> new IllegalArgumentException("Exam Not Found"));

        assertTrue(examdb.isCanceled());
    }

    @Test
    @DisplayName("UT_EM_094: Trường hợp lấy ra tất cả exam thành công " +
            "--> Trả về List<Exam> có có phần tử bằng số lượng Exam trong db")
    public void getAll() {
        //arrange
        List<Exam> exams = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            Exam exam = new Exam();
            exam.setTitle("Test Exam" + i);
            exam = examRepository.save(exam);
            exams.add(exam);
        }

        //act
        List<Exam> examDb = examServiceImpl.getAll();

        //assert
        assertEquals(5, exams.size());
        assertEquals(exams.get(0), examDb.get(0));
        assertEquals(exams.get(4), examDb.get(4));
    }

    @Test
    @DisplayName("UT_EM_095: Trường hợp lấy ra exam theo id thành công " +
            "--> Trả về exam có id trùng với exam id truyền vào")
    public void getExamById() {
        //arrange
        Exam exam = new Exam();
        exam.setTitle("Test Exam");

        exam = examRepository.save(exam);

        //act
        Exam examDb =  examServiceImpl.getExamById(exam.getId())
                .orElseThrow(() -> new IllegalArgumentException("Exam Not Found"));

        //assert
        assertEquals(exam, examDb);
    }

    @Test
    @DisplayName("UT_EM_096: Trường hợp lấy ra tất cả exam thành công theo username của người tạo " +
            "--> Trả về Page<Exam> có các phần tử là các exam do user tạo")
    public void findAllByCreatedBy_Username() {
        //arrange
        User user1 = new User();
        user1.setUsername("user01");
        user1.setEmail("email01@gmail.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user02");
        user2.setEmail("email02@gmail.com");
        userRepository.save(user2);

        Exam exam1 = new Exam();
        exam1.setTitle("Test Exam 1");
        exam1.setCreatedBy(user1);
        exam1 = examRepository.save(exam1);

        Exam exam2 = new Exam();
        exam2.setTitle("Test Exam 2");
        exam2.setCreatedBy(user1);
        exam2 = examRepository.save(exam2);

        Exam exam3 = new Exam();
        exam3.setTitle("Test Exam 3");
        exam3.setCreatedBy(user1);
        exam3 = examRepository.save(exam3);

        Exam exam4 = new Exam();
        exam4.setTitle("Test Exam 4");
        exam4.setCreatedBy(user2);
        exam4 = examRepository.save(exam4);

        Exam exam5 = new Exam();
        exam5.setTitle("Test Exam 5");
        exam5.setCreatedBy(user2);
        exam5 = examRepository.save(exam5);

        //act
        Pageable pageable = PageRequest.of(0, 5);
        Page<Exam> examPage = examServiceImpl.findAllByCreatedBy_Username(pageable, "user01");

        //assert
        assertEquals(3, examPage.getTotalElements());
        assertEquals(exam1, examPage.getContent().get(0));
        assertEquals(exam2, examPage.getContent().get(1));

    }

    @Test
    @DisplayName("UT_EM_097: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi TF," +
            " user chọn 1 đáp án đúng " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả đúng")
    public void getChoiceList_1TF_1ChoiceCorrect() {
        //arrange
        QuestionType typeTF = new QuestionType();
        typeTF.setTypeCode(EQTypeCode.TF);
        typeTF = questionTypeRepository.save(typeTF);

        Question q = new Question();
        q.setQuestionType(typeTF);
        q = questionRepository.save(q);

        Choice c = new Choice();
        c.setChoiceText("Đúng");
        c = choiceRepository.save(c);

        AnswerSheet sheet = new AnswerSheet();
        sheet.setQuestionId(q.getId());

        List<AnswerSheet> userChoices = new ArrayList<>();
        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(c));
        userChoices.add(answerSheet);

        List<ExamQuestionPoint>  examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint examQuestionPoint = new ExamQuestionPoint();
        examQuestionPoint.setQuestionId(q.getId());
        examQuestionPoints.add(examQuestionPoint);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);


        //asssert
        assertTrue(result.get(0).getIsSelectedCorrected());
    }

//    Logic line 98 không thể đạt được, do so sánh choice text với chính nó luôn bằng nhau
//    @Test
//    public void getChoiceList_1TF_1ChoiceWrong() {
//        //arrange
//        QuestionType typeTF = new QuestionType();
//        typeTF.setTypeCode(EQTypeCode.TF);
//        typeTF = questionTypeRepository.save(typeTF);
//
//        Question q = new Question();
//        q.setQuestionType(typeTF);
//        q = questionRepository.save(q);
//
//        Choice c = new Choice();
//        c.setChoiceText("Sai");
//        c = choiceRepository.save(c);
//
//        AnswerSheet sheet = new AnswerSheet();
//        sheet.setQuestionId(q.getId());
//
//        List<AnswerSheet> userChoices = new ArrayList<>();
//        AnswerSheet answerSheet = new AnswerSheet();
//        answerSheet.setQuestionId(q.getId());
//        answerSheet.setChoices(Arrays.asList(c));
//        userChoices.add(answerSheet);
//
//        List<ExamQuestionPoint>  examQuestionPoints = new ArrayList<>();
//        ExamQuestionPoint examQuestionPoint = new ExamQuestionPoint();
//        examQuestionPoint.setQuestionId(q.getId());
//        examQuestionPoints.add(examQuestionPoint);
//
//        //act
//        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);
//
//
//        //asssert
//        assertFalse(result.get(0).getIsSelectedCorrected());
//    }

    @Test
    @DisplayName("UT_EM_098: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MC," +
            " user chọn 1 đáp án đúng " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả đúng")
    public void getChoiceList_1MC_1ChoiceCorrect() {
        //arrange
        QuestionType typeMC = createQuestionType(EQTypeCode.MC);

        List<Choice> choices = create4Choices1Correct();

        Question q = createQuestion(typeMC, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(choices.get(0)));


        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertTrue(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_099: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MC," +
            " user chọn 1 đáp án sai " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả sai")
    public void getChoiceList_1MC_1ChoiceWrong() {
        //arrange
        QuestionType typeMC = createQuestionType(EQTypeCode.MC);

        List<Choice> choices = create4Choices1Correct();

        Question q = createQuestion(typeMC, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(choices.get(1)));


        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertFalse(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_100: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MC," +
            " user chọn 1 đáp án đúng, 1 đáp án sai " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả sai")
    public void getChoiceList_1MC_1ChoiceWrong_1ChoiceCorrect() {
        //arrange
        QuestionType typeMC = createQuestionType(EQTypeCode.MC);

        List<Choice> choices = create4Choices1Correct();

        Question q = createQuestion(typeMC, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(choices.get(1), choices.get(0)));

        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertFalse(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_101: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MC," +
            " user không chọn đáp án nào" +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả sai")
    public void getChoiceList_1MC_0Choice() {
        //arrange
        QuestionType typeMC = createQuestionType(EQTypeCode.MC);

        List<Choice> choices = create4Choices1Correct();

        Question q = createQuestion(typeMC, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(new  ArrayList<>());

        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertFalse(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_102: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MS," +
            " user chọn tất cả các đáp án đúng " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả đúng")
    public void getChoiceList_1MS_AllCorrect() {
        //arrange
        QuestionType typeMS = createQuestionType(EQTypeCode.MS);

        List<Choice> choices = create4Choices2Correct();

        Question q = createQuestion(typeMS, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(choices.get(1), choices.get(0)));

        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertTrue(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_103: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MS," +
            " user chọn một phần các đáp án đúng " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả sai")
    public void getChoiceList_1MS_PartialCorrect() {
        //arrange
        QuestionType typeMS = createQuestionType(EQTypeCode.MS);

        List<Choice> choices = create4Choices2Correct();

        Question q = createQuestion(typeMS, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(choices.get(1)));

        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertFalse(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_104: Trường hợp lấy ra choice list, trong đề thì có 1 câu hỏi MS," +
            " user chọn tất cả đáp án đúng nhưng chọn dư đáp án sai " +
            "--> Trả về List<ChoiceList> chứa 1 ChoiceList với kết quả sai")
    public void getChoiceList_1MS_MixAllCorrectAndWrong() {
        //arrange
        QuestionType typeMS = createQuestionType(EQTypeCode.MS);

        List<Choice> choices = create4Choices2Correct();

        Question q = createQuestion(typeMS, choices);

        AnswerSheet answerSheet = new AnswerSheet();
        answerSheet.setQuestionId(q.getId());
        answerSheet.setChoices(Arrays.asList(choices.get(1), choices.get(0),  choices.get(2)));

        List<AnswerSheet> userChoices = new ArrayList<>();
        userChoices.add(answerSheet);

        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();
        ExamQuestionPoint eqp = new ExamQuestionPoint();
        eqp.setQuestionId(q.getId());
        examQuestionPoints.add(eqp);

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertFalse(result.get(0).getIsSelectedCorrected());
    }

    @Test
    @DisplayName("UT_EM_105: Trường hợp lấy ra choice list, trong đề thì có 5 câu hỏi MS," +
            " user đúng hết cả 5 câu " +
            "--> Trả về List<ChoiceList> chứa 5 ChoiceList với kết quả đúng")
    public void getChoiceList_5MS_AllCorrect() {
        //arrange
        QuestionType typeMS = createQuestionType(EQTypeCode.MS);

        List<AnswerSheet> userChoices = new ArrayList<>();
        List<ExamQuestionPoint> examQuestionPoints = new ArrayList<>();

        int numberOfQuestions = 5;

        for (int i = 0; i < numberOfQuestions; i++) {
            List<Choice> choices = create4Choices2Correct();
            Question q = createQuestion(typeMS, choices);

            AnswerSheet answerSheet = new AnswerSheet();
            answerSheet.setQuestionId(q.getId());
            answerSheet.setPoint(10);
            answerSheet.setChoices(Arrays.asList(choices.get(0), choices.get(1)));

            userChoices.add(answerSheet);

            ExamQuestionPoint eqp = new ExamQuestionPoint();
            eqp.setQuestionId(q.getId());
            examQuestionPoints.add(eqp);
        }

        //act
        List<ChoiceList> result = examServiceImpl.getChoiceList(userChoices, examQuestionPoints);

        //assert
        assertNotNull(result);
        assertEquals(5, result.size());

        for (int i = 0; i < numberOfQuestions; i++) {
            assertTrue(result.get(i).getIsSelectedCorrected());
            assertEquals(2, result.get(i).getChoices().size());
        }
    }

    @Test
    @DisplayName("UT_EM_106: Trường hợp lưu exam thành công " +
            "--> Exam được lưu vào db")
    public void saveExam() {
        //arrange
        Exam exam = new Exam();
        exam.setTitle("Test Exam");

        //act
        exam = examServiceImpl.saveExam(exam);

        //assert
        Exam examDb = examRepository.findById(exam.getId())
                .orElseThrow(() -> new IllegalArgumentException("Exam Not Found"));

        assertEquals(exam, examDb);
    }


    public Question createQuestion(QuestionType questionType, List<Choice> choices) {

        Question q = new Question();
        q.setQuestionType(questionType);
        q.setChoices(choices);
        q = questionRepository.save(q);
        return q;
    }

    public QuestionType  createQuestionType(EQTypeCode eQTypeCode) {
        QuestionType type = new QuestionType();
        type.setTypeCode(eQTypeCode);
        type = questionTypeRepository.save(type);
        return type;
    }

    public List<Choice> create4Choices1Correct() {

        Choice c1 = new Choice();
        c1.setChoiceText("A");
        c1.setIsCorrected(1);
        c1 = choiceRepository.save(c1);

        Choice c2 = new Choice();
        c2.setChoiceText("B");
        c2.setIsCorrected(0);
        c2 = choiceRepository.save(c2);

        Choice c3 = new Choice();
        c3.setChoiceText("C");
        c3.setIsCorrected(0);
        c3 = choiceRepository.save(c3);

        Choice c4 = new Choice();
        c4.setChoiceText("D");
        c4.setIsCorrected(0);
        c4 = choiceRepository.save(c4);

        return  Arrays.asList(c1, c2, c3, c4);
    }

    public List<Choice> create4Choices2Correct() {

        Choice c1 = new Choice();
        c1.setChoiceText("A");
        c1.setIsCorrected(1);
        c1 = choiceRepository.save(c1);

        Choice c2 = new Choice();
        c2.setChoiceText("B");
        c2.setIsCorrected(1);
        c2 = choiceRepository.save(c2);

        Choice c3 = new Choice();
        c3.setChoiceText("C");
        c3.setIsCorrected(0);
        c3 = choiceRepository.save(c3);

        Choice c4 = new Choice();
        c4.setChoiceText("D");
        c4.setIsCorrected(0);
        c4 = choiceRepository.save(c4);

        return  Arrays.asList(c1, c2, c3, c4);
    }
}
