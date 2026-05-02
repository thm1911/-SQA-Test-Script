package com.thanhtam.backend.service;

import com.amazonaws.services.workdocs.model.EntityNotExistsException;
import com.thanhtam.backend.entity.*;
import com.thanhtam.backend.repository.*;
import com.thanhtam.backend.ultilities.ERole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ExamUserServiceImplTest {
    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamUserRepository examUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ExamUserServiceImpl examUserServiceImpl;

    @Test
    @DisplayName("UT_EM_107: Tạo exam user với 1 user --> 1 exam user được lưu vào db")
    public void create_1User() {
        //arrange
        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);
        List<User> userSet = new ArrayList<>();
        userSet.add(user1);

        Exam exam = createExam("Exam",60000);

        //act
        examUserServiceImpl.create(exam, userSet);

        entityManager.flush();
        entityManager.clear();

        //assert
        ExamUser examUserDb = examUserRepository.findByExam_IdAndUser_Username(exam.getId(), user1.getUsername());

        assertNotNull(examUserDb);
        assertEquals(exam,  examUserDb.getExam());
        assertEquals(user1, examUserDb.getUser());
    }

    @Test
    @DisplayName("UT_EM_108: Tạo exam user với 0 user --> trả về Exception")
    public void create_0User() {
        //arrange
        List<User> userSet = new ArrayList<>();

        Exam exam = createExam("Exan",60000);

        //assert
        assertThrows(IllegalArgumentException.class, () -> examUserServiceImpl.create(exam, userSet));

    }

    @Test
    @DisplayName("UT_EM_109: Tạo exam user với 5 user --> 5 exam user được lưu vào db")
    public void create_5User() {
        //arrange
        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);
        User user2 = createUser("user02", "mail02@gmail.com", ERole.ROLE_STUDENT);
        User user3 = createUser("user03", "mail03@gmail.com", ERole.ROLE_STUDENT);
        User user4 = createUser("user04", "mail04@gmail.com", ERole.ROLE_STUDENT);
        User user5 = createUser("user05", "mail05@gmail.com", ERole.ROLE_STUDENT);
        List<User> userSet = new ArrayList<>();
        userSet.add(user1);
        userSet.add(user2);
        userSet.add(user3);
        userSet.add(user4);
        userSet.add(user5);

        Exam exam = createExam("Exam",60000);

        //act
        examUserServiceImpl.create(exam, userSet);

        entityManager.flush();
        entityManager.clear();

        //assert
        ExamUser examUserDb1 = examUserRepository.findByExam_IdAndUser_Username(exam.getId(), user1.getUsername());
        assertEquals(exam,  examUserDb1.getExam());
        assertEquals(user1, examUserDb1.getUser());

        ExamUser examUserDb2 = examUserRepository.findByExam_IdAndUser_Username(exam.getId(), user2.getUsername());
        assertEquals(exam,  examUserDb2.getExam());
        assertEquals(user2, examUserDb2.getUser());

        ExamUser examUserDb3 = examUserRepository.findByExam_IdAndUser_Username(exam.getId(), user3.getUsername());
        assertEquals(exam,  examUserDb3.getExam());
        assertEquals(user3, examUserDb3.getUser());

        ExamUser examUserDb4 = examUserRepository.findByExam_IdAndUser_Username(exam.getId(), user4.getUsername());
        assertEquals(exam,  examUserDb4.getExam());
        assertEquals(user4, examUserDb4.getUser());

        ExamUser examUserDb5 = examUserRepository.findByExam_IdAndUser_Username(exam.getId(), user5.getUsername());
        assertEquals(exam,  examUserDb5.getExam());
        assertEquals(user5, examUserDb5.getUser());
    }

    @Test
    @DisplayName("UT_EM_110: Lấy ra danh sách ExamUser theo username " +
            "--> trả về danh sách ExamUser của user")
    public void getExamListByUsername() {
        //arrange
        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);

        List<Exam> examList = new ArrayList<>();
        List<ExamUser> examUserList = new ArrayList<>();

        for (int i  = 0; i < 5; i++) {
            Exam exam = createExam("Exam " + (i + 1),60000);
            examList.add(exam);
            examUserList.add(createExamUser(exam, user1));
        }

        //act
        List<ExamUser> examUserDb = examUserServiceImpl.getExamListByUsername(user1.getUsername());

        //assert
        assertEquals(examUserList.size(), examUserDb.size());
        assertEquals(examUserList.get(0).getExam(), examUserDb.get(0).getExam());
        assertEquals(examUserList.get(1).getExam(), examUserDb.get(1).getExam());
        assertEquals(examUserList.get(2).getExam(), examUserDb.get(2).getExam());
        assertEquals(examUserList.get(3).getExam(), examUserDb.get(3).getExam());
        assertEquals(examUserList.get(4).getExam(), examUserDb.get(4).getExam());
    }

    @Test
    @DisplayName("UT_EM_111: Lấy ra  ExamUser theo examId và username " +
            "--> trả về ExamUser có examId và user trùng khớp với dữ liệu truyền vào ")
    public void findByExamAndUser() {
        //arrange
        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);
        Exam exam1 = createExam("Exam 1",60000);
        ExamUser examUser1 = createExamUser(exam1, user1);

        //act
        ExamUser examUserDb = examUserServiceImpl.findByExamAndUser(exam1.getId(), user1.getUsername());

        //assert
        assertEquals(examUser1.getId(), examUserDb.getId());
        assertEquals(exam1, examUserDb.getExam());
        assertEquals(user1, examUserDb.getUser());
    }

    @Test
    @DisplayName("UT_EM_112: Cập nhật ExamUser " +
            "--> dữ liệu exam user trong db được cập nhật ")
    public void update() {
        //arrange
        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);
        Exam exam1 = createExam("Exam 1",60000);
        ExamUser examUser1 = createExamUser(exam1, user1);

        examUser1.setTotalPoint(10.0);
        examUser1.setIsStarted(true);

        //act
        examUserServiceImpl.update(examUser1);

        entityManager.flush();
        entityManager.clear();

        //assert
        ExamUser examUserDb = examUserRepository.findByExam_IdAndUser_Username(exam1.getId(), user1.getUsername());
        assertEquals(user1, examUserDb.getUser());
        assertEquals(exam1, examUserDb.getExam());
        assertEquals(10.0, examUserDb.getTotalPoint());
        assertTrue(examUser1.getIsStarted());
    }

    @Test
    @DisplayName("UT_EM_113: Tìm exam user theo id" +
            "--> trả về exam user tìm được với id trùng khớp với id truyền vào")
    public void findExamUserById() {
        //arrange
        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);
        Exam exam1 = createExam("Exam 1",60000);
        ExamUser examUser1 = createExamUser(exam1, user1);

        //act
        ExamUser examUserDb = examUserServiceImpl.findExamUserById(examUser1.getId())
                .orElseThrow(() -> new EntityNotExistsException("Exam user không tồn tại"));

        //assert
        assertEquals(examUser1.getId(), examUserDb.getId());
        assertEquals(exam1, examUserDb.getExam());
        assertEquals(user1, examUserDb.getUser());

    }

    @Test
    @DisplayName("UT_EM_114: Lấy ra danh sách ExamUser mà user đã hoàn thành" +
            "--> trả về danh sách tất cả các exam user mà user đã hoàn thành")
    public void getCompletedExams() {
        //arrange
        Course course1 = createCourse("Course1", "Course1");
        Part part1 = createPart("Part1", course1);
        Part part2 = createPart("Part2", course1);

        Course course2 = createCourse("Course2", "Course2");
        Part part3 = createPart("Part3", course2);

        User user1 = createUser("user01", "mail01@gmail.com", ERole.ROLE_STUDENT);
        Exam exam1 = createExamWithPart("Exam 1",60000, part1);
        Exam exam2 = createExamWithPart("Exam 2",60000, part2);
        Exam exam3 = createExamWithPart("Exam 3",60000, part3);

        ExamUser examUser1 = createExamUser(exam1, user1);
        ExamUser examUser2 = createExamUser(exam2, user1);
        ExamUser examUser3 = createExamUser(exam3, user1);

        //act
        List<ExamUser> examUserListDb = examUserServiceImpl.getCompleteExams(course1.getId(), user1.getUsername());

        //assert
        assertEquals(2, examUserListDb.size());
    }

    @Test
    @DisplayName("UT_EM_115: Lấy ra danh sách ExamUser theo exam id" +
            "--> trả về danh sách tất cả các exam user có exam id bằng exam id truyền vào")
    public void findAllByExamId() {
        //arrange
        Exam exam1 = createExam("Exam 1",60000);
        User user1 = createUser("user1", "mail1@gmail.com",  ERole.ROLE_STUDENT);
        User user2 = createUser("user2", "mail2@gmail.com",  ERole.ROLE_STUDENT);
        User user3 = createUser("user3", "mail3@gmail.com",  ERole.ROLE_STUDENT);

        ExamUser examUser1 = createExamUser(exam1, user1);
        ExamUser examUser2 = createExamUser(exam1, user2);
        ExamUser examUser3 = createExamUser(exam1, user3);
        //act
        List<ExamUser> examUserListDb = examUserServiceImpl.findAllByExam_Id(exam1.getId());

        //assert
        assertEquals(3, examUserListDb.size());
        assertEquals(user1, examUserListDb.get(0).getUser());
        assertEquals(user2, examUserListDb.get(1).getUser());
        assertEquals(user3, examUserListDb.get(2).getUser());
    }


    @Test
    @DisplayName("UT_EM_116: Lấy ra danh sách ExamUser theo exam id và trạng thái đã kết thúc" +
            "--> trả về danh sách tất cả các exam user có exam id bằng exam id truyền vào" +
            " và trạng thái đã kết thúc")
    public void findExamUsersByIsFinishedIsTrueAndExam_Id() {
        //arrange
        Exam exam1 = createExam("Exam 1",60000);
        User user1 = createUser("user1", "mail1@gmail.com",  ERole.ROLE_STUDENT);
        User user2 = createUser("user2", "mail2@gmail.com",  ERole.ROLE_STUDENT);
        User user3 = createUser("user3", "mail3@gmail.com",  ERole.ROLE_STUDENT);

        ExamUser examUser1 = createExamUser(exam1, user1);
        ExamUser examUser2 = createExamUser(exam1, user2);
        ExamUser examUser3 = createExamUser(exam1, user3);
        //act
        List<ExamUser> examUserListDb = examUserServiceImpl.findExamUsersByIsFinishedIsTrueAndExam_Id(exam1.getId());

        //assert
        assertEquals(3, examUserListDb.size());
        assertEquals(user1, examUserListDb.get(0).getUser());
        assertTrue(examUserListDb.get(0).getIsFinished());
        assertEquals(user2, examUserListDb.get(1).getUser());
        assertTrue(examUserListDb.get(1).getIsFinished());
        assertEquals(user3, examUserListDb.get(2).getUser());
        assertTrue(examUserListDb.get(2).getIsFinished());
    }

    public User createUser(String username, String email, ERole eRole) {
        Role role =  new Role();
        role.setName(eRole);
        role =  roleRepository.save(role);

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public Exam createExam(String title, int duration) {
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDurationExam(duration);
        return examRepository.save(exam);
    }

    public ExamUser createExamUser(Exam exam, User user) {
        ExamUser examUser = new ExamUser();
        examUser.setExam(exam);
        examUser.setUser(user);
        examUser.setTotalPoint(0.0);
        examUser.setIsFinished(true);
        return examUserRepository.save(examUser);
    }

    public Part createPart(String partName, Course course) {
        Part part = new Part();
        part.setName(partName);
        part.setCourse(course);
        return partRepository.save(part);
    }

    public Course createCourse(String courseName, String courseCode) {
        Course course = new Course();
        course.setName(courseName);
        course.setCourseCode(courseCode);
        return courseRepository.save(course);
    }

    public Exam createExamWithPart(String title, int duration, Part part) {
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDurationExam(duration);
        exam.setPart(part);
        return   examRepository.save(exam);
    }
}
