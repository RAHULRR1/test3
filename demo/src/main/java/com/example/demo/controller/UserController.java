package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.uuid.Generators;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;




@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/api/{tenantId}/users")
    public User createUser(@PathVariable String tenantId, @RequestBody User user) {
        // Validation: Ensure the URL tenant matches any payload requirements if needed
        return userRepository.save(user);
    }

    @GetMapping("/api/{tenantId}/users")
    public List<User> getUsers(@PathVariable String tenantId) {
        // Spring automatically routes this to the "org_{tenantId}" database
        return userRepository.findAll();
    }


    @GetMapping("/api/generate-org-id")
    public String generateOrgId() {
        UUID uuid = Generators.timeBasedEpochGenerator().generate();
        return uuid.toString();
    }


}
