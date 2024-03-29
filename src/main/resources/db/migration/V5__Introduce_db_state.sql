create table db_state
(
    id         bigint generated by default as identity
        constraint db_state_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    updated_at timestamptz default CURRENT_TIMESTAMP
);

INSERT INTO db_state (updated_at) VALUES (CURRENT_TIMESTAMP);