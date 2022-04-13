ALTER TABLE platform ADD COLUMN IF NOT EXISTS group_id varchar;
ALTER TABLE platform ADD COLUMN IF NOT EXISTS artifact_id varchar;

-- Set groupId and artifactId from the catalog
UPDATE platform set group_id = 'io.quarkus.platform', artifact_id = 'quarkus-bom-quarkus-platform-descriptor' where platform_key = 'io.quarkus.platform';
UPDATE platform set group_id = 'com.redhat.quarkus.platform', artifact_id = 'quarkus-bom-quarkus-platform-descriptor' where platform_key = 'com.redhat.quarkus.platform';

-- Create index for performance
CREATE INDEX platform_group_id_artifact_id_idx on platform (id, group_id, artifact_id);

