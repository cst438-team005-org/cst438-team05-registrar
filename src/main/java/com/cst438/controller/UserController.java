package com.cst438.controller;

import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.UserDTO;
import com.cst438.dto.UserPasswordDTO;
import com.cst438.service.GradebookServiceProxy;
import jakarta.validation.Valid;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;


@RestController
public class UserController {

    private final UserRepository userRepository;

    private final GradebookServiceProxy gradebook;

    private final BCryptPasswordEncoder encoder;

    public UserController(
            UserRepository userRepository,
            GradebookServiceProxy gradebook,
            BCryptPasswordEncoder encoder
    ) {
        this.userRepository = userRepository;
        this.gradebook = gradebook;
        this.encoder = encoder;
    }

  // return users in userId order
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public List<UserDTO> findAllUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(u -> new UserDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getType()
        )).toList();
    }


    @PostMapping("/users")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public UserDTO createUser(@Valid @RequestBody UserDTO dto) throws Exception {

        // create and save a user Entity
        User u = userRepository.findByEmail(dto.email());
        if (u!=null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate email");
        }
        u = new User();
        u.setName(dto.name());
        u.setPassword(encoder.encode(dto.name()+"2025"));
        u.setEmail(dto.email());
//        if (!(dto.type().equals("STUDENT") || dto.type().equals("INSTRUCTOR") || dto.type().equals("ADMIN"))) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid type");
//        }
        u.setType(dto.type());
        userRepository.save(u);
        UserPasswordDTO p = new UserPasswordDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getType(),
                u.getPassword()
        );
        // send message to gradebook service
        gradebook.sendMessage("addUser", p);
        return new UserDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getType());
    }

    @PutMapping("/users")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public UserDTO updateUser(@Valid @RequestBody UserDTO dto) throws Exception {

        User u = userRepository.findById(dto.id()).orElse(null);
        if (u==null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user  id not found");
        }
        u.setName(dto.name());
//        if (!(dto.type().equals("STUDENT") || dto.type().equals("INSTRUCTOR") || dto.type().equals("ADMIN"))) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid type");
//        }
        u.setType(dto.type());
        userRepository.save(u);
        UserDTO result = new UserDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getType());
        //  send message to gradebook service
        gradebook.sendMessage("updateUser", result);
        return result;
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    public void  updateUser(@PathVariable("id") int id) {
        // send message to gradebook service
        userRepository.deleteById(id);
        gradebook.sendMessage("deleteUser", id);
    }
}
