package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.admin.CategoryResponse;
import com.ademarli.municipality_service.model.dto.admin.CategoryUpsertRequest;
import com.ademarli.municipality_service.model.entity.ComplaintCategory;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.ComplaintCategoryRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCategoryService {

    private final ComplaintCategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;

    public AdminCategoryService(ComplaintCategoryRepository categoryRepository,
                                DepartmentRepository departmentRepository) {
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryUpsertRequest req) {
        String name = req.getName().trim();

        categoryRepository.findByNameIgnoreCase(name)
                .ifPresent(x -> { throw new BusinessException("CATEGORY_NAME_ALREADY_EXISTS"); });

        Department dept = departmentRepository.findById(req.getDefaultDepartmentId())
                .orElseThrow(() -> new NotFoundException("DEFAULT_DEPARTMENT_NOT_FOUND"));

        if (!dept.isActive()) throw new BusinessException("DEPARTMENT_NOT_ACTIVE");

        ComplaintCategory c = new ComplaintCategory();
        c.setName(name);
        c.setDefaultDepartment(dept);
        if (req.getActive() != null) c.setActive(req.getActive());

        categoryRepository.save(c);
        return toResp(c);
    }

    public Page<CategoryResponse> list(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toResp);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpsertRequest req) {
        ComplaintCategory c = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND"));

        String newName = req.getName().trim();
        if (!c.getName().equalsIgnoreCase(newName)) {
            categoryRepository.findByNameIgnoreCase(newName)
                    .ifPresent(exists -> { throw new BusinessException("CATEGORY_NAME_ALREADY_EXISTS"); });
            c.setName(newName);
        }

        Department dept = departmentRepository.findById(req.getDefaultDepartmentId())
                .orElseThrow(() -> new NotFoundException("DEFAULT_DEPARTMENT_NOT_FOUND"));

        if (!dept.isActive()) throw new BusinessException("DEPARTMENT_NOT_ACTIVE");

        c.setDefaultDepartment(dept);
        if (req.getActive() != null) c.setActive(req.getActive());

        categoryRepository.save(c);
        return toResp(c);
    }

    @Transactional
    public void delete(Long id) {
        ComplaintCategory c = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND"));

        c.setActive(false);
        categoryRepository.save(c);
    }

    private CategoryResponse toResp(ComplaintCategory c) {
        CategoryResponse r = new CategoryResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setActive(c.isActive());
        if (c.getDefaultDepartment() != null) {
            r.setDefaultDepartmentId(c.getDefaultDepartment().getId());
            r.setDefaultDepartmentName(c.getDefaultDepartment().getName());
        }
        return r;
    }
}
