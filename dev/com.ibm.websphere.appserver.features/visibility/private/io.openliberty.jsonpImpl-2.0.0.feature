# This private impl feature corresponds to jsonpContainer-1.1, which gives you
# JSON-P 1.1 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpImpl-2.0.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.bells-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.jsonp.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json:jakarta.json-api:2.0.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
