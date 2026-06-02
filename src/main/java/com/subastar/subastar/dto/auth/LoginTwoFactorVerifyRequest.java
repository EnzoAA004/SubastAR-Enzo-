package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginTwoFactorVerifyRequest {

    @NotBlank
    @JsonProperty("challenge_id")
    private String challengeId;

    @NotBlank
    @Size(min = 4, max = 8)
    private String codigo;
}
