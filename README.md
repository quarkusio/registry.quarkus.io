# Extension Registry Application

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You need a PostgreSQL DB running. If you don't have one, use docker or podman:

    podman run -e POSTGRESQL_ADMIN_PASSWORD=admin --net=host registry.redhat.io/rhscl/postgresql-12-rhel7 

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

## Indexing

Once the application is running, you can trigger the index for a specific platform by running 
      
    http --form POST localhost:8080/admin/registry/platform groupId=io.quarkus artifactId=quarkus-bom version=1.10.0.Final




#TODO:

- Rejected/on-hold: Use GitHub as the main input for data and have the app clone the repository

## Endpoints: 
- List of platforms (marked with the recommended platform)
- Descriptor of the chosen platform (extensions belonging to platform)
