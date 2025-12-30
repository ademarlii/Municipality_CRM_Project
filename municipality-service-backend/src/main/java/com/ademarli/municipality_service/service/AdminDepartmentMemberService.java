package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.admin.AddDepartmentMemberRequest;
import com.ademarli.municipality_service.model.dto.admin.DepartmentMemberResponse;
import com.ademarli.municipality_service.model.entity.Department;
import com.ademarli.municipality_service.model.entity.DepartmentMember;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.DepartmentMemberRole;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.DepartmentRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminDepartmentMemberService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DepartmentMemberRepository memberRepository;

    public AdminDepartmentMemberService(DepartmentRepository departmentRepository,
                                        UserRepository userRepository,
                                        DepartmentMemberRepository memberRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void addMember(Long deptId, AddDepartmentMemberRequest req) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new NotFoundException("DEPARTMENT_NOT_FOUND"));
        if (!dept.isActive()) throw new BusinessException("DEPARTMENT_NOT_ACTIVE");

        User u = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        if (u.getRoles() == null || !u.getRoles().contains(Role.AGENT)) {
            throw new BusinessException("ONLY_AGENT_CAN_BE_DEPARTMENT_MEMBER");
        }

        DepartmentMemberRole role = parseMemberRoleOrDefault(req.getMemberRole());

        var existingOpt = memberRepository.findByDepartmentIdAndUserId(deptId, u.getId());
        if (existingOpt.isPresent()) {
            DepartmentMember existing = existingOpt.get();
            existing.setActive(true);
            existing.setMemberRole(role);
            memberRepository.save(existing);
            return;
        }

        DepartmentMember m = new DepartmentMember();
        m.setDepartment(dept);
        m.setUser(u);
        m.setMemberRole(role);
        m.setActive(true);
        memberRepository.save(m);
    }

    public List<DepartmentMemberResponse> listMembers(Long deptId) {
        return memberRepository.findAllByDepartmentId(deptId).stream()
                .map(this::toResp)
                .toList();
    }

    @Transactional
    public void removeMember(Long deptId, Long userId) {
        DepartmentMember m = memberRepository.findByDepartmentIdAndUserId(deptId, userId)
                .orElseThrow(() -> new NotFoundException("DEPARTMENT_MEMBER_NOT_FOUND"));
        m.setActive(false);
        memberRepository.save(m);
    }

    @Transactional
    public void changeRole(Long deptId, Long userId, String memberRole) {
        DepartmentMember m = memberRepository.findByDepartmentIdAndUserId(deptId, userId)
                .orElseThrow(() -> new NotFoundException("DEPARTMENT_MEMBER_NOT_FOUND"));
        m.setMemberRole(parseMemberRoleOrDefault(memberRole));
        memberRepository.save(m);
    }

    private DepartmentMemberRole parseMemberRoleOrDefault(String memberRole) {
        if (memberRole == null || memberRole.isBlank()) return DepartmentMemberRole.MEMBER;
        try {
            return DepartmentMemberRole.valueOf(memberRole.trim());
        } catch (Exception e) {
            throw new BusinessException("INVALID_MEMBER_ROLE");
        }
    }

    private DepartmentMemberResponse toResp(DepartmentMember m) {
        DepartmentMemberResponse r = new DepartmentMemberResponse();
        r.setUserId(m.getUser().getId());
        r.setEmail(m.getUser().getEmail());
        r.setPhone(m.getUser().getPhone());
        r.setMemberRole(m.getMemberRole().name());
        r.setActive(m.isActive());
        return r;
    }
}
