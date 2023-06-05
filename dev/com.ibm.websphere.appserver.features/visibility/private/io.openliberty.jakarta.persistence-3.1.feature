-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence-3.1
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-3.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.jakarta.persistence.api.3.1
-jars=io.openliberty.jakarta.persistence.3.1; location:=dev/api/spec/; mavenCoordinates="jakarta.persistence:jakarta.persistence-api:3.1.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
