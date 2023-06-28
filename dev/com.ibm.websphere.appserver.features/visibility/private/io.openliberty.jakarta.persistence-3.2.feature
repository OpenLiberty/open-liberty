-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence-3.2
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-3.2, \
  com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=io.openliberty.jakarta.persistence.api.3.1
-jars=io.openliberty.jakarta.persistence.3.1; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:3.1.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
