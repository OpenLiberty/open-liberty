# This private impl feature corresponds to jsonbContainer-3.0, which gives you
# JSON-B 3.0 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbImpl-3.0.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  com.ibm.websphere.appserver.bells-1.0, \
  io.openliberty.jakarta.cdi-4.0; ibm.tolerates:="4.1", \
  io.openliberty.jsonp-2.1
-bundles=\
  io.openliberty.jakarta.jsonb.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:3.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
