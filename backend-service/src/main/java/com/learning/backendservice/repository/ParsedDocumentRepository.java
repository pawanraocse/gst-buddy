package com.learning.backendservice.repository;

import com.learning.backendservice.entity.ParsedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ParsedDocumentRepository extends JpaRepository<ParsedDocument, UUID> {
    List<ParsedDocument> findByTenantId(String tenantId);
}
