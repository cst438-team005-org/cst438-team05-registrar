package com.cst438.dto;

import jakarta.validation.constraints.*;

/*
 * Data Transfer Object for user data
 * A user can be a STUDENT, ADMIN, or INSTRUCTOR type
 */
public record UserDTO (

        int id,
        @NotNull(message="name required")
        @NotBlank(message="name cannot be blank")
        @Size(max=50, message="name max length 50")
        @Pattern(regexp ="^[a-zA-Z ]+$", message="invalid char in name")
        String name,
        @Email(message="invalid email")
        String email,
        @Pattern(regexp="^ADMIN|STUDENT|INSTRUCTOR$", message="invalid user type")
        String type) {
}
