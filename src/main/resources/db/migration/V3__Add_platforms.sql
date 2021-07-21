INSERT INTO platform (platform_key, name, is_default) VALUES ('io.quarkus.platform', 'Quarkus Community Platform', true);
--  Adds Productized platform (unused in community deployment)
INSERT INTO platform (platform_key, name, is_default) VALUES ('com.redhat.quarkus.platform', 'Quarkus Product Platform', false);