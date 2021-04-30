create table category
(
    id bigint generated by default as identity
        constraint category_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    description varchar(4096),
    metadata json,
    name varchar(255) not null
        constraint category_nkey
            unique
);

create table extension
(
    id bigint generated by default as identity
        constraint extension_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    artifact_id varchar(255) not null,
    description varchar(4096),
    group_id varchar(255) not null,
    name varchar(255) not null,
    constraint extension_nkey
        unique (artifact_id, group_id)
);

create table extension_release
(
    id bigint generated by default as identity
        constraint extension_release_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    quarkus_core varchar(255) not null,
    metadata json,
    version varchar(255) not null,
    version_sortable varchar(100) not null,
    extension_id bigint not null
        constraint fka1ko47ipb9gxqh6jhwy704b7
            references extension,
    constraint extension_release_nkey
        unique (extension_id, version)
);

create table platform
(
    id bigint generated by default as identity
        constraint platform_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    artifact_id varchar(255) not null,
    group_id varchar(255) not null,
    is_default boolean default false,
    constraint platform_nkey
        unique (artifact_id, group_id)
);

create table platform_release
(
    id bigint generated by default as identity
        constraint platform_release_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    metadata json,
    version varchar(255) not null,
    version_sortable varchar(100) not null,
    quarkus_core varchar(255) not null,
    quarkus_core_upstream varchar(255),
    platform_id bigint
        constraint platform_fkey
            references platform,
    constraint platform_release_nkey
        unique (platform_id, version)
);

create table platform_extension
(
    id bigint generated by default as identity
        constraint platform_extension_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
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

create table platform_release_category
(
    id bigint generated by default as identity
        constraint platform_release_category_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    description varchar(4096),
    metadata json,
    name varchar(255),
    category_id bigint not null
        constraint platform_release_category_category_fkey
            references category,
    platform_release_id bigint not null
        constraint platform_release_category_fkey
            references platform_release,
    constraint platform_release_category_nkey
        unique (category_id, platform_release_id)
);