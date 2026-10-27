create table shedlock
(
    name       VARCHAR(64),
    lock_until timestamp    not null,
    lock_at    timestamp    not null,
    locked_by  varchar(255) not null,
    primary key (name)
);