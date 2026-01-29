package com.example.demo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients; // <--- IMPORT THIS
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value; // <--- IMPORT THIS
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Configuration
public class MultiTenantConfig {

    // 1. ThreadLocal Context
    private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

    // --- NEW: Inject URI from properties ---
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    // --- NEW: Manually Create the MongoClient ---
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    // 2. Interceptor (Updated with exclusion)
    @Bean
    public MappedInterceptor tenantInterceptor() {
        return new MappedInterceptor(
                new String[]{"/api/**"},
                new String[]{"/api/generate-org-id"}, // Exclude generation endpoint
                new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                        String[] parts = req.getRequestURI().split("/");
                        if (parts.length > 2) {
                            TENANT_CONTEXT.set(parts[2]);
                        }
                        return true;
                    }

                    @Override
                    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object h, Exception ex) {
                        TENANT_CONTEXT.remove();
                    }
                }
        );
    }

    // 3. Factory Override
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, "default_db") {
            @Override
            public MongoDatabase getMongoDatabase() {
                String tenantId = TENANT_CONTEXT.get();
                return super.getMongoDatabase(tenantId != null ? "org_" + tenantId : "default_db");
            }
        };
    }
}