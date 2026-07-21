package com.cst438.dto;

import jakarta.validation.constraints.*;

/*
 * Data Transfer Object for course data
 */
public record CourseDTO(

        @NotNull(message="courseId required")
        @NotBlank(message="courseId cannot be blank")
        @Size(max=10, message="courseId max length 10")
        @Pattern(regexp ="^[a-zA-Z0-9]+$", message="invalid char in courseId")
        String courseId,
        @NotNull(message="title required")
        @NotBlank(message="title cannot be blank")
        @Size(max=100, message="title max length 100")
        @Pattern(regexp ="^[a-zA-Z0-9-_ ]+$", message="invalid char in title")
        String title,
        @NotNull(message="credits required")
        @Max(value=9, message="credits must be in range 1-9")
        @Min(value=1, message="credits must be in range 1-9")
        int credits
) {
}
