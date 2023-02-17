# This private impl feature corresponds to jsonpContainer-2.1, which gives you
# JSON-P 2.1 spec with the ability to choose the default provider via a bell.
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpImpl-2.1.0
singleton=true
visibility=private
-features=\
  com.ibm.websphere.appserver.bells-1.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
 io.openliberty.jakarta.jsonp.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json:jakarta.json-api:2.1.1"
kind=beta
edition=core
WLP-Activation-Type: parallel
