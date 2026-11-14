package com.example.procureflow.approval.service;

import com.example.procureflow.approval.dto.ApprovalResponse;
import com.example.procureflow.approval.entity.Approval;
import com.example.procureflow.approval.repository.ApprovalRepository;
import com.example.procureflow.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;

    public ApprovalService(ApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalResponse> findAll(Pageable pageable) {
        Page<ApprovalResponse> page = approvalRepository.findAll(pageable)
                .map(ApprovalResponse::fromEntity);
        return PageResponse.of(page);
    }
}
