package com.example.procureflow.approval.dto;

import com.example.procureflow.approval.entity.Approval;

import java.time.Instant;

public record ApprovalResponse(
        Long id,
        Long requisitionId,
        String stage,
        String decision,
        Long actorId,
        String actorName,
        String comment,
        Instant createdAt
) {
    public static ApprovalResponse fromEntity(Approval a) {
        return new ApprovalResponse(
                a.getId(),
                a.getRequisition() != null ? a.getRequisition().getId() : null,
                a.getStage(),
                a.getDecision() != null ? a.getDecision().name() : null,
                a.getActor() != null ? a.getActor().getId() : null,
                a.getActor() != null ? a.getActor().getFullName() : null,
                a.getComment(),
                a.getCreatedAt()
        );
    }
}
