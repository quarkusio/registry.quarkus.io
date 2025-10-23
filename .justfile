
# Run Quarkus dev locally
dev:
   quarkus dev

# build main project fast - skip docs, tests, ITs, invoker, extension validation, gradle tests, truststore
local:
  quarkus dev -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus -Dquarkus.datasource.username=quarkus -Dquarkus.datasource.password=quarkus