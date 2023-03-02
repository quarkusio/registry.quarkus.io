ALTER TABLE platform_release ADD COLUMN bom varchar;

UPDATE platform_release
SET bom = subquery.gav
FROM (select pr.id,
             p.group_id || ':' || REPLACE(p.artifact_id, '-quarkus-platform-descriptor', '') || '::pom:' ||
             pr.version as gav
      from platform_release pr
               cross join platform_stream ps
               cross join platform p
      where pr.platform_stream_id = ps.id
        and ps.platform_id = p.id) AS subquery
WHERE platform_release.id = subquery.id;

ALTER TABLE platform_release ALTER COLUMN bom SET NOT NULL;
