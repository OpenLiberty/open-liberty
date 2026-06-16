# This private impl feature corresponds to jsonpContainer-2.2, which gives you
# JSON-P 2.1 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpImpl-2.2.0
singleton=true
visibility=private
-features=\
  com.ibm.websphere.appserver.bells-1.0, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.noShip-1.0
-bundles=\
 io.openliberty.jakarta.jsonp.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json:jakarta.json-api:2.1.3"
kind=noship
edition=full
WLP-Activation-Type: parallel
