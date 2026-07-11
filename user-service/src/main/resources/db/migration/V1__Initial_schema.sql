CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,

    email         VARCHAR(255) NOT NULL UNIQUE,

    first_name    VARCHAR(100) NOT NULL,

    last_name     VARCHAR(100) NOT NULL,

    gender        VARCHAR(20)  NOT NULL
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),

    mobile        VARCHAR(20) UNIQUE,

    date_of_birth DATE         NOT NULL,

    active        BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(255) NOT NULL,
    updated_by    VARCHAR(255) NOT NULL,
    version       INT          NOT NULL DEFAULT 0
);

CREATE TABLE roles
(
    id          BIGSERIAL PRIMARY KEY,

    name        VARCHAR(100) NOT NULL UNIQUE,

    description VARCHAR(255),

    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255) NOT NULL,
    version     INT          NOT NULL DEFAULT 0
);

CREATE TABLE permissions
(
    id          BIGSERIAL PRIMARY KEY,

    name        VARCHAR(100) NOT NULL UNIQUE,

    description VARCHAR(255),

    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255) NOT NULL,
    version     INT          NOT NULL DEFAULT 0
);

CREATE TABLE user_roles
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT       NOT NULL,

    role_id    BIGINT       NOT NULL,

    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,

    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
            REFERENCES users (id),

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id),
    version    INT          NOT NULL DEFAULT 0


);

CREATE TABLE role_permissions
(
    id            BIGSERIAL PRIMARY KEY,

    role_id       BIGINT       NOT NULL,

    permission_id BIGINT       NOT NULL,

    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(255) NOT NULL,
    updated_by    VARCHAR(255) NOT NULL,

    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id),

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions (id),
    version       INT          NOT NULL DEFAULT 0
);
