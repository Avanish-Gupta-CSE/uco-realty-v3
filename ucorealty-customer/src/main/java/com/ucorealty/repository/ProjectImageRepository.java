package com.ucorealty.repository;

import com.ucorealty.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
  // SQL: select * from realty_project_images where project_id = ?
  List<ProjectImage> findByProjectId(String projectId);
  // Only ids: the photo bytes are not loaded just to build a list of pictures
  // Written query (JPQL, uses entity names): the first photo id of each project in the list
  @Query("select min(i.id), i.projectId from ProjectImage i where i.projectId in :ids and i.fileKind = 'PHOTO' group by i.projectId")
  List<Object[]> findFirstImageIds(@Param("ids") Collection<String> ids);

  @Query("select i.id from ProjectImage i where i.projectId = :projectId and i.fileKind = 'PHOTO' order by i.id")
  List<Long> findIdsByProjectId(@Param("projectId") String projectId);

  // The PDF documents of a live project: [id, file kind] (BROCHURE / FLOOR_PLAN)
  @Query("select i.id, i.fileKind from ProjectImage i where i.projectId = :projectId and i.fileKind <> 'PHOTO' order by i.id")
  List<Object[]> findDocuments(@Param("projectId") String projectId);

}
