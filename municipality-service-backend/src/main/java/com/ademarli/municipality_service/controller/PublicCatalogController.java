package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.publiccatalog.PublicCategoryItem;
import com.ademarli.municipality_service.model.dto.publiccatalog.PublicDepartmentItem;
import com.ademarli.municipality_service.service.PublicCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {

    private final PublicCatalogService service;

    public PublicCatalogController(PublicCatalogService service) {
        this.service = service;
    }

    @GetMapping("/departments")
    public ResponseEntity<List<PublicDepartmentItem>> departments() {
        return ResponseEntity.ok(service.activeDepartments());
    }

    @GetMapping("/departments/{deptId}/categories")
    public ResponseEntity<List<PublicCategoryItem>> categories(@PathVariable Long deptId) {
        return ResponseEntity.ok(service.activeCategoriesByDepartment(deptId));
    }
}
