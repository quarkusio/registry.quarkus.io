create table CATEGORY
(
    id bigint not null
        constraint category_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    description varchar(4096),
    metadata json,
    name varchar(255) not null
        constraint uk_tfaydch44g7w53336v8nkr2fm
            unique
);

create table CORE_RELEASE
(
    id bigint not null
        constraint core_release_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    artifact_id varchar(255) not null,
    group_id varchar(255) not null,
    version varchar(255) not null,
    major_release_id bigint
        constraint fkfvc598sdrg8j6qqnanpwgjicw
            references CORE_RELEASE,
    constraint uk_krg9rqlarowbuqj34s6hiox3o
        unique (artifact_id, group_id, version)
);

create table EXTENSION
(
    id bigint not null
        constraint extension_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    artifact_id varchar(255) not null,
    description varchar(4096),
    group_id varchar(255) not null,
    name varchar(255) not null,
    constraint uk_6yeu1nba4s3cj7qwx2xeo52tq
        unique (artifact_id, group_id)
);

create table EXTENSION_RELEASE
(
    id bigint not null
        constraint extension_release_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    metadata json,
    version varchar(255) not null,
    extension_id bigint not null
        constraint fka1ko47ipb9gxqh6jhwy704b7
            references EXTENSION,
    constraint uk_2xrr8at461jxt1t1ha4jqb75s
        unique (extension_id, version)
);

create table EXTENSION_RELEASE_CORE_RELEASE
(
    extension_release_id bigint not null
        constraint fkomtg1g2aua6jv9sseonbgv1uf
            references EXTENSION_RELEASE,
    compatible_releases_id bigint not null
        constraint fkmbvab5er7mm2gv4y00u36hslo
            references CORE_RELEASE
);

create table PLATFORM
(
    id bigint not null
        constraint platform_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    artifact_id varchar(255) not null,
    group_id varchar(255) not null,
    constraint uk_fmdju0hrj6v0l87n9i4btk9ji
        unique (artifact_id, group_id)
);

create table PLATFORM_RELEASE
(
    id bigint not null
        constraint platform_release_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    metadata json,
    version varchar(255) not null,
    platform_id bigint
        constraint fkt84uvvm37q3fujjswuig0l5ji
            references PLATFORM,
    quarkus_version_id bigint
        constraint fkku6iwcywvsy7xkpyasutk9xoh
            references CORE_RELEASE,
    constraint uk_j614spsyw56w2o3ebf5ynm14
        unique (platform_id, version)
);

create table PLATFORM_EXTENSION
(
    id bigint not null
        constraint platform_extension_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    metadata json,
    extension_release_id bigint not null
        constraint fkecbedi4m381p3rvx8sj0va0cq
            references EXTENSION_RELEASE,
    platform_release_id bigint not null
        constraint fkkyjlb9eg2x3t7bwusyb0ueatm
            references PLATFORM_RELEASE,
    constraint uk_32m64tx5214yd569pyaxvhjc7
        unique (extension_release_id, platform_release_id)
);

create table PLATFORM_RELEASE_CATEGORY
(
    id bigint not null
        constraint platform_release_category_pkey
            primary key,
    created_at timestamp default CURRENT_TIMESTAMP,
    description varchar(4096),
    metadata json,
    name varchar(255),
    category_id bigint not null
        constraint fk2385yh01h07ugfeqqqhlgqgb
            references CATEGORY,
    platform_release_id bigint not null
        constraint fkd20pdi1xwvk9kiwju11hs9903
            references PLATFORM_RELEASE,
    constraint uk_3cs293h0rwjxy86ukuekp8c6k
        unique (category_id, platform_release_id)
);
