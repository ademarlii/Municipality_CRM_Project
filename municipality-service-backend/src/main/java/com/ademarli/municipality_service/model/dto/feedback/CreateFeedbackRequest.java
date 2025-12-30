package com.ademarli.municipality_service.model.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateFeedbackRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;

    public CreateFeedbackRequest() {
    }

    // getters/setters

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

