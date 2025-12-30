package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentMemberResponse;
import com.ademarli.municipality_service.service.AdminDepartmentMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/departments/{deptId}/members")
public class AdminDepartmentMemberController {

    private final AdminDepartmentMemberService service;

    public AdminDepartmentMemberController(AdminDepartmentMemberService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> add(@PathVariable Long deptId, @Valid @RequestBody AddDepartmentMemberRequest req) {
        service.addMember(deptId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<DepartmentMemberResponse>> list(@PathVariable Long deptId) {
        return ResponseEntity.ok(service.listMembers(deptId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable Long deptId, @PathVariable Long userId) {
        service.removeMember(deptId, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/role/{role}")
    public ResponseEntity<Void> changeRole(@PathVariable Long deptId,
                                           @PathVariable Long userId,
                                           @PathVariable String role) {
        service.changeRole(deptId, userId, role);
        return ResponseEntity.ok().build();
    }
}
