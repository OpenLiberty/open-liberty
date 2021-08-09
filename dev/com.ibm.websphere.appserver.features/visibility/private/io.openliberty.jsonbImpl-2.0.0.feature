# This private impl feature corresponds to JSON-B 2.0 with the Yasson implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbImpl-2.0.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.cdi-3.0, \
  io.openliberty.jsonp-2.0
-bundles=\
  com.ibm.ws.org.eclipse.yasson.2.0, \
  io.openliberty.jakarta.jsonb.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:2.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
