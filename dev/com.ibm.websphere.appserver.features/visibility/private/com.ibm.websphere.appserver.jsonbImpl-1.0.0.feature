# This private impl feature corresponds to jsonbContainer-1.0, which gives you
# JSON-B 1.0 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonbImpl-1.0.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.bells-1.0, \
  com.ibm.websphere.appserver.eeCompatible-8.0, \
  com.ibm.websphere.appserver.javax.cdi-2.0
-bundles=com.ibm.websphere.javaee.jsonb.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.json.bind:javax.json.bind-api:1.0"
kind=ga
edition=core
