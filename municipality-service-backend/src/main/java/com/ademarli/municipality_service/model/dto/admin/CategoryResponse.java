package com.ademarli.municipality_service.model.dto.admin;


public class CategoryResponse {
    private Long id;
    private String name;
    private boolean active;

    private Long defaultDepartmentId;
    private String defaultDepartmentName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getDefaultDepartmentId() { return defaultDepartmentId; }
    public void setDefaultDepartmentId(Long defaultDepartmentId) { this.defaultDepartmentId = defaultDepartmentId; }

    public String getDefaultDepartmentName() { return defaultDepartmentName; }
    public void setDefaultDepartmentName(String defaultDepartmentName) { this.defaultDepartmentName = defaultDepartmentName; }
}
