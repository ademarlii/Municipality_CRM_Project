package com.ademarli.municipality_service.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "complaint_category")
public class ComplaintCategory {

    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "default_department_id")
    private Department defaultDepartment;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Department getDefaultDepartment() { return defaultDepartment; }
    public void setDefaultDepartment(Department defaultDepartment) { this.defaultDepartment = defaultDepartment; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}


