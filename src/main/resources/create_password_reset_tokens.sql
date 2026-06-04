IF OBJECT_ID('password_reset_tokens', 'U') IS NULL
BEGIN
    CREATE TABLE password_reset_tokens (
        id INT IDENTITY(1,1) PRIMARY KEY,
        email VARCHAR(255) NOT NULL,
        token VARCHAR(80) NOT NULL UNIQUE,
        codigo_hash VARCHAR(255) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        used_at DATETIME2 NULL,
        invalidated_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        attempts INT NOT NULL DEFAULT 0
    );
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_password_reset_tokens_email'
      AND object_id = OBJECT_ID('password_reset_tokens')
)
BEGIN
    CREATE INDEX IX_password_reset_tokens_email
    ON password_reset_tokens(email);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_password_reset_tokens_token'
      AND object_id = OBJECT_ID('password_reset_tokens')
)
BEGIN
    CREATE INDEX IX_password_reset_tokens_token
    ON password_reset_tokens(token);
END;
GO
