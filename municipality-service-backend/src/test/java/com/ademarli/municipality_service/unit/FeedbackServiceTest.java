package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.Feedback;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.FeedbackRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock FeedbackRepository feedbackRepository;
    @Mock ComplaintRepository complaintRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FeedbackService service;

    private User user(Long id, boolean enabled, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setEnabled(enabled);
        if (roles == null) u.setRoles(null);
        else u.setRoles(new HashSet<>(Arrays.asList(roles)));
        return u;
    }

    private Complaint complaint(Long id, ComplaintStatus status) {
        Complaint c = new Complaint();
        c.setId(id);
        c.setStatus(status);
        return c;
    }

    private Feedback feedback(Long id, Complaint c, User citizen, Integer rating) {
        Feedback f = new Feedback();
        f.setId(id);
        f.setComplaint(c);
        f.setCitizen(citizen);
        f.setRating(rating);
        return f;
    }

    // --------------------------------
    // upsertRating - guards
    // --------------------------------

    @Test
    void upsertRating_userNotFound_shouldThrow() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.upsertRating(10L, 1L, 5));

        verify(userRepository).findById(10L);
        verifyNoInteractions(complaintRepository, feedbackRepository);
    }

    @Test
    void upsertRating_notCitizen_shouldThrowAccessDenied() {
        User agent = user(10L, true, Role.AGENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));

        assertThrows(AccessDeniedException.class, () ->
                service.upsertRating(10L, 1L, 5));

        verifyNoInteractions(complaintRepository, feedbackRepository);
    }

    @Test
    void upsertRating_ratingNull_shouldThrowBusiness() {
        User citizen = user(10L, true, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));

        assertThrows(BusinessException.class, () ->
                service.upsertRating(10L, 1L, null));

        verifyNoInteractions(complaintRepository, feedbackRepository);
    }

    @Test
    void upsertRating_ratingOutOfRange_shouldThrowBusiness() {
        User citizen = user(10L, true, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));

        assertThrows(BusinessException.class, () ->
                service.upsertRating(10L, 1L, 0));

        assertThrows(BusinessException.class, () ->
                service.upsertRating(10L, 1L, 6));

        verifyNoInteractions(complaintRepository, feedbackRepository);
    }

    @Test
    void upsertRating_complaintNotFound_shouldThrow() {
        User citizen = user(10L, true, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));
        when(complaintRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.upsertRating(10L, 1L, 5));

        verify(complaintRepository).findById(1L);
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    void upsertRating_complaintNotRateable_shouldThrow() {
        User citizen = user(10L, true, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));

        Complaint c = complaint(1L, ComplaintStatus.NEW);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(BusinessException.class, () ->
                service.upsertRating(10L, 1L, 5));

        verifyNoInteractions(feedbackRepository);
    }

    // --------------------------------
    // upsertRating - existing vs new
    // --------------------------------

    @Test
    void upsertRating_existingFeedback_shouldUpdateRating_save_andReturnStats() {
        User citizen = user(10L, true, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));

        Complaint c = complaint(1L, ComplaintStatus.RESOLVED);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        Feedback existing = feedback(100L, c, citizen, 2);
        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.of(existing));

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        // stats view mock
        FeedbackRepository.FeedbackStatsView view = mock(FeedbackRepository.FeedbackStatsView.class);
        when(view.getAvgRating()).thenReturn(4.5);
        when(view.getRatingCount()).thenReturn(12L);
        when(feedbackRepository.getStats(1L)).thenReturn(view);

        // getMyRating kısmı buildStatsResponse içinde çağrılacağı için:
        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.of(existing)); // myRating -> existing.getRating (ama rating update sonrası 5 bekleyeceğiz)

        FeedbackStatsResponse out = service.upsertRating(10L, 1L, 5);

        verify(feedbackRepository).save(argThat(f ->
                f.getId().equals(100L) &&
                        f.getComplaint() == c &&
                        f.getCitizen() == citizen &&
                        f.getRating() == 5
        ));

        assertEquals(1L, out.getComplaintId());
        assertEquals(4.5, out.getAvgRating());
        assertEquals(12L, out.getRatingCount());
        // myRating getMyRating’den gelir (repo tekrar okur) - biz aynı objeyi döndürdüğümüz için 5 olmalı
        assertEquals(5, out.getMyRating());
    }

    @Test
    void upsertRating_noExistingFeedback_shouldCreate_setLinks_save_andReturnStats() {
        User citizen = user(10L, true, Role.CITIZEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(citizen));

        Complaint c = complaint(1L, ComplaintStatus.CLOSED);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));

        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.empty());

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackRepository.FeedbackStatsView view = mock(FeedbackRepository.FeedbackStatsView.class);
        when(view.getAvgRating()).thenReturn(3.0);
        when(view.getRatingCount()).thenReturn(1L);
        when(feedbackRepository.getStats(1L)).thenReturn(view);

        // myRating için: save sonrası tekrar find çağrısı olacak => rating 4 döndürelim
        Feedback saved = new Feedback();
        saved.setComplaint(c);
        saved.setCitizen(citizen);
        saved.setRating(4);

        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.of(saved));

        FeedbackStatsResponse out = service.upsertRating(10L, 1L, 4);

        verify(feedbackRepository).save(argThat(f ->
                f.getComplaint() == c &&
                        f.getCitizen() == citizen &&
                        f.getRating() == 4
        ));

        assertEquals(1L, out.getComplaintId());
        assertEquals(3.0, out.getAvgRating());
        assertEquals(1L, out.getRatingCount());
        assertEquals(4, out.getMyRating());
    }

    // --------------------------------
    // getMyRating
    // --------------------------------

    @Test
    void getMyRating_exists_shouldReturnRating() {
        Feedback fb = new Feedback();
        fb.setRating(5);

        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.of(fb));

        Integer out = service.getMyRating(10L, 1L);

        assertEquals(5, out);
    }

    @Test
    void getMyRating_notExists_shouldReturnNull() {
        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.empty());

        Integer out = service.getMyRating(10L, 1L);

        assertNull(out);
    }

    // --------------------------------
    // getStats + buildStatsResponse defaults
    // --------------------------------

    @Test
    void getStats_statsNull_shouldDefaultAvgAndCountToZero_andMyRatingNullWhenNoCitizen() {
        when(feedbackRepository.getStats(1L)).thenReturn(null);

        FeedbackStatsResponse out = service.getStats(1L, null);

        assertEquals(1L, out.getComplaintId());
        assertEquals(0.0, out.getAvgRating());
        assertEquals(0L, out.getRatingCount());
        assertNull(out.getMyRating());

        verify(feedbackRepository, never()).findByComplaint_IdAndCitizen_Id(anyLong(), anyLong());
    }

    @Test
    void getStats_statsWithNullFields_shouldDefaultToZero_andSetMyRatingWhenCitizenProvided() {
        FeedbackRepository.FeedbackStatsView view = mock(FeedbackRepository.FeedbackStatsView.class);
        when(view.getAvgRating()).thenReturn(null);
        when(view.getRatingCount()).thenReturn(null);
        when(feedbackRepository.getStats(1L)).thenReturn(view);

        Feedback fb = new Feedback();
        fb.setRating(2);
        when(feedbackRepository.findByComplaint_IdAndCitizen_Id(1L, 10L))
                .thenReturn(Optional.of(fb));

        FeedbackStatsResponse out = service.getStats(1L, 10L);

        assertEquals(1L, out.getComplaintId());
        assertEquals(0.0, out.getAvgRating());
        assertEquals(0L, out.getRatingCount());
        assertEquals(2, out.getMyRating());
    }
}
