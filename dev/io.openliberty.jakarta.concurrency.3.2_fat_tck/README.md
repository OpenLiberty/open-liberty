# Jakarta Concurrency TCK Open Liberty Implementation

This project runs the Jakarta Concurrency Technology Compatibility Kit (TCK) on Open Liberty.
Before running this project it is recommended to read the documentation located in the base [TCK project](https://github.com/jakartaee/concurrency/blob/main/tck/README.md)

## Overview

In order to aid in the development of the `concurrent-3.2` feature, this project is configured specifically to allow
the feature developers to run the TCK against a development image of Open Liberty.

## Choose a test strategy

This project has been constructed to support two different test strategies.

1. Running the TCK for Verification
   1. This strategy is for those who want to run the TCK to verify compatibility, but do not need to run against a development environment.
2. Running the TCK for Developers
   1. This strategy is for those who are familiar with the Open Liberty project and are helping to develop the `concurrent-3.2` feature.

Many of the assets in this project are used in different ways based on the strategy being used. 
Therefore, each strategy below has a `Background` section to explain how the different assets interact and end up depending on the strategy.

## Requirements

- JDK 21 or higher
- [Maven 3.8.0 or higher](https://maven.apache.org/download.cgi)

## Getting started

### Building Jakarta Concurrency

If you want to test against the latest API and TCK versions the first step will be to clone and build the Jakarta Concurrency project.

```sh
git clone git@github.com:jakartaee/concurrency.git
cd concurrency
mvn clean install
```

The API and TCK libraries will be tagged with the `3.2.0` version. Keep this in mind if you plan to follow the [Running the TCK for Verification](#Running-the-TCK-for-Verification) section. 

### Getting Open Liberty

You will first need to clone the Open Liberty project:

```sh
git clone git@github.com:OpenLiberty/open-liberty.git
cd open-liberty/dev
```

## Running the TCK for Verification

### Background

1. `build.gradle` 
   1. This project is set up using gradle.
   2. The `build.gradle` file has tasks to set up a `wlp` directory with an Open Liberty install.
   3. There are additional tasks that will copy dependencies, configurations, and create a server.
2. `publish/servers`
   1. This is where the server configuration files are located.
   2. These files will be copied to `wlp/usr/servers/ConcurrentTCKServer` at build time.
   3. A special `logging.properties` file is also here to ensure that the TCK logs are formatted and saved to a specific location.
3. `publish/tckRunner`
   1. This is a maven project that will actually run the TCK. 
   2. This project contains the TCK configuration files that will be copied to the `wlp` directory at build time.
   3. This includes the pom.xml that is used to get the TCK itself, and the dependencies to run the TCK. 
   4. This is also where the TestNG and Arquillian configuration files are located.
4. `fat/src`
   1. This asset is ignored for this test strategy.

### Project set up

First, check out the release branch: 

```sh
git checkout release
git pull
```

To make things simpler this project contains custom gradle tasks for setting up a standalone Liberty Profile.

First open `build.gradle`. 
You need to complete the `TODO` under dependencies.
Provide a WebSphere Liberty Profile (WLP) dependency that corresponds to the release you want to test against.

Then, run the `buildWLP` task

```sh
./gradlew io.openliberty.jakarta.concurrency.3.2_fat_tck:buildWLP -Djakarta.tck.platform=web
```

To run in full mode use `-Djakarta.tck.platform=full`

This will create a `wlp` directory within this project.
Navigate to this directory and start your server:

```sh
cd io.openliberty.jakarta.concurrency.3.2_fat_tck/wlp

./bin/server start ConcurrentTCKServer \
-Denv.tck_port=9080 \
-Denv.tck_port_secure=9443 \
-Djimage.dir=$PWD/usr/shared/jimage/output
```
### Run the TCK

Now you can run the TCK tests. 

```sh
mvn clean test -B \
-Dwlp=$PWD \
-Dtck_server=ConcurrentTCKServer \
-Dtck_failSafeUndeployment=true \
-Dtck_appDeployTimeout=180 \
-Dtck_appUndeployTimeout=60 \
-Dtck_port=9080 \
-Djakarta.tck.platform=web \
-Dsun.rmi.transport.tcp.responseTimeout=60000 \
-Djava.util.logging.config.file=$PWD/logging.properties
```

To run in full mode use `-Djakarta.tck.platform=full`

By default the TCK will run against a staged version of Jakarta API and TCK uploaded to sonatype.
If you want to test against a local `3.2.0-SNAPSHOT` then set these properties on the command above: 

```txt
-Djakarta.concurrent.tck.groupid=jakarta.enterprise.concurrent \
-Djakarta.concurrent.tck.version=3.2.0-SNAPSHOT
```

Finally, remember to stop the running server

```sh
./bin/server stop ConcurrentTCKServer
```

### View the results

The test results will be located under the following directory:

```txt
/wlp/tck/target/surefire/surefire-reports
```

## Running the TCK for Developers

### Background

1. `build.gradle` 
   1. This project is built and run using gradle.
   2. The parent gradle project has a `fat.gradle` file that controls this project's build and runtime lifecycle.
   3. The build.gradle file in this project has specific tasks to pull and copy dependencies needed by the server at runtime. 
2. `publish/servers`
   1. This is where the server configuration files are located.
   2. These files will be copied to `build/libs/autoFVT` and later deployed to the `wlp` directory at runtime.
   3. A special `logging.properties` file is also here to ensure that the TCK logs are formatted and saved to a specific location.
3. `publish/tckRunner`
   1. This is a maven sub-project that will actually run the TCK. 
   2. This sub-project contains the TCK configuration files that will be copied to the `build/libs/autoFVT` directory at build time.
   3. This includes the pom.xml that is used to get the TCK itself, and the dependencies to run the TCK. 
   4. This is also where the TestNG and Arquillian configuration files are located.
4. `fat/src`
   1. At runtime we use JUnit to run a proxy test.
   2. This test will start the Open Liberty development server.
   3. Then, this test will then run the `mvn clean test` target on the `tckRunner` sub-project.

### Project set up

Option 1) If you are working on new functionality and want to run the TCK against the latest version of 
Open Liberty then you will want to check the release (default) branch:

```sh
git checkout release
git pull
```

Option 2) If you want to run the TCK against a specific release of Open Liberty to check compatibility, 
then you will want to check out the corresponding release branch:

```sh
git fetch --all --tags
git checkout <your-release-branch>
```

You will need to build a development image of Open Liberty to test against based on the 
branch checked out above:

```sh
./gradlew cnf:initialize
./gradlew assemble :com.ibm.websphere.appserver.features:releaseNeeded
```

For more detailed instructions on building Open Liberty please see the [Building Open Liberty Wiki](https://github.com/OpenLiberty/open-liberty/wiki/Building-Open-Liberty)

### Run the TCK

Finally, you can run the TCK using the `buildandrun` gradle task

```sh
./gradlew io.openliberty.jakarta.concurrency.3.2_fat_tck:buildandrun
```

NOTE: Running the TCK using this method will use a published TCK version, and not the `SNAPSHOT` version from a local build. 

### View the results

The test results will be located under the following directory:

```txt
build/libs/autoFVT/results/junit
```