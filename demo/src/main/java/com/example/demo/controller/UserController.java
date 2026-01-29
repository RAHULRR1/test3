package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;






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
        // In future: Create Org entry in "Master Database" here
        // Logic to generate UUID v7 (Time-ordered)
        java.util.UUID uuid = generateUUIDv7();
        return uuid.toString();
    }


    private java.util.UUID generateUUIDv7() {
        long timestamp = System.currentTimeMillis();

        // Random bytes
        byte[] randomBytes = new byte[10];
        new java.security.SecureRandom().nextBytes(randomBytes);

        long msb = (timestamp << 16) | ((randomBytes[0] & 0xFFL) << 8) | (randomBytes[1] & 0xFFL);
        long lsb = 0;
        for (int i = 2; i < 10; i++) {
            lsb = (lsb << 8) | (randomBytes[i] & 0xFFL);
        }

        // Set Version to 7 (0111)
        msb = (msb & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L;
        // Set Variant to IETF (10)
        lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new java.util.UUID(msb, lsb);
    }


}
