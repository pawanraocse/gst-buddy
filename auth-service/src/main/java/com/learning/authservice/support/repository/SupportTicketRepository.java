package com.learning.authservice.support.repository;

import com.learning.authservice.support.domain.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID>, JpaSpecificationExecutor<SupportTicket> {
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<SupportTicket> findByTenantId(String tenantId, Pageable pageable);
}
