# This private impl feature corresponds to jsonbContainer-3.1, which gives you
# JSON-B 3.1 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbImpl-3.1.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  com.ibm.websphere.appserver.bells-1.0, \
  io.openliberty.jakarta.cdi-5.0, \
  io.openliberty.jsonp-2.2, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.jsonb.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:3.0.1"
kind=noship
edition=full
WLP-Activation-Type: parallel
