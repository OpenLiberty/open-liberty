-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence-4.0
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-4.0, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.jakarta.persistence.api.4.0
-jars=io.openliberty.jakarta.persistence.4.0; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:4.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
