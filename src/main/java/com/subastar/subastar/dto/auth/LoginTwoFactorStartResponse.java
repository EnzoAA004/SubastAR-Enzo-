package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginTwoFactorStartResponse {

    @JsonProperty("requires_2fa")
    private boolean requiresTwoFactor;

    @JsonProperty("challenge_id")
    private String challengeId;

    private String email;
    private String message;
}
