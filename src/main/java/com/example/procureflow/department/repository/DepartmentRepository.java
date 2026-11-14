package com.example.procureflow.department.repository;

import com.example.procureflow.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {
    Optional<Department> findByName(String name);
    Optional<Department> findByCode(String code);
    boolean existsByName(String name);
    boolean existsByCode(String code);
}
