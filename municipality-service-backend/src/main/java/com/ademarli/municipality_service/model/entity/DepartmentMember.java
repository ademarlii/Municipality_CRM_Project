package com.ademarli.municipality_service.model.entity;

import com.ademarli.municipality_service.model.enums.DepartmentMemberRole;
import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "department_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"department_id","user_id"}))
public class DepartmentMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentMemberRole memberRole = DepartmentMemberRole.MEMBER;

    @Column(nullable = false)
    private boolean active = true;

    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public DepartmentMemberRole getMemberRole() { return memberRole; }
    public void setMemberRole(DepartmentMemberRole memberRole) { this.memberRole = memberRole; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
