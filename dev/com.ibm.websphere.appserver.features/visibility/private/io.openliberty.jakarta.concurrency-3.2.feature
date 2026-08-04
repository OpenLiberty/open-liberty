-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.concurrency-3.2
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.jakarta.concurrency.3.2; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.concurrency:jakarta.concurrency-api:3.2.0-M2"
kind=noship
edition=full
WLP-Activation-Type: parallel
