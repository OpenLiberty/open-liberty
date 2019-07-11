<!-- PROJECT LOGO -->

<p align="center">
  <a href="https://openliberty.io/">
    <img src="https://openliberty.io/img/spaceship.svg" alt="Logo">
  </a>
</p>
<p align="center">
  <a href="https://openliberty.io/">
    <img src="https://github.com/OpenLiberty/open-liberty/blob/master/logos/logo_horizontal_light_navy.png" alt="title" width="400">
  </a>
</p>
<br />


[![Maven Central](https://img.shields.io/maven-central/v/io.openliberty/openliberty-runtime.svg?label=Maven%20Central)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.openliberty%22%20a%3A%22openliberty-runtime%22)
[![Docker Pulls](https://img.shields.io/docker/pulls/_/open-liberty.svg?color=yellow)](https://hub.docker.com/_/open-liberty)
[![Website](https://img.shields.io/badge/website-live-purple.svg)](https://openliberty.io/)
[![Stack Overflow](https://img.shields.io/badge/find-answers-blue.svg)](https://stackoverflow.com/questions/tagged/open-liberty)
[![Groups.io](https://img.shields.io/badge/ask-groups.io-orange.svg)](https://groups.io/g/openliberty)
[![License](https://img.shields.io/badge/License-EPL%201.0-green.svg)](https://opensource.org/licenses/EPL-1.0)
[![Twitter](https://img.shields.io/twitter/follow/openlibertyio.svg?style=social&label=Follow)](https://twitter.com/OpenLibertyIO)

<!-- TABLE OF CONTENTS -->

## Table of Contents
* [Summary](#summary)
* [Getting Started](#getting-started)
  * [Prerequisites](#prerequisites)
  * [Downloads](#downloads)
* [Usage](#usage)
  * [Docs](#open-liberty-docs)
* [Contributing](#contributing)
  * [Running a Build](#running-a-build)
* [License](#license)
* [Contact](#contact)

# Summary
A lightweight open framework for building fast and efficient cloud-native Java microservices.

* The Open Liberty modular architecture means only the features you need are loaded at runtime to help reduce image size and memory consumption. The smaller footprint means you can run more application instances per machine to help reduce production costs.

* Servers running Open Liberty start faster, making them ideal for rapid scale-up and scale-down of containerized applications.

## Getting Started

### Launch a sample app using [Maven](https://maven.apache.org/)
    git clone https://github.com/OpenLiberty/sample-getting-started.git
    cd sample-getting-started
    mvn clean package liberty:run-server

Open browser to http://localhost:9080/ and explore the demo application.

This [guide](https://openliberty.io/guides/getting-started.html) shows how it was built

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
[![License](https://img.shields.io/badge/License-EPL%201.0-green.svg)](https://opensource.org/licenses/EPL-1.0)

Usage is provided under the [EPL 1.0 license](https://opensource.org/licenses/EPL-1.0) See LICENSE for the full details.


## Contact
1. [Open Liberty group.io](https://groups.io/g/openliberty)
2. [Open an issue](https://github.com/OpenLiberty/open-liberty/issues)
3. [Open Liberty on stackoverflow](https://stackoverflow.com/questions/tagged/open-liberty)
