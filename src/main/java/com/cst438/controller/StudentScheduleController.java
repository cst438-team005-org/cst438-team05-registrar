package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.GradebookServiceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
public class StudentScheduleController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final GradebookServiceProxy gradebook;

    public StudentScheduleController(
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            UserRepository userRepository,
            GradebookServiceProxy gradebook
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.gradebook = gradebook;
    }


    @PostMapping("/enrollments/sections/{sectionNo}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public EnrollmentDTO addCourse(
            @PathVariable int sectionNo,
            Principal principal ) throws Exception  {

        // create and save an EnrollmentEntity
        //  relate enrollment to the student's User entity and to the Section entity
        //  check that student is not already enrolled in the section
        //  check that the current date is not before addDate, not after addDeadline
		//  of the section's term.  Return an EnrollmentDTO with the id of the 
		//  Enrollment and other fields.

        Section section = sectionRepository.findById(sectionNo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "section not found " + sectionNo));

        User student = userRepository.findByEmail(principal.getName());

        Enrollment existing = enrollmentRepository
                .findEnrollmentBySectionNoAndStudentId(sectionNo, student.getId());
        if (existing != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "student already enrolled in section " + sectionNo);
        }

        Term term = section.getTerm();
        Date today = new Date();
        if (today.before(term.getAddDate()) || today.after(term.getAddDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "section not open for enrollment " + sectionNo);
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setSection(section);
        enrollment.setStudent(student);
        enrollmentRepository.save(enrollment);

        EnrollmentDTO dto = toDTO(enrollment);

        // notify Gradebook service (stateless, no shared database) so it
        // creates the matching enrollment record using the same primary key.
        gradebook.sendMessage("addEnrollment", dto);

        return dto;
    }

    // student drops a course
    @DeleteMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public void dropCourse(@PathVariable("enrollmentId") int enrollmentId, Principal principal) throws Exception {

        // check that enrollment belongs to the logged in student
		// and that today is not after the dropDeadLine for the term.

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "enrollment not found " + enrollmentId));

        if (!enrollment.getStudent().getEmail().equals(principal.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "enrollment does not belong to logged in student " + enrollmentId);
        }

        Term term = enrollment.getSection().getTerm();
        Date today = new Date();
        if (today.after(term.getDropDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "drop deadline has passed for term " + term.getTermId());
        }

        enrollmentRepository.deleteById(enrollmentId);

        // notify Gradebook service (stateless, no shared database) so it
        // removes its corresponding enrollment record.
        gradebook.sendMessage("deleteEnrollment", enrollmentId);
    }

    private EnrollmentDTO toDTO(Enrollment enrollment) {
        Section section = enrollment.getSection();
        User student = enrollment.getStudent();
        return new EnrollmentDTO(
                enrollment.getEnrollmentId(),
                enrollment.getGrade(),
                student.getId(),
                student.getName(),
                student.getEmail(),
                section.getCourse().getCourseId(),
                section.getCourse().getTitle(),
                section.getSectionId(),
                section.getSectionNo(),
                section.getBuilding(),
                section.getRoom(),
                section.getTimes(),
                section.getCourse().getCredits(),
                section.getTerm().getYear(),
                section.getTerm().getSemester()
        );
    }

}


