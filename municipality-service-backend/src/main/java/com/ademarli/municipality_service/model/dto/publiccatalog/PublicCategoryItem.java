package com.ademarli.municipality_service.model.dto.publiccatalog;

public class PublicCategoryItem {
    private Long id;
    private String name;

    public PublicCategoryItem() {}
    public PublicCategoryItem(Long id, String name) { this.id = id; this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
