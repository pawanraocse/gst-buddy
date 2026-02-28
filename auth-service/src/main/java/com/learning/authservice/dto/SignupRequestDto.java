package com.learning.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequestDto(
                @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,

                @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

                @NotBlank(message = "Name is required") String name,

                // Optional: Only for Organization Signup
                String companyName,

                // Optional: Only for Organization Signup
                String tier,

                // Optional: Referral code from signup link
                String referralCode) {
}
