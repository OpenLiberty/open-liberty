![](https://github.com/OpenLiberty/open-liberty/blob/master/logos/logo_horizontal_light_navy.png)

# OpenLiberty

[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/OpenLibertyIO)
[![License](https://img.shields.io/badge/License-EPL%201.0-green.svg)](https://opensource.org/licenses/EPL-1.0)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Maven Central](https://img.shields.io/maven-central/v/io.openliberty/openliberty-runtime.svg)](https://search.maven.org/#artifactdetails%7Cio.openliberty%7Copenliberty-runtime%7C18.0.0.1%7Czip)

# Summary
Open Liberty is a highly composable, fast to start, dynamic application server runtime environment.

# Table of Contents
* [Getting Started](https://github.com/OpenLiberty/open-liberty#getting-started)
* [Contribute to Open Liberty](https://github.com/OpenLiberty/open-liberty#contribute-to-open-liberty)
* [Community](https://github.com/OpenLiberty/open-liberty#community)

## Getting Started

### Downloading
You can download released versions and nightly build artifacts from: https://www.openliberty.io/downloads/

### Quick start
    git clone https://github.com/OpenLiberty/sample-getting-started.git
    cd sample-getting-started
    mvn clean install

Open browser to http://localhost:9080/ and explore the demo application. Afterwards run `mvn liberty:stop-server` to stop the server.

### Open Liberty Guides
Visit the [OpenLiberty website](https://openliberty.io/guides/) for a number of step by step guides.


## Contribute to Open Liberty
Our [CONTRIBUTING](https://github.com/OpenLiberty/open-liberty/blob/master/CONTRIBUTING.md) document contains details for submitting pull requests.

1. Clone the repository to your system.

    ```git clone git@github.com:OpenLiberty/open-liberty.git```

2. Run a gradle build.

    ```cd open-liberty/dev```
    
    ```./gradlew cnf:initialize```

    ```./gradlew assemble```

3. Run the unit or FAT tests.

   ```./gradlew test``` for unit tests
   
   ```./gradlew build.example_fat:buildandrun``` to run a [FAT project](https://github.com/OpenLiberty/open-liberty/wiki/FAT-tests)
   
   **NOTE:** ```./gradlew build``` runs assemble and test targets
   
4. Perform a local release

    ```./gradlew releaseNeeded```
    
    **NOTE:** This task releases all projects to the local releaseRepo.
    The final openliberty zip can be found in
    
    ```open-liberty\dev\cnf\release\dev\openliberty\<version>\openliberty-xxx.zip```

5. Go [Open issues](https://github.com/OpenLiberty/open-liberty/issues), [Review existing contributions](https://github.com/OpenLiberty/open-liberty/pulls), or [Submit fixes](https://github.com/OpenLiberty/open-liberty/blob/master/CONTRIBUTING.md).

## Community
1. [Open Liberty group.io](https://groups.io/g/openliberty)
2. [OpenLibertyIO on Twitter](https://twitter.com/OpenLibertyIO)
3. [open-liberty tag on stackoverflow](https://stackoverflow.com/questions/tagged/open-liberty)

