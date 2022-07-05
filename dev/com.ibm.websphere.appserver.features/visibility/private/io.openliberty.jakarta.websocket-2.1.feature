-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.websocket-2.1
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  io.openliberty.jakarta.websocket.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.websocket:jakarta.websocket-api:2.1.0", \
  io.openliberty.jakarta.websocket.client.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.websocket:jakarta.websocket-client-api:2.1.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
