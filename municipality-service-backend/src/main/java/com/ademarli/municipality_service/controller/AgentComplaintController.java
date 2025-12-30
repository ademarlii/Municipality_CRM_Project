package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.agent.AgentComplaintListItem;
import com.ademarli.municipality_service.model.dto.complaint.ChangeStatusRequest;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.service.AgentComplaintService;
import com.ademarli.municipality_service.service.ComplaintService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent/complaints")
public class AgentComplaintController {

    private static final Logger log = LoggerFactory.getLogger(AgentComplaintController.class);

    private final AgentComplaintService agentComplaintService;
    private final ComplaintService complaintService;




    public AgentComplaintController(AgentComplaintService agentComplaintService, ComplaintService complaintService) {
        this.agentComplaintService = agentComplaintService;
        this.complaintService = complaintService;
    }

    @GetMapping
    public Page<AgentComplaintListItem> list(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ComplaintStatus status,
            Pageable pageable
    ) {
        String username = (ud != null ? ud.getUsername() : null);

        assert username != null;
        Long agentUserId = Long.parseLong(username);
        List<ComplaintStatus> statuses = (status == null) ? null : List.of(status);

        Page<AgentComplaintListItem> result = agentComplaintService.listForAgent(agentUserId, q, statuses, pageable);

        log.info("[AGENT_LIST] success userId={}, returnedElements={}, totalElements={}",
                agentUserId, result.getNumberOfElements(), result.getTotalElements());

        return result;
    }


    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PostMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@AuthenticationPrincipal UserDetails ud,
                                             @PathVariable Long id,
                                             @Valid @RequestBody ChangeStatusRequest req) {
        Long actorId = Long.parseLong(ud.getUsername());
        ComplaintStatus toStatus =ComplaintStatus.valueOf(req.getToStatus());
        complaintService.changeStatus(actorId, id, toStatus, req.getNote(), req.getPublicAnswer());
        return ResponseEntity.ok().build();
    }
}
