package com.learning.authservice.support.repository;

import com.learning.authservice.support.domain.TicketReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketReplyRepository extends JpaRepository<TicketReply, UUID> {
}
