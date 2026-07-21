package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.GradebookServiceProxy;
import jakarta.validation.Valid;
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

import java.util.ArrayList;
import java.util.List;

@RestController
public class CourseController {

    private final CourseRepository courseRepository;


    private final GradebookServiceProxy gradebook;

    public CourseController(
            CourseRepository courseRepository,
            GradebookServiceProxy gradebook
    ) {
        this.courseRepository = courseRepository;
        this.gradebook = gradebook;
    }


    // ADMIN function to create a new course
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    @PostMapping("/courses")
    public CourseDTO addCourse(@Valid @RequestBody CourseDTO dto) throws Exception {

        Course c = courseRepository.findById(dto.courseId()).orElse(null);
        if (c!=null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate course id");
        }

        c = new Course();
        c.setCourseId(dto.courseId());
        c.setTitle(dto.title());
        c.setCredits(dto.credits());
        courseRepository.save(c);
        CourseDTO result =  new CourseDTO(
                c.getCourseId(),
                c.getTitle(),
                c.getCredits()
        );
        gradebook.sendMessage("addCourse", result);
        return result;
    }

    // ADMIN function to update a course
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    @PutMapping("/courses")
    public CourseDTO updateCourse(@Valid @RequestBody CourseDTO dto) throws Exception {

        Course c = courseRepository.findById(dto.courseId()).orElse(null);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course id not found");
        }
        c.setTitle(dto.title());
        c.setCredits(dto.credits());
        courseRepository.save(c);
        CourseDTO result =  new CourseDTO(
                c.getCourseId(),
                c.getTitle(),
                c.getCredits()
        );
        gradebook.sendMessage("updateCourse", result);
        return result;
    }

    // ADMIN function to delete a course
    // delete will fail if the course has sections
    @DeleteMapping("/courses/{courseid}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public void deleteCourse(@PathVariable("courseid") String courseId) {

        courseRepository.deleteById(courseId);
        gradebook.sendMessage("deleteCourse", courseId);
    }

    @GetMapping("/courses")
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAllOrderByCourseId().stream().map((c) ->
            new CourseDTO(
                    c.getCourseId(),
                    c.getTitle(),
                    c.getCredits()
            )
        ).toList();
    }

}
