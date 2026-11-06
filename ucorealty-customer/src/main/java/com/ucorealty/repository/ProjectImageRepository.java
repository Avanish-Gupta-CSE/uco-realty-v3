package com.ucorealty.repository;

import com.ucorealty.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
  List<ProjectImage> findByProjectId(String projectId);
  // Only ids: the photo bytes are not loaded just to build a list of pictures
  @Query("select min(i.id), i.projectId from ProjectImage i where i.projectId in :ids group by i.projectId")
  List<Object[]> findFirstImageIds(@Param("ids") Collection<String> ids);

  @Query("select i.id from ProjectImage i where i.projectId = :projectId order by i.id")
  List<Long> findIdsByProjectId(@Param("projectId") String projectId);
}
