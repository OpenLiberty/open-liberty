# This private impl feature corresponds to JSON-P 2.1 with the Parsson implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpImpl-2.1.1
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.jakarta.jsonp.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="io.openliberty.jakarta.json:jakarta.json-api:2.1.0", \
 io.openliberty.org.eclipse.parsson.1.1
kind=noship
edition=full
WLP-Activation-Type: parallel
