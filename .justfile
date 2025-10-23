
# Run Quarkus dev locally
dev:
   quarkus dev

# Run Quarkus dev with a local DB
local:
  quarkus dev -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus -Dquarkus.datasource.username=quarkus -Dquarkus.datasource.password=quarkus