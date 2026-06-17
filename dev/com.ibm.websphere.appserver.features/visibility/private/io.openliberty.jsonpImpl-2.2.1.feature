# This private impl feature corresponds to JSON-P 2.2 with the Parsson implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpImpl-2.2.1
singleton=true
visibility=private
-features=\
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.jsonp.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json:jakarta.json-api:2.1.3", \
  io.openliberty.org.eclipse.parsson.1.1
kind=noship
edition=full
WLP-Activation-Type: parallel
