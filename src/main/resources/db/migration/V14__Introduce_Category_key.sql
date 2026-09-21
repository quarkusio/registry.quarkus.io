ALTER TABLE category ADD COLUMN IF NOT EXISTS category_key varchar NOT NULL DEFAULT 'undefined';

UPDATE category SET category_key = 'core', description = 'Core Quarkus components: engine, logging, etc.' where name = 'Core';
UPDATE category SET category_key = 'web' where name = 'Web';
UPDATE category SET category_key = 'data' where name = 'Data';
UPDATE category SET category_key = 'messaging' where name = 'Messaging';
UPDATE category SET category_key = 'reactive' where name = 'Reactive';
UPDATE category SET category_key = 'cloud' where name = 'Cloud';
UPDATE category SET category_key = 'observability' where name = 'Observability';
UPDATE category SET category_key = 'security' where name = 'Security';
UPDATE category SET category_key = 'integration' where name = 'Integration';
UPDATE category SET category_key = 'grpc' where name = 'gRPC';
UPDATE category SET category_key = 'business-automation' where name = 'Business Automation';
UPDATE category SET category_key = 'serialization' where name = 'Serialization';
UPDATE category SET category_key = 'miscellaneous' where name = 'Miscellaneous';
UPDATE category SET category_key = 'compatibility' where name = 'Compatibility';
UPDATE category SET category_key = 'alt-languages' where name = 'Alternative languages';

ALTER TABLE category ADD CONSTRAINT category_key_idx unique (category_key);
