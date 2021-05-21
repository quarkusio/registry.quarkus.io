# Extension Registry Application

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You need a PostgreSQL DB running. If you don't have one, use docker or podman:

    podman run -e POSTGRESQL_ADMIN_PASSWORD=admin --net=host registry.redhat.io/rhel8/postgresql-12 

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw clean compile quarkus:dev
```

## Indexing Quarkiverse extensions

Once the application is running, clone the https://github.com/quarkusio/quarkus-extension-catalog and run the following in the root of your cloned repo: 
          
    jbang .github/workflows/publishcatalog.java --working-directory=. --registry-url=http://localhost:8080 --token=test -sv

## Endpoints: 
- List of platforms (marked with the recommended platform)
- Descriptor of the chosen platform (extensions belonging to platform)

### Maven endpoints  

#### **io.quarkus.registry:quarkus-platforms::json:1.0-SNAPSHOT** 
A JSON file that lists the preferred versions of every registered platform (e.g. quarkus-bom, quarkus-universe-bom, ect). It also indicates which platform is the default one (for project creation), e.g. the quarkus-universe-bom;

#### io.quarkus.registry:quarkus-platforms:<QUARKUS-VERSION>:json:1.0-SNAPSHOT - 

Same as above but per Quarkus core version expressed with <QUARKUS-VERSION> as the artifact’s classifier;

#### io.quarkus.registry:quarkus-non-platform-extensions:<QUARKUS-VERSION>:json:1.0-SNAPSHOT 

JSON catalog of non-platform extensions that are compatible with a given Quarkus core version expressed with <QUARKUS-VERSION> as the artifact’s classifier;

#### io.quarkus.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT 

The JSON registry descriptor which includes the default settings to communicate with the registry (including specific groupId, artifactId and versions for the QER artifacts described above, Maven repository URL, etc).

#### Platform

    /maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/metadata.xml
    /maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.pom
    /maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.json

#### Extensions

    /maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/metadata.xml
    /maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-<QUARKUS_VERSION>-1.0-SNAPSHOT.pom
    /maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms--<QUARKUS_VERSION>-1.0-SNAPSHOT.json

#### Non-Platform Extensions

    /maven/io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/metadata.xml
    /maven/io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT.pom
    /maven/io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.json

## GraphQL queries

The following queries below are useful to gather data to feed the application's database. 
You can go to https://graphql.github.com to perform these queries.

### All extension repositories in the Quarkiverse org

```graphql
{
    search(query: "topic:quarkus-extension org:quarkiverse", type: REPOSITORY, first: 100) {
		repositoryCount
        nodes {
            ... on Repository {
                name
            }
        }
    }
}
```
