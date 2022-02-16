# This private impl feature corresponds to JjsonbContainer-2.0, which gives you
# JSON-B 2.0 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbImpl-2.0.0
singleton=true
visibility=private
-features=io.openliberty.jsonp-2.0, \
  com.ibm.websphere.appserver.bells-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.cdi-3.0
-bundles=io.openliberty.jakarta.jsonb.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:2.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
