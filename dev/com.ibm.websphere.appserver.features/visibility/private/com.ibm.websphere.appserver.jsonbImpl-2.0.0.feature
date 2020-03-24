# This private impl feature corresponds to JSON-B 2.0 with the Yasson implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonbImpl-2.0.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.jsonp-2.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.jakarta.cdi-3.0,\
  com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=com.ibm.ws.org.eclipse.yasson.1.0.jakarta, \
  com.ibm.websphere.jakarta.jsonb.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json.bind:jakarta.json.bind-api:2.0.0"
kind=noship
edition=core
WLP-Activation-Type: parallel
