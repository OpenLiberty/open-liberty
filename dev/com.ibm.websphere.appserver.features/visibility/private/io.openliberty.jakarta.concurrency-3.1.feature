-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.concurrency-3.1
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.jakarta.concurrency.3.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.1.0-M1"
kind=noship
edition=full
WLP-Activation-Type: parallel
