package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StudentControllerUnitTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void studentScheduleAndTranscript() {

        User student = userRepository.findByEmail("sam@csumb.edu");
        Section section = sectionRepository.findById(1).orElse(null);

        assertNotNull(student);
        assertNotNull(section);

        Enrollment enrollment =
                enrollmentRepository.findEnrollmentBySectionNoAndStudentId(
                        section.getSectionNo(),
                        student.getId()
                );

        boolean createdEnrollment = false;

        if (enrollment == null) {
            enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setSection(section);
            enrollment.setGrade("A");
            enrollment = enrollmentRepository.save(enrollment);
            createdEnrollment = true;
        }

        EntityExchangeResult<LoginDTO> loginResult =
                client.get().uri("/login")
                        .headers(headers ->
                                headers.setBasicAuth("sam@csumb.edu", "sam2025"))
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(LoginDTO.class)
                        .returnResult();

        LoginDTO loginDTO = loginResult.getResponseBody();
        assertNotNull(loginDTO);
        assertNotNull(loginDTO.jwt());

        client.get()
                .uri("/enrollments?year=2026&semester=Fall")
                .headers(headers -> headers.setBearerAuth(loginDTO.jwt()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .value(schedule -> {
                    assertEquals(1, schedule.size());
                    assertEquals("cst489", schedule.get(0).courseId());
                });

        client.get()
                .uri("/transcripts")
                .headers(headers -> headers.setBearerAuth(loginDTO.jwt()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .value(transcript -> {
                    assertEquals(1, transcript.size());
                    assertEquals(student.getId(), transcript.get(0).studentId());
                });

        if (createdEnrollment) {
            enrollmentRepository.delete(enrollment);
        }
    }
}
