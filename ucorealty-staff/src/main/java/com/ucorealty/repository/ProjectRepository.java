package com.ucorealty.repository;

import com.ucorealty.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// Repository = DB helper. No SQL needed for basic CRUD (like a typed fetch wrapper).
public interface ProjectRepository extends JpaRepository<Project, String> {
  List<Project> findByStatusAndIsActiveAndIsPublished(String status, Integer active, Integer published);
  List<Project> findByZoneNameIgnoreCase(String zoneName);

  // Row lock: two people reducing units at the same moment cannot both read the same old count.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Project p where p.id = :id")
  Optional<Project> findByIdForUpdate(@Param("id") String id);
}
