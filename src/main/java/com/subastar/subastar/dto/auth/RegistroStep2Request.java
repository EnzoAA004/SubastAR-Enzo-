package com.subastar.subastar.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroStep2Request {
    @NotBlank
    @JsonProperty("token_verificacion")
    private String tokenVerificacion;

    @NotBlank @Size(min = 8)
    private String password;

    @NotBlank
    @JsonProperty("password_confirmacion")
    private String passwordConfirmacion;
}
