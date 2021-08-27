# Extension Registry Application

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

The application will start a PostgreSQL DB using [DevServices](https://quarkus.io/guides/datasource#dev-services-configuration-free-databases). 

Run the following command to start the application (make sure your Docker daemon is running):

```shell script
./mvnw clean compile quarkus:dev -Ddebug
```

## Indexing extensions and platforms

Once the application is running, clone the https://github.com/quarkusio/quarkus-extension-catalog and run the following in the root of your cloned repo: 
          
    jbang publishcatalog@quarkusio --working-directory=. --registry-url=http://localhost:8080 --token=test -sv

## CI Builds

CI deploys a new tag to [Quay](https://quay.io/repository/quarkus/registry-app?tab=tags) on every build. The tag is based on the first 7 characters from the SHA1 commit.

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

## Troubleshooting

### My extension or the latest platform is still not listed. Why?
Updates to the registry.quarkus.io happen from an scheduled job set in https://github.com/quarkusio/quarkus-extension-catalog.
Everytime an extension or platform update is detected, the corresponding YAML file is updated with the detected version. 
If you would still like to manually trigger the indexing, do the following:

- Clone https://github.com/quarkusio/quarkus-extension-catalog
- Run the following command (make sure you have [JBang](https://www.jbang.dev/documentation/guide/latest/installation.html) installed): 
```bash
jbang publishcatalog@quarkusio --working-directory=. --registry-url=https://registry.quarkus.io --token=$TOKEN -sv` 
```

`$TOKEN` is a shared secret known to the registry maintainers.

If you need help please [open an issue](https://github.com/quarkusio/registry.quarkus.io/issues).

### SRCFG00011: Could not expand value buildNumber in property quarkus.application.version

If while running the application during quarkus:dev mode and changing the `application.properties` file, you may encounter this error once you restart the application (by typing `s`): 

```
One or more configuration errors have prevented the application from starting. The errors are:
  - SRCFG00011: Could not expand value buildNumber in property quarkus.application.version
```

The workaround is to create an `.env` file in the root with the following contents: 

```properties
buildNumber=999-SNAPSHOT
```

### How to update/bump a deployed version? 

If you need to update a registry.quarkus.io application running in an openshift cluster, 
use the `./update_tag.sh` script after performing a `oc login` in the cluster. This will tag the `quarkus-registry-app:production` imagestream with the tag provided. 

### How to register as a Nexus Repository proxy

You can register this as a Nexus repository proxy. You need to be an administrator to perform these operations.

#### Nexus 2.x
Some options need to be set:

- Set the `Repository Policy` to `Snapshot`;
- Disable `Download Remote Indexes`;
- Disable `Allow File Browsing`;
- Disable `Include in Search`.

Here is an example on how it should look like: 

![image](https://user-images.githubusercontent.com/54133/129068554-1db01f5e-5b57-405b-8386-636449e7d6ae.png)


#### Nexus 3.x

- Create a `maven2(proxy)` repository
- Set the `Version Policy` to `Snapshot`
- Set the `Remote Storage` URL to `https://registry.quarkus.io/maven`

![image](https://user-images.githubusercontent.com/54133/131173101-989974fd-f01c-4db0-889c-d493835546eb.png)
