package com.learning.authservice.support.repository;

import com.learning.authservice.support.domain.SupportTicket;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class SupportTicketSpecification {

    /**
     * Builds a JPA Specification for admin ticket filtering.
     *
     * @param tenantId   required — scopes all queries to the active tenant
     * @param status     optional — OPEN | IN_PROGRESS | RESOLVED | CLOSED
     * @param email      optional — partial match on email
     * @param category   optional — exact match on category
     * @param isEnrolled optional — "true" = registered users only, "false" = guests only, null = all
     */
    public static Specification<SupportTicket> filterBy(
            String tenantId,
            String status,
            String email,
            String category,
            Boolean isEnrolled
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            // Always scope to tenant
            predicate = cb.and(predicate, cb.equal(root.get("tenantId"), tenantId));

            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(email)) {
                // Search across both email and userId columns
                var emailPredicate = cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
                var userIdPredicate = cb.like(cb.lower(root.get("userId")), "%" + email.toLowerCase() + "%");
                predicate = cb.and(predicate, cb.or(emailPredicate, userIdPredicate));
            }
            if (StringUtils.hasText(category)) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            }
            if (isEnrolled != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isEnrolled"), isEnrolled));
            }

            // Default sort: newest first
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return predicate;
        };
    }
}
