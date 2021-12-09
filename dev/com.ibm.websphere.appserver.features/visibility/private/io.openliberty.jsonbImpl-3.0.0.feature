# This private impl feature corresponds to JSON-B 2.1 with the Yasson implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbImpl-3.0.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.cdi-4.0, \
  io.openliberty.jsonp-2.1
-bundles=\
  com.ibm.ws.org.eclipse.yasson.2.0, \
  io.openliberty.jakarta.jsonb.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:2.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
