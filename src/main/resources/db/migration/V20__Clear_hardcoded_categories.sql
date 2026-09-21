-- Remove hardcoded category seed data from V2__Add_categories.sql
-- Categories are now dynamically populated from platform descriptors (catalog-overrides.json)
-- which are the authoritative source of truth for the category list.
-- This makes the platform descriptors (ultimately backed by Quarkus' catalog-overrides.json)
-- the single source of truth for categories, instead of the hardcoded seed in V2__Add_categories.sql.

-- First, delete the link table entries
DELETE FROM platform_release_category WHERE category_id IN (
    SELECT id FROM category WHERE category_key IN (
        'core', 'web', 'data', 'messaging', 'reactive', 'cloud', 'observability',
        'security', 'integration', 'grpc', 'business-automation', 'serialization',
        'miscellaneous', 'compatibility', 'alt-languages'
    )
);

-- Then delete the hardcoded categories
DELETE FROM category WHERE category_key IN (
    'core', 'web', 'data', 'messaging', 'reactive', 'cloud', 'observability',
    'security', 'integration', 'grpc', 'business-automation', 'serialization',
    'miscellaneous', 'compatibility', 'alt-languages'
);
