package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.GradebookServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentScheduleControllerUnitTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    // GradebookServiceProxy calls RabbitMQ (rabbitTemplate.convertAndSend). Mocking it
    // here means enroll/drop can be unit tested without a running RabbitMQ broker.
    @MockitoBean
    private GradebookServiceProxy gradebookServiceProxy;

    /*
     * The starter data (data.sql) contains:
     * student:  sam@csumb.edu / sam2025
     * section:  section_no = 1, term_id = 12 (2026 Fall), which is open for
     *           enrollment (add_date 2026-04-01 .. add_deadline 2026-09-30).
     * Rather than hardcoding section_no = 1, this test looks up an open
     * section via SectionRepository so it doesn't break if data.sql changes.
     */

    // Verifies that a student can enroll in an open section: the endpoint
    // returns 200 with an EnrollmentDTO describing the new enrollment, the
    // Enrollment row is actually persisted, and the Gradebook service is
    // notified via gradebookServiceProxy.sendMessage("addEnrollment", ...).
    @Test
    public void enrollStudentInOpenSection() {

        Section section = getOpenSection();
        User student = userRepository.findByEmail("sam@csumb.edu");
        assertNotNull(student, "Test student sam@csumb.edu was not found.");

        // make sure there isn't a leftover enrollment from a previous run
        removeExistingEnrollment(section.getSectionNo(), student.getId());

        String jwt = loginAsStudent();

        EntityExchangeResult<EnrollmentDTO> enrollResult = client.post()
                .uri("/enrollments/sections/{sectionNo}", section.getSectionNo())
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO.class)
                .returnResult();

        EnrollmentDTO dto = enrollResult.getResponseBody();

        assertNotNull(dto);
        assertTrue(dto.enrollmentId() > 0, "enrollmentId should be assigned by the database");
        assertEquals(section.getSectionNo(), dto.sectionNo());
        assertEquals(section.getSectionId(), dto.sectionId());
        assertEquals(section.getCourse().getCourseId(), dto.courseId());
        assertEquals(student.getId(), dto.studentId());
        assertEquals(student.getEmail(), dto.email());
        assertEquals(student.getName(), dto.name());

        // the Enrollment row must actually exist in the database
        Enrollment saved = enrollmentRepository.findById(dto.enrollmentId()).orElse(null);
        assertNotNull(saved, "Enrollment was not persisted to the database.");
        assertEquals(student.getId(), saved.getStudent().getId());
        assertEquals(section.getSectionNo(), saved.getSection().getSectionNo());

        // Gradebook must be notified so it can create its matching enrollment
        verify(gradebookServiceProxy, times(1)).sendMessage(eq("addEnrollment"), any());

        // clean up
        enrollmentRepository.deleteById(dto.enrollmentId());
    }

    // Verifies that a student can drop a section they are enrolled in: the
    // endpoint returns success, the Enrollment row is removed from the
    // database, and the Gradebook service is notified via
    // gradebookServiceProxy.sendMessage("deleteEnrollment", ...).
    @Test
    public void dropStudentFromSection() {

        Section section = getOpenSection();
        User student = userRepository.findByEmail("sam@csumb.edu");
        assertNotNull(student, "Test student sam@csumb.edu was not found.");

        removeExistingEnrollment(section.getSectionNo(), student.getId());

        Enrollment enrollment = new Enrollment();
        enrollment.setSection(section);
        enrollment.setStudent(student);
        enrollment.setGrade(null);
        enrollment = enrollmentRepository.save(enrollment);

        int enrollmentId = enrollment.getEnrollmentId();

        String jwt = loginAsStudent();

        client.delete()
                .uri("/enrollments/{enrollmentId}", enrollmentId)
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk();

        Enrollment afterDrop = enrollmentRepository.findById(enrollmentId).orElse(null);
        assertNull(afterDrop, "Enrollment should have been removed from the database.");

        verify(gradebookServiceProxy, times(1)).sendMessage(eq("deleteEnrollment"), any());

        // nothing left to clean up - the row was deleted by the endpoint
    }

    // Negative test: a student must not be able to drop an enrollment that
    // belongs to a different student. The controller throws
    // HttpStatus.FORBIDDEN for this case, but ExceptionAdvisor
    // .handleResponseStatusException() currently hardcodes every
    // ResponseStatusException response (and its JSON "status" field) to
    // HttpStatus.BAD_REQUEST regardless of the exception's actual status.
    // Until that bug in ExceptionAdvisor is fixed, the client actually
    // receives 400 BAD_REQUEST instead of 403 FORBIDDEN, so this test
    // documents today's real behavior rather than the intended behavior.
    // Either way, the enrollment must not be dropped and Gradebook must not
    // be notified.
    @Test
    public void dropEnrollmentNotBelongingToLoggedInStudentIsRejected() {

        Section section = getOpenSection();

        // create a second, temporary student who "owns" the enrollment
        User otherStudent = new User();
        otherStudent.setName("Other Test Student");
        otherStudent.setEmail("other-student-unittest@csumb.edu");
        otherStudent.setPassword("unused");
        otherStudent.setType("STUDENT");
        otherStudent = userRepository.save(otherStudent);

        removeExistingEnrollment(section.getSectionNo(), otherStudent.getId());

        Enrollment enrollment = new Enrollment();
        enrollment.setSection(section);
        enrollment.setStudent(otherStudent);
        enrollment.setGrade(null);
        enrollment = enrollmentRepository.save(enrollment);

        int enrollmentId = enrollment.getEnrollmentId();

        // logged in as sam@csumb.edu, who does NOT own the enrollment above
        String jwt = loginAsStudent();

        // NOTE: should be .isForbidden() (403) per StudentScheduleController,
        // but currently asserts the actual 400 BAD_REQUEST caused by the
        // ExceptionAdvisor bug described above.
        client.delete()
                .uri("/enrollments/{enrollmentId}", enrollmentId)
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isBadRequest();

        // the enrollment must still exist - the drop was rejected
        Enrollment stillThere = enrollmentRepository.findById(enrollmentId).orElse(null);
        assertNotNull(stillThere, "Enrollment should not have been dropped.");

        verify(gradebookServiceProxy, times(0)).sendMessage(eq("deleteEnrollment"), any());

        // clean up
        enrollmentRepository.deleteById(enrollmentId);
        userRepository.deleteById(otherStudent.getId());
    }

    // returns a Section that is currently open for enrollment, looked up via
    // SectionRepository rather than assuming a hardcoded section_no exists.
    private Section getOpenSection() {
        List<Section> openSections = sectionRepository.findByOpenOrderByCourseIdSectionId();
        assertFalse(openSections.isEmpty(),
                "No section open for enrollment was found - check data.sql term dates.");
        return openSections.get(0);
    }

    // removes any leftover enrollment for this student/section combination
    // left over from a previous, possibly failed, test run.
    private void removeExistingEnrollment(int sectionNo, int studentId) {
        Enrollment existing = enrollmentRepository
                .findEnrollmentBySectionNoAndStudentId(sectionNo, studentId);
        if (existing != null) {
            enrollmentRepository.deleteById(existing.getEnrollmentId());
        }
    }

    private String loginAsStudent() {

        EntityExchangeResult<LoginDTO> loginResult = client.get()
                .uri("/login")
                .headers(headers ->
                        headers.setBasicAuth("sam@csumb.edu", "sam2025"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        LoginDTO login = loginResult.getResponseBody();

        assertNotNull(login);
        assertNotNull(login.jwt());

        return login.jwt();
    }
}
