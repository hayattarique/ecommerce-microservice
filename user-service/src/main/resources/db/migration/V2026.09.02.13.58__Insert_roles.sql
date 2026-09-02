INSERT INTO roles (
    name,
    description,
    active,
    created_at,
    updated_at,
    created_by,
    updated_by
)
VALUES
    (
        'ADMIN',
        'Administrator role',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'system',
        'system'
    ),
    (
        'USER',
        'Regular user role',
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        'system',
        'system'
    );
ON CONFLICT (name) DO NOTHING;