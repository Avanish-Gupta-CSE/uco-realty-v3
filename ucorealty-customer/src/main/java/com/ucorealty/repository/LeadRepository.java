package com.ucorealty.repository;

import com.ucorealty.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

// Note: save(), findById(), findAll(), delete() need no SQL: JpaRepository provides them.
public interface LeadRepository extends JpaRepository<Lead, Long> {
}
