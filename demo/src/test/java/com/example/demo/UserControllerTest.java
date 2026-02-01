package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        // Given
        String tenantId = "tenant-123";
        User inputUser = new User(null, "John Doe", "john@example.com", "ADMIN");
        User savedUser = new User("user-id-123", "John Doe", "john@example.com", "ADMIN");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When & Then
        mockMvc.perform(post("/api/{tenantId}/users", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "John Doe",
                                    "email": "john@example.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-id-123"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_WithDifferentTenantId_ShouldStillCreateUser() throws Exception {
        // Given
        String tenantId = "org-456";
        User savedUser = new User("user-id-456", "Jane Smith", "jane@example.com", "USER");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When & Then
        mockMvc.perform(post("/api/{tenantId}/users", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Jane Smith",
                                    "email": "jane@example.com",
                                    "role": "USER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-id-456"))
                .andExpect(jsonPath("$.name").value("Jane Smith"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUsers_ShouldReturnListOfUsers() throws Exception {
        // Given
        String tenantId = "tenant-789";
        List<User> users = Arrays.asList(
                new User("1", "Alice Johnson", "alice@example.com", "ADMIN"),
                new User("2", "Bob Williams", "bob@example.com", "USER"),
                new User("3", "Charlie Brown", "charlie@example.com", "MANAGER")
        );

        when(userRepository.findAll()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("Alice Johnson"))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].name").value("Bob Williams"))
                .andExpect(jsonPath("$[2].id").value("3"))
                .andExpect(jsonPath("$[2].name").value("Charlie Brown"));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUsers_WhenNoUsersExist_ShouldReturnEmptyList() throws Exception {
        // Given
        String tenantId = "tenant-empty";
        when(userRepository.findAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUsers_WithDifferentTenantId_ShouldCallRepository() throws Exception {
        // Given
        String tenantId = "org-xyz";
        List<User> users = Arrays.asList(
                new User("10", "Test User", "test@example.com", "USER")
        );

        when(userRepository.findAll()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("10"));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void generateOrgId_ShouldReturnValidUUID() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                )));
    }

    @Test
    void generateOrgId_ShouldReturnDifferentUUIDsOnMultipleCalls() throws Exception {
        // When
        String firstUuid = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondUuid = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then
        assert !firstUuid.equals(secondUuid) : "UUIDs should be different";

        // Verify both are valid UUIDs
        UUID.fromString(firstUuid);
        UUID.fromString(secondUuid);
    }

    @Test
    void generateOrgId_ShouldGenerateTimeOrderedUUID() throws Exception {
        // When
        String uuidString = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then
        UUID uuid = UUID.fromString(uuidString);
        // UuidCreator.getTimeOrdered() generates UUID v6 (time-ordered)
        // The version is in bits 12-15 of the time_hi_and_version field
        int version = uuid.version();
        assert version == 6 : "UUID should be version 6 (time-ordered), but was version " + version;
    }
}

