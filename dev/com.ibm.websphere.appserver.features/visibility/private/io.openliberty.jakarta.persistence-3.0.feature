-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence-3.0
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-3.0
-bundles=io.openliberty.jakarta.persistence.api.3.0
-jars=io.openliberty.jakarta.persistence.3.0; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:3.0-RC2"
kind=beta
edition=core
WLP-Activation-Type: parallel
