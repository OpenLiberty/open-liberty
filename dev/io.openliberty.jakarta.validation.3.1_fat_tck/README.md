# Jakarta Validation TCK Open Liberty Implementation

This project runs the Jakarta Validation Technology Compatibility Kit (TCK) on Open Liberty.
Before running this project it is recommended to read the documentation located in the base [TCK project](https://github.com/jakartaee/validation-tck/blob/main/README.md)

## Overview

In order to aid in the development of the `validation 3.1` feature, this project is configured specifically to allow
the feature developers to run the TCK against a development image of Open Liberty.

## Requirements

- [JDK 17 or higher](https://adoptium.net/en-GB/temurin/archive/?version=17)
- [Maven 3.6.0 or higher](https://maven.apache.org/download.cgi)


## Running the TCK for Developers

### Background
`build.gradle` 
   1. This project is built and run using gradle.
   2. The parent gradle project has a `fat.gradle` file that controls this project's build and runtime lifecycle.
   3. The build.gradle file in this project has specific tasks to pull and copy dependencies needed by the server at runtime. 

`publish/servers`
   1. This is where the server configuration files are located.
   2. These files will be copied to `build/libs/autoFVT` and later deployed to the `wlp` directory at runtime.
   3. A special `logging.properties` file is also here to ensure that the TCK logs are formatted and saved to a specific location.

`publish/tckRunner`
   1. This is a maven sub-project that will actually run the TCK. 
   2. This sub-project contains the TCK configuration files that will be copied to the `build/libs/autoFVT` directory at build time.
   3. This includes the pom.xml that is used to get the TCK itself, and the dependencies to run the TCK. 
   4. This is also where the TestNG and Arquillian configuration files are located.

`fat/src`
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
./gradlew io.openliberty.jakarta.validation.3.1_fat_tck:buildandrun
```

NOTE: Running the TCK using this method will use a published TCK version, and not the `SNAPSHOT` version from a local build. 

### View the results

The test results will be located under the following directory:

```txt
build/libs/autoFVT/results/junit
```