package com.ademarli.municipality_service.repository;

import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {
    Optional<ComplaintCategory> findByNameIgnoreCase(String name);

    List<ComplaintCategory> findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(Long defaultDepartmentId);

}


