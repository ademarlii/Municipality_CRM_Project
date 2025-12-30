package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.agent.AgentComplaintListItem;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.DepartmentMemberRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentComplaintService {

    private static final Logger log = LoggerFactory.getLogger(AgentComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final UserRepository userRepository;

    public AgentComplaintService(ComplaintRepository complaintRepository,
                                 DepartmentMemberRepository departmentMemberRepository,
                                 UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.departmentMemberRepository = departmentMemberRepository;
        this.userRepository = userRepository;
    }

    public Page<AgentComplaintListItem> listForAgent(Long agentUserId,
                                                     String q,
                                                     List<ComplaintStatus> statuses,
                                                     Pageable pageable) {

        log.info("[AGENT_LIST] service start userId={}, q={}, statuses={}, page={}, size={}",
                agentUserId, q, statuses, pageable.getPageNumber(), pageable.getPageSize());

        User agent = userRepository.findById(agentUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        log.info("[AGENT_LIST] agent roles={}, enabled={}", agent.getRoles(), agent.isEnabled());

        if (agent.getRoles() == null || !agent.getRoles().contains(Role.AGENT)) {
            throw new BusinessException("ONLY_STAFF_CAN_CHANGE_STATUS");
        }

        List<Long> deptIds = departmentMemberRepository.findActiveDepartmentIdsByUserId(agentUserId);
        log.info("[AGENT_LIST] deptIds={}", deptIds);

        if (deptIds == null || deptIds.isEmpty()) {
            throw new BusinessException("NOT_A_MEMBER_OF_THIS_DEPARTMENT");
        }

        String query = (q == null) ? null : q.trim();
        if (query != null && query.isBlank()) query = null;

        Page<Complaint> page;
        try {
            page = complaintRepository.findAgentList(deptIds, query, statuses, pageable);
        } catch (Exception ex) {
            log.error("[AGENT_LIST] repo failed deptIds={}, q={}, statuses={}", deptIds, query, statuses, ex);
            throw ex;
        }

        log.info("[AGENT_LIST] repo ok elements={}, total={}", page.getNumberOfElements(), page.getTotalElements());

        return page.map(this::toItem);
    }

    private AgentComplaintListItem toItem(Complaint c) {
        AgentComplaintListItem x = new AgentComplaintListItem();
        x.setId(c.getId());
        x.setTrackingCode(c.getTrackingCode());
        x.setTitle(c.getTitle());
        x.setStatus(c.getStatus());
        x.setCreatedAt(c.getCreatedAt());
        x.setCategoryName(c.getCategory() != null ? c.getCategory().getName() : null);
        x.setDepartmentName(c.getDepartment() != null ? c.getDepartment().getName() : null);
        x.setCitizenEmail(c.getCreatedBy() != null ? c.getCreatedBy().getEmail() : null);
        return x;
    }
}
