[![Groups.io](https://img.shields.io/badge/ask-groups.io-orange.svg)](https://groups.io/g/openliberty)
[![Stack Overflow](https://img.shields.io/badge/find-answers-blue.svg)](https://stackoverflow.com/questions/tagged/open-liberty)
[![Twitter](https://img.shields.io/twitter/follow/openlibertyio.svg?style=social&label=Follow)](https://twitter.com/OpenLibertyIO)
[![License](https://img.shields.io/badge/License-EPL%201.0-green.svg)](https://opensource.org/licenses/EPL-1.0)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Maven Central](https://img.shields.io/maven-central/v/io.openliberty/openliberty-runtime.svg?label=Maven%20Central)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.openliberty%22%20a%3A%22openliberty-runtime%22)

<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="https://github.com/othneildrew/Best-README-Template">
    <img src="https://openliberty.io/img/spaceship.svg" alt="Logo">
  </a>

  <h1 align="center">Open Liberty</h1>
</p>

<!-- TABLE OF CONTENTS -->
## Table of Contents

* [Summary](#summary)
* [Getting Started](#getting-started)
  * [Prerequisites](#prerequisites)
  * [Installation](#installation)
* [Usage](#usage)
  * [Docs](#open-liberty-docs)
* [Contributing](#contributing)
  * [Running a Build](#running-a-build)
* [License](#license)
* [Contact](#contact)

# Summary
Open Liberty is a comprehensive, flexible and secure Java EE and MicroProfile application server ready for building modern cloud-native applications and services.

* The Open Liberty modular architecture means only the features you need are loaded at runtime to help reduce memory consumption. The smaller footprint means you can run more application instances per machine to help reduce production costs.

* Servers running WebSphere Liberty start faster, making them ideal for rapid scale-up and scale-down of containerized applications.


## Getting Started

### Quick start using [Maven](https://maven.apache.org/)
    git clone https://github.com/OpenLiberty/sample-getting-started.git
    cd sample-getting-started
    mvn clean package liberty:run-server

Open browser to http://localhost:9080/ and explore the demo application.

### Prerequisites

* Java SE 8,11,12 - [More details](https://openliberty.io/docs/ref/general/#java-se.html)

### Downloads

* Zipped released versions and nightly build artifacts from: https://www.openliberty.io/downloads/
* Official Released Open Liberty Docker containers from : https://hub.docker.com/_/open-liberty


## Usage

### Controlling Open Liberty

The bin directory contains a server script to help control the server process.
The script supports the following actions:

Command| Does...
:-----:|:-----:
`create`|creates a new server
`start`|launches the server as a background process
`run`|launches the server in the foreground
`debug`|launches the server in the foreground with JVM debug options
`stop`|stops a running server
`status`|check to see if a specified server is running
`package`|packages server runtime and target server configuration/application(s) into zip archive file
`dump`|dump diagnostic information from the server into an archive
`javadump`|dump diagnostic information from the server JVM
`list`|list existing servers
`version`|displays the version of the server runtime
`pause`|pause all the components in the server that can be paused
`resume`|resume all paused components in the server
`help`|get command-line/script help, including descriptions of additional options

To use the script on Windows (the .bat extension is optional):

    bin\server.bat create <serverName>
    bin\server.bat start <serverName>
    bin\server.bat help

To use the script on other platforms:

    bin/server create <serverName>
    bin/server start <serverName>
    bin/server help


### Open Liberty Docs
Visit the [OpenLiberty website](https://openliberty.io/docs/)
* Reference docs including features, config and API's
* Open Liberty Guides - exploring many technologies through step by step examples.


## Contributing
Our [CONTRIBUTING](https://github.com/OpenLiberty/open-liberty/blob/master/CONTRIBUTING.md) document contains details for submitting pull requests.

### Running a Build

1. Clone the repository to your system.

        git clone git@github.com:OpenLiberty/open-liberty.git

2. Run a gradle build.

        cd open-liberty/dev
        ./gradlew cnf:initialize
        ./gradlew assemble`
    
3. Run the unit or FAT tests.

    `./gradlew test` for unit tests
    
    `./gradlew build.example_fat:buildandrun` to run a [FAT project](https://github.com/OpenLiberty/open-liberty/wiki/FAT-tests)
   
   **NOTE:** ```./gradlew build``` runs the `assemble` and `test` tasks
   
4. Perform a local release

    ```./gradlew releaseNeeded```
    
    **NOTE:** This task releases all projects to the local releaseRepo.
    The final openliberty zip can be found in
    
    ```open-liberty\dev\cnf\release\dev\openliberty\<version>\openliberty-xxx.zip```

## License
Usage is provided under the [EPL 1.0 license](https://opensource.org/licenses/EPL-1.0) See LICENSE for the full details.


## Contact
1. [Open Liberty group.io](https://groups.io/g/openliberty)
2. [Open an issue](https://github.com/OpenLiberty/open-liberty/issues)
