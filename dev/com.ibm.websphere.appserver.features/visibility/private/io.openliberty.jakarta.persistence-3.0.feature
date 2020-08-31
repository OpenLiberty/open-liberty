-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence-3.0
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-3.0
-bundles=com.ibm.ws.jakartaee.persistence.api.3.0
-jars=io.openliberty.jakarta.persistence.3.0; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:3.0-RC1"
kind=noship
edition=full
WLP-Activation-Type: parallel
