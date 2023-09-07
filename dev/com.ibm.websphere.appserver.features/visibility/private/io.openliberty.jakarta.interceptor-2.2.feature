-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.interceptor-2.2
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.jakarta.interceptor.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.interceptor:jakarta.interceptor-api:2.1.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
