package com.gbm.app.dto;

import com.gbm.app.entity.ReviewType;

import lombok.Data;

@Data
public class ReviewRequest {
    private ReviewType type;
    private Long mechanicId;
    private Integer rating;
    private String comment;
}
