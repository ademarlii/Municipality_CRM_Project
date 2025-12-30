package com.ademarli.municipality_service.repository;

import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {


    List<Complaint> findByCreatedBy(User user);

    Optional<Complaint> findByTrackingCode(String trackingCode);

    Page<Complaint> findByStatusIn(List<ComplaintStatus> statuses, Pageable pageable);

    boolean existsByTrackingCode(String trackingCode);


    @Query("""
  select c from Complaint c
  where c.department.id in :deptIds
    and (
      :q is null or :q = '' or
      lower(c.trackingCode) like lower(concat('%', :q, '%')) or
      lower(c.title)       like lower(concat('%', :q, '%'))
    )
    and (:statuses is null or c.status in :statuses)
""")
    Page<Complaint> findAgentList(
            @Param("deptIds") List<Long> deptIds,
            @Param("q") String q,
            @Param("statuses") List<ComplaintStatus> statuses,
            Pageable pageable
    );








    interface PublicFeedRow {
        Long getId();
        String getTrackingCode();
        String getTitle();
        String getCategoryName();
        String getDepartmentName();
        ComplaintStatus getStatus();
        java.time.OffsetDateTime getAnsweredAt();
        String getPublicAnswer();
        Double getAvgRating();
        Long getRatingCount();
    }

    @Query("""
        select
          c.id as id,
          c.trackingCode as trackingCode,
          c.title as title,
          cat.name as categoryName,
          d.name as departmentName,
          c.status as status,
          c.resolvedAt as answeredAt,
          c.publicAnswer as publicAnswer,
          coalesce(avg(f.rating), 0) as avgRating,
          coalesce(count(f.id), 0) as ratingCount
        from Complaint c
          join c.category cat
          join c.department d
          left join Feedback f on f.complaint = c
        where c.status = com.ademarli.municipality_service.model.enums.ComplaintStatus.RESOLVED
        group by c.id, c.trackingCode, c.title, cat.name, d.name, c.status, c.resolvedAt, c.publicAnswer
        """)
    Page<PublicFeedRow> publicFeedResolvedWithRatings(Pageable pageable);

}
