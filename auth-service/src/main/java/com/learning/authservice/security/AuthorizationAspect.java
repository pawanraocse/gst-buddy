package com.learning.authservice.security;

import com.learning.authservice.security.service.AuthorizationService;
import com.learning.common.constants.HeaderNames;
import com.learning.common.infra.exception.PermissionDeniedException;
import com.learning.common.infra.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that enforces {@link RequirePermission} on controller methods.
 * Reads the caller's identity from the {@code X-User-Id} request header
 * and verifies that they hold the declared permission via {@link AuthorizationService}.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationAspect {

    private final AuthorizationService authorizationService;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequirePermission requirePermission) throws Throwable {

        String userId = resolveUserId();
        String resource = requirePermission.resource();
        String action = requirePermission.action();
        String permissionId = resource + ":" + action;

        if (!authorizationService.hasPermission(userId, resource, action)) {
            log.warn("Permission denied: userId={}, required={}", userId, permissionId);
            throw new PermissionDeniedException(
                    "User " + userId + " lacks permission " + permissionId);
        }

        log.debug("Permission granted: userId={}, permission={}", userId, permissionId);
        return joinPoint.proceed();
    }

    private String resolveUserId() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new PermissionDeniedException("No request context available");
        }

        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new PermissionDeniedException("Missing " + HeaderNames.USER_ID + " header");
        }
        return userId;
    }
}
