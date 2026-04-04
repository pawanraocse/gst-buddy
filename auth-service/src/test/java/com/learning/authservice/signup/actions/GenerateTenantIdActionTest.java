package com.learning.authservice.signup.actions;

import com.learning.authservice.signup.pipeline.SignupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTenantIdActionTest {

    private GenerateTenantIdAction action;

    @BeforeEach
    void setUp() {
        action = new GenerateTenantIdAction();
    }

    @Test
    void execute_PersonalSignup_GeneratesTenantIdBasedOnEmail() {
        SignupContext ctx = SignupContext.builder()
                .signupType(SignupContext.SignupType.PERSONAL)
                .email("test.user.123@example.com")
                .build();

        action.execute(ctx);

        assertNotNull(ctx.getTenantId());
        assertTrue(ctx.getTenantId().startsWith("testuser123-"));
    }

    @Test
    void execute_OrganizationSignupWithMissingCompanyName_GeneratesTenantIdBasedOnEmail() {
        SignupContext ctx = SignupContext.builder()
                .signupType(SignupContext.SignupType.ORGANIZATION)
                .email("admin@startup.com")
                .build();

        action.execute(ctx);

        assertNotNull(ctx.getTenantId());
        assertTrue(ctx.getTenantId().startsWith("admin-"));
    }

    @Test
    void execute_OrganizationSignupWithCompanyName_GeneratesTenantIdBasedOnSlug() {
        SignupContext ctx = SignupContext.builder()
                .signupType(SignupContext.SignupType.ORGANIZATION)
                .email("admin@startup.com")
                .companyName("Acme Corp Inc. (HQ)")
                .build();

        action.execute(ctx);

        assertNotNull(ctx.getTenantId());
        assertTrue(ctx.getTenantId().startsWith("acme-corp-inc---hq--"));
    }

    @Test
    void isAlreadyDone_ReturnsTrueIfTenantIdExists() {
        SignupContext ctx = SignupContext.builder()
                .tenantId("existing-tenant-id")
                .build();

        assertTrue(action.isAlreadyDone(ctx));
    }

    @Test
    void isAlreadyDone_ReturnsFalseIfTenantIdIsNull() {
        SignupContext ctx = SignupContext.builder().build();

        assertFalse(action.isAlreadyDone(ctx));
    }
}
