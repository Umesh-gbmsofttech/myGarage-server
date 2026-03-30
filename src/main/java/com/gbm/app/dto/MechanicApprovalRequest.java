package com.gbm.app.dto;

import com.gbm.app.entity.ApprovalStatus;
import lombok.Data;

@Data
public class MechanicApprovalRequest {
    private ApprovalStatus status;
}
