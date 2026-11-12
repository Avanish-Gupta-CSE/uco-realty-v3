package com.ucorealty.repository;

import com.ucorealty.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
  // SQL: select * from realty_project_images where project_id = ?   (photos of a live project)
  List<ProjectImage> findByProjectId(String projectId);

  /** Photos of a staging request (not live yet). */
  // SQL: select * from realty_project_images where staging_id = ?
  List<ProjectImage> findByStagingId(Long stagingId);
  // Only ids: the photo bytes are not loaded just to build a list of pictures
  // Written query (JPQL, uses entity names): the first photo id of each project in the list
  @Query("select min(i.id), i.projectId from ProjectImage i where i.projectId in :ids and i.fileKind = 'PHOTO' group by i.projectId")
  List<Object[]> findFirstImageIds(@Param("ids") Collection<String> ids);

  // The PDF documents of a live project: [id, file kind] (BROCHURE / FLOOR_PLAN)
  @Query("select i.id, i.fileKind from ProjectImage i where i.projectId = :projectId and i.fileKind <> 'PHOTO' order by i.id")
  List<Object[]> findDocuments(@Param("projectId") String projectId);
}
