ALTER TABLE extension_release
    ADD COLUMN IF NOT EXISTS quarkus_core_version_sortable varchar not null default '0';