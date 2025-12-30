package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.admin.DepartmentResponse;
import com.ademarli.municipality_service.model.dto.admin.DepartmentUpsertRequest;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDepartmentService {

    private final DepartmentRepository departmentRepository;

    public AdminDepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentUpsertRequest req) {
        String name = req.getName().trim();

        departmentRepository.findByNameIgnoreCase(name)
                .ifPresent(d -> { throw new BusinessException("DEPARTMENT_NAME_ALREADY_EXISTS"); });

        Department d = new Department();
        d.setName(name);
        if (req.getActive() != null) d.setActive(req.getActive());

        departmentRepository.save(d);
        return toResp(d);
    }

    public Page<DepartmentResponse> list(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(this::toResp);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpsertRequest req) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("DEPARTMENT_NOT_FOUND"));

        String newName = req.getName().trim();
        if (!d.getName().equalsIgnoreCase(newName)) {
            departmentRepository.findByNameIgnoreCase(newName)
                    .ifPresent(exists -> { throw new BusinessException("DEPARTMENT_NAME_ALREADY_EXISTS"); });
            d.setName(newName);
        }

        if (req.getActive() != null) d.setActive(req.getActive());

        departmentRepository.save(d);
        return toResp(d);
    }

    @Transactional
    public void delete(Long id) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("DEPARTMENT_NOT_FOUND"));

        d.setActive(false);
        departmentRepository.save(d);
    }

    private DepartmentResponse toResp(Department d) {
        DepartmentResponse r = new DepartmentResponse();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setActive(d.isActive());
        return r;
    }
}
