package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        mongoDBContainer.start();
    }

    @AfterEach
    void cleanup() {
        // Clean up all tenant databases
        for (String dbName : mongoClient.listDatabaseNames()) {
            if (dbName.startsWith("org_")) {
                mongoClient.getDatabase(dbName).drop();
            }
        }
    }

    @Test
    void createUser_ShouldPersistToDatabase() throws Exception {
        // Given
        String tenantId = "org-123";

        // When
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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // Then - Verify in tenant-specific database
        MongoDatabase tenantDb = mongoClient.getDatabase("org_" + tenantId);
        long count = tenantDb.getCollection("users").countDocuments();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void createUser_WithDifferentTenant_ShouldPersistSuccessfully() throws Exception {
        // Given
        String tenantId = "org-456";

        // When
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
                .andExpect(jsonPath("$.name").value("Jane Smith"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Then - Verify persistence in correct tenant database
        MongoDatabase tenantDb = mongoClient.getDatabase("org_" + tenantId);
        long count = tenantDb.getCollection("users").countDocuments();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void getUsers_ShouldReturnAllPersistedUsers() throws Exception {
        // Given
        String tenantId = "org-789";
        MongoDatabase tenantDb = mongoClient.getDatabase("org_" + tenantId);

        // Insert test data directly into tenant database
        tenantDb.getCollection("users").insertOne(
            new org.bson.Document()
                .append("name", "Alice Johnson")
                .append("email", "alice@example.com")
                .append("role", "ADMIN")
        );
        tenantDb.getCollection("users").insertOne(
            new org.bson.Document()
                .append("name", "Bob Williams")
                .append("email", "bob@example.com")
                .append("role", "USER")
        );
        tenantDb.getCollection("users").insertOne(
            new org.bson.Document()
                .append("name", "Charlie Brown")
                .append("email", "charlie@example.com")
                .append("role", "MANAGER")
        );

        // When & Then
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Alice Johnson", "Bob Williams", "Charlie Brown")))
                .andExpect(jsonPath("$[*].email", containsInAnyOrder("alice@example.com", "bob@example.com", "charlie@example.com")))
                .andExpect(jsonPath("$[*].role", containsInAnyOrder("ADMIN", "USER", "MANAGER")));
    }

    @Test
    void getUsers_WhenNoUsersExist_ShouldReturnEmptyList() throws Exception {
        // Given
        String tenantId = "org-empty";

        // When & Then
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().json("[]"));

        // Verify database is empty
        MongoDatabase tenantDb = mongoClient.getDatabase("org_" + tenantId);
        long count = tenantDb.getCollection("users").countDocuments();
        assertThat(count).isEqualTo(0);
    }

    @Test
    void createAndRetrieveUser_EndToEndWorkflow() throws Exception {
        // Given
        String tenantId = "org-workflow";

        // Step 1: Create first user
        mockMvc.perform(post("/api/{tenantId}/users", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test User 1",
                                    "email": "test1@example.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk());

        // Step 2: Create second user
        mockMvc.perform(post("/api/{tenantId}/users", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test User 2",
                                    "email": "test2@example.com",
                                    "role": "USER"
                                }
                                """))
                .andExpect(status().isOk());

        // Step 3: Retrieve all users
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", oneOf("Test User 1", "Test User 2")))
                .andExpect(jsonPath("$[1].name", oneOf("Test User 1", "Test User 2")));

        // Step 4: Verify database state in tenant-specific database
        MongoDatabase tenantDb = mongoClient.getDatabase("org_" + tenantId);
        long count = tenantDb.getCollection("users").countDocuments();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void multiTenant_IsolationTest_DataShouldBeIsolatedByTenant() throws Exception {
        // Given - Create users in two different tenants
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        // When - Create user in tenant 1
        mockMvc.perform(post("/api/{tenantId}/users", tenant1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tenant 1 User",
                                    "email": "user1@tenant1.com",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk());

        // Create user in tenant 2
        mockMvc.perform(post("/api/{tenantId}/users", tenant2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Tenant 2 User",
                                    "email": "user2@tenant2.com",
                                    "role": "USER"
                                }
                                """))
                .andExpect(status().isOk());

        // Then - Verify tenant 1 only sees their user
        mockMvc.perform(get("/api/{tenantId}/users", tenant1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Tenant 1 User"));

        // Verify tenant 2 only sees their user
        mockMvc.perform(get("/api/{tenantId}/users", tenant2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Tenant 2 User"));

        // Verify data isolation at database level
        MongoDatabase tenant1Db = mongoClient.getDatabase("org_" + tenant1);
        MongoDatabase tenant2Db = mongoClient.getDatabase("org_" + tenant2);
        assertThat(tenant1Db.getCollection("users").countDocuments()).isEqualTo(1);
        assertThat(tenant2Db.getCollection("users").countDocuments()).isEqualTo(1);
    }

    @Test
    void generateOrgId_ShouldReturnValidUUID() throws Exception {
        // When
        String uuidString = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                )))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then
        UUID uuid = UUID.fromString(uuidString);
        assertThat(uuid).isNotNull();
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
        assertThat(uuid.version()).isEqualTo(6); // UuidCreator.getTimeOrdered() generates UUID v6
    }

    @Test
    void generateOrgId_ShouldGenerateUniqueUUIDs() throws Exception {
        // When - Generate multiple UUIDs
        String uuid1 = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String uuid2 = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String uuid3 = mockMvc.perform(get("/api/generate-org-id"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - Verify all are unique
        assertThat(uuid1).isNotEqualTo(uuid2);
        assertThat(uuid2).isNotEqualTo(uuid3);
        assertThat(uuid1).isNotEqualTo(uuid3);
    }

    @Test
    void createMultipleUsers_WithSameTenantId_ShouldAllPersist() throws Exception {
        // Given
        String tenantId = "org-multi";

        // When - Create 5 users
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/{tenantId}/users", tenantId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "name": "User %d",
                                        "email": "user%d@example.com",
                                        "role": "USER"
                                    }
                                    """, i, i)))
                    .andExpect(status().isOk());
        }

        // Then - Verify all users are persisted
        mockMvc.perform(get("/api/{tenantId}/users", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));

        // Verify in database
        MongoDatabase tenantDb = mongoClient.getDatabase("org_" + tenantId);
        long count = tenantDb.getCollection("users").countDocuments();
        assertThat(count).isEqualTo(5);
    }
}

