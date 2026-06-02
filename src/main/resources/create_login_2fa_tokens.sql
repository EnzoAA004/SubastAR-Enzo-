IF OBJECT_ID('login_2fa_tokens', 'U') IS NULL
BEGIN
    CREATE TABLE login_2fa_tokens (
        id INT IDENTITY(1,1) PRIMARY KEY,
        challenge_id VARCHAR(80) NOT NULL UNIQUE,
        email VARCHAR(255) NOT NULL,
        persona_id INT NOT NULL,
        codigo_hash VARCHAR(255) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        used_at DATETIME2 NULL,
        invalidated_at DATETIME2 NULL,
        attempts INT NOT NULL DEFAULT 0,
        creado_en DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_login_2fa_tokens_email'
      AND object_id = OBJECT_ID('login_2fa_tokens')
)
BEGIN
    CREATE INDEX IX_login_2fa_tokens_email
    ON login_2fa_tokens(email);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_login_2fa_tokens_challenge'
      AND object_id = OBJECT_ID('login_2fa_tokens')
)
BEGIN
    CREATE INDEX IX_login_2fa_tokens_challenge
    ON login_2fa_tokens(challenge_id);
END;
GO
