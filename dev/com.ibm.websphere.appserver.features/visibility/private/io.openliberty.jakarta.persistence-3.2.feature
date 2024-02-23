-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence-3.2
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-3.2, \
  com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=io.openliberty.jakarta.persistence.api.3.2
-jars=io.openliberty.jakarta.persistence.3.2; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:3.2.0-M1"
kind=noship
edition=full
WLP-Activation-Type: parallel
