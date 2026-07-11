CREATE TABLE user_credentials
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    user_account_id BIGINT       NOT NULL,
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    version    INT          NOT NULL DEFAULT 0
);

CREATE TABLE refresh_tokens
(
    id              BIGSERIAL PRIMARY KEY,
    credential_id   BIGINT       NOT NULL REFERENCES user_credentials (id) ON DELETE CASCADE,
    token           VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(255) NOT NULL,
    expired_at      TIMESTAMP,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    user_account_id BIGINT       NOT NULL,
    updated_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(255) NOT NULL,
    version    INT          NOT NULL DEFAULT 0
);