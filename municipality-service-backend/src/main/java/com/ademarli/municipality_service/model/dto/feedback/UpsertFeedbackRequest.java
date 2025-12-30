package com.ademarli.municipality_service.model.dto.feedback;

import jakarta.validation.constraints.NotBlank;

public class UpsertFeedbackRequest {

    private Integer rating;

    public UpsertFeedbackRequest() {}

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
