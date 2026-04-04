package com.learning.authservice.signup.actions;

import com.learning.authservice.signup.pipeline.SignupAction;
import com.learning.authservice.signup.pipeline.SignupContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Action to generate a Tenant ID (Workspace ID) for the new user.
 * 
 * Order: 10
 */
@Component
@Order(10)
@Slf4j
public class GenerateTenantIdAction implements SignupAction {

    @Override
    public String getName() {
        return "GenerateTenantId";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public boolean supports(SignupContext ctx) {
        return true;
    }

    @Override
    public boolean isAlreadyDone(SignupContext ctx) {
        return ctx.getTenantId() != null;
    }

    @Override
    public void execute(SignupContext ctx) {
        log.info("Generating Tenant ID for email: {}", ctx.getEmail());
        String tenantId;
        
        if (ctx.getSignupType() == SignupContext.SignupType.ORGANIZATION && ctx.getCompanyName() != null) {
            String slug = ctx.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "-");
            tenantId = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        } else {
            String prefix = ctx.getEmail().split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
            tenantId = prefix + "-" + System.currentTimeMillis();
        }
        
        ctx.setTenantId(tenantId);
        log.info("Generated Tenant ID: {}", tenantId);
    }
    
    @Override
    public void rollback(SignupContext ctx) {
        // Nothing to rollback
    }
}
