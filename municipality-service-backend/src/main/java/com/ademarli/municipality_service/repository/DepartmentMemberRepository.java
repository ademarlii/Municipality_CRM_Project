package com.ademarli.municipality_service.repository;

import com.ademarli.municipality_service.model.entity.DepartmentMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentMemberRepository extends JpaRepository<DepartmentMember, Long> {
    Optional<DepartmentMember> findByDepartmentIdAndUserId(Long departmentId, Long userId);

    List<DepartmentMember> findAllByDepartmentId(Long departmentId);


    boolean existsByDepartmentIdAndUserIdAndActiveTrue(Long departmentId, Long userId);

    @Query("""
      select dm.department.id
      from DepartmentMember dm
      where dm.user.id = :userId
        and dm.active = true
        and dm.department.active = true
    """)

    List<Long> findActiveDepartmentIdsByUserId(@Param("userId") Long userId);
}
