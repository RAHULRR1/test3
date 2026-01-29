package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

// 3. Repository Interface
public interface UserRepository extends MongoRepository<User, String> {}