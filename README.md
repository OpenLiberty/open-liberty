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

# Summary
A lightweight open framework for building fast and efficient cloud-native Java microservices:
* Open Liberty is fast to start up with low memory footprint and live reload for quick iteration.
* Simple to add and remove features from the latest versions of MicroProfile and Java EE.
* Zero migration lets you focus on what's important, not the APIs changing under you.

## Getting Started

### Launch a sample app using [Maven](https://maven.apache.org/)
    git clone https://github.com/OpenLiberty/sample-getting-started.git
    cd sample-getting-started
    mvn clean package liberty:run

Open browser to http://localhost:9080/ and explore the demo application.

See [this guide](https://openliberty.io/guides/getting-started.html)  to learn how it works.

### Downloads

* [Maven Central](https://mvnrepository.com/artifact/io.openliberty/openliberty-runtime)
* [Open Liberty website](https://www.openliberty.io/downloads/)
* [Docker Hub](https://hub.docker.com/_/open-liberty)

## Usage


### Docker

* [This guide](https://openliberty.io/guides/docker.html) demonstrates development using Docker containers. 

### Maven

* [This guide](https://openliberty.io/guides/maven-intro.html) demonstrates development using the Liberty Maven Plugin. 

### Command Line

* [This reference](https://openliberty.io/docs/ref/command/) demonstrates the basic server operation commands.


### Open Liberty Docs

* [Open Liberty Guides](https://openliberty.io/guides/)
* [Reference docs including features, config and API's](https://openliberty.io/docs/)


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

