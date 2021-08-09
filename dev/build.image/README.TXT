This project is responsible for building the install zip for openliberty.

The openliberty zip is generated into the generated/distributions directory.

To build, run gradle target publish.
e.g. from dev: ./gradlew build.image:publish

Version control information is defined in /cnf/resources/bnd/liberty-release.props

