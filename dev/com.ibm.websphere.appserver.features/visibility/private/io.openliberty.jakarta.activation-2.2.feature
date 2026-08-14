-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.activation-2.2
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, install
IBM-Process-Types: client, server
Subsystem-Name: Jakarta Activation 2.2
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.activation.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.activation:jakarta.activation-api:2.1.3", \
  io.openliberty.org.glassfish.hk2.osgi-resource-locator
kind=noship
edition=full
WLP-Activation-Type: parallel
