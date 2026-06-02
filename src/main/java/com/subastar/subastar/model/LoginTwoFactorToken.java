package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "login_2fa_tokens")
public class LoginTwoFactorToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "challenge_id", nullable = false, unique = true, length = 80)
    private String challengeId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "persona_id", nullable = false)
    private Integer personaId;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (attempts == null) attempts = 0;
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
