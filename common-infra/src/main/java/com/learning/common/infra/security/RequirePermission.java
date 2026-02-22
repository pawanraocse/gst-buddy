package com.learning.common.infra.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as requiring a specific permission.
 * The {@code AuthorizationAspect} intercepts annotated methods,
 * resolves the caller from the {@code X-User-Id} header, and
 * verifies that they hold the declared resource:action pair.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** Resource being accessed (e.g. "user", "credit", "plan"). */
    String resource();

    /** Action being performed (e.g. "read", "manage", "delete"). */
    String action();
}
