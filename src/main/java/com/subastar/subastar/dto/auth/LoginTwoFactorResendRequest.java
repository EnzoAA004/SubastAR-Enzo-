package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginTwoFactorResendRequest {

    @NotBlank
    @JsonProperty("challenge_id")
    private String challengeId;
}
