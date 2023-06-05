# This private impl feature corresponds to JSON-B 3.0 with the Yasson implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonbImpl-3.0.1
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.cdi-4.0, \
  io.openliberty.jsonp-2.1
-bundles=\
  io.openliberty.jakarta.jsonb.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:3.0.0",\
  io.openliberty.org.eclipse.yasson.3.0
kind=ga
edition=core
WLP-Activation-Type: parallel
