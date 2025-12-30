package com.ademarli.municipality_service.model.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class CategoryUpsertRequest {
    @NotBlank
    private String name;

    @NotNull
    private Long defaultDepartmentId;

    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getDefaultDepartmentId() { return defaultDepartmentId; }
    public void setDefaultDepartmentId(Long defaultDepartmentId) { this.defaultDepartmentId = defaultDepartmentId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
