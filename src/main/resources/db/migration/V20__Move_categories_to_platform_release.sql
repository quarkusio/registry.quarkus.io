-- Categories are not something the registry curates independently: they arrive as part of the extension catalog a
-- platform publishes, and their name, description and metadata only ever come from that catalog. Storing them in their
-- own table meant they could drift from the platform that defines them, and in practice they did -- the table was
-- seeded with a hardcoded list in V2 that was never updated, so categories added upstream (AI, for one) never appeared.
--
-- They now live as a JSON document on the release that declared them. See PlatformCategory and docs/categories.md.

ALTER TABLE platform_release ADD COLUMN categories json NOT NULL DEFAULT '[]';

-- Carry across whatever the join table holds, preserving the order the rows were inserted in, which is the order the
-- catalog listed the categories in.
UPDATE platform_release pr
SET categories = agg.categories
FROM (SELECT prc.platform_release_id,
             json_agg(
                     json_build_object(
                             'id', c.category_key,
                             'name', c.name,
                             'description', c.description,
                             'metadata', prc.metadata
                     )
                     ORDER BY prc.id
             ) AS categories
      FROM platform_release_category prc
               JOIN category c ON c.id = prc.category_id
      GROUP BY prc.platform_release_id) agg
WHERE pr.id = agg.platform_release_id;

DROP TABLE platform_release_category;
DROP TABLE category;
