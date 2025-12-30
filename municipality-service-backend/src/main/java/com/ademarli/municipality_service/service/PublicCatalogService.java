package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.model.dto.publiccatalog.PublicCategoryItem;
import com.ademarli.municipality_service.model.dto.publiccatalog.PublicDepartmentItem;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicCatalogService {

    private final DepartmentRepository departmentRepository;
    private final ComplaintCategoryRepository categoryRepository;

    public PublicCatalogService(DepartmentRepository departmentRepository,
                                ComplaintCategoryRepository categoryRepository) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<PublicDepartmentItem> activeDepartments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(d -> new PublicDepartmentItem(d.getId(), d.getName()))
                .toList();
    }

    public List<PublicCategoryItem> activeCategoriesByDepartment(Long deptId) {
        return categoryRepository.findByActiveTrueAndDefaultDepartmentIdOrderByNameAsc(deptId)
                .stream()
                .map(c -> new PublicCategoryItem(c.getId(), c.getName()))
                .toList();
    }
}
