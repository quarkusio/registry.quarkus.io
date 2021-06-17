create table category
(
    id bigint generated by default as identity
        constraint category_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    description varchar,
    metadata json,
    name varchar not null
        constraint category_nkey
            unique
);

create table extension
(
    id bigint generated by default as identity
        constraint extension_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    group_id varchar not null,
    artifact_id varchar not null,
    name varchar not null,
    description varchar,
    constraint extension_nkey
        unique (artifact_id, group_id)
);

create table extension_release
(
    id bigint generated by default as identity
        constraint extension_release_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    quarkus_core_version varchar not null,
    metadata json,
    version varchar not null,
    version_sortable varchar not null,
    extension_id bigint not null
        constraint fka1ko47ipb9gxqh6jhwy704b7
            references extension,
    constraint extension_release_nkey
        unique (extension_id, version)
);

create table extension_release_compatibility
(
    id bigint generated by default as identity
        constraint extension_release_compatibility_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    quarkus_core_version varchar not null,
    compatible bool not null,
    extension_release_id bigint not null
        constraint extension_release_compatibility_fkey
            references extension_release,
    constraint extension_release_compatibility_nkey
        unique (extension_release_id, quarkus_core_version)
);

create table platform
(
    id bigint generated by default as identity
        constraint platform_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    platform_key varchar not null,
    name varchar,
    metadata json,
    is_default boolean default false,
    constraint platform_nkey
        unique (platform_key)
);

create table platform_stream
(
    id bigint generated by default as identity
        constraint platform_stream_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    stream_key varchar not null,
    name varchar,
    metadata json,
    platform_id bigint
        constraint platform_fkey
            references platform,
    constraint platform_stream_nkey
        unique (platform_id, stream_key)
);


create table platform_release
(
    id bigint generated by default as identity
        constraint platform_release_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    metadata json,
    member_boms json,
    version varchar not null,
    version_sortable varchar not null,
    quarkus_core_version varchar not null,
    upstream_quarkus_core_version varchar,
    platform_id bigint
        constraint platform_fkey
            references platform,
    platform_stream_id bigint
        constraint platform_stream_fkey
            references platform_stream,
    constraint platform_release_nkey
        unique (platform_id, version)
);

create table platform_extension
(
    id bigint generated by default as identity
        constraint platform_extension_pkey
            primary key,
    created_at timestamptz default CURRENT_TIMESTAMP,
    metadata json,
    extension_release_id bigint not null
        constraint platform_extension_release_fkey
            references extension_release,
    platform_release_id bigint not null
        constraint platform_platform_release_fkey
            references platform_release,
    constraint platform_extension_nkey
        unique (extension_release_id, platform_release_id)
);