package com.example.procureflow.user.repository;

import com.example.procureflow.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
}
