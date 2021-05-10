# This private impl feature corresponds to jsonpContainer-1.1, which gives you
# JSON-P 1.1 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonpImpl-1.1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.bells-1.0, \
  com.ibm.websphere.appserver.eeCompatible-8.0
-bundles=com.ibm.websphere.javaee.jsonp.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.json:javax.json-api:1.1.3"
kind=ga
edition=core
