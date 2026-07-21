package com.cst438.dto;


import jakarta.validation.constraints.*;

/*
 * Data Transfer Object for data for a section of a course
 */
public record SectionDTO(

        // only input fields that are not validated in Section controller
        // are validated here
        int secNo,
        int year,
        String semester,
        String courseId,
        String title,
        @NotNull(message="secId required")
        @Positive(message="secId must be positive integer")
        int secId,
        @NotNull(message="building required")
        @NotBlank(message="building cannot be blank")
        @Size(max=10, message="building max length 10")
        @Pattern(regexp ="^[a-zA-Z0-9-_: ]+$", message="invalid char in building")
        String building,
        @NotNull(message="room required")
        @NotBlank(message="room cannot be blank")
        @Size(max=10, message="room max length 10")
        @Pattern(regexp ="^[a-zA-Z0-9-_: ]+$", message="invalid char in room")
        String room,
        @NotNull(message="times required")
        @NotBlank(message="times cannot be blank")
        @Size(max=25, message="times max length 25")
        @Pattern(regexp ="^[a-zA-Z0-9_\\:\\- ]+$", message="invalid char in times")
        String times,
        String instructorName,
        String instructorEmail

       ) {
}
