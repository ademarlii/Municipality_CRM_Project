package com.ademarli.municipality_service.repository;

import com.ademarli.municipality_service.model.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByNameIgnoreCase(String name);

    Page<Department> findAllByActive(boolean active, Pageable pageable);

    List<Department> findByActiveTrueOrderByNameAsc();

    List<Department> findAllByActiveOrderByNameAsc(boolean active);
}


