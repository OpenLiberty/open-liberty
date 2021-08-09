# This private impl feature corresponds to JSON-P 1.0 with the Glassfish implementation
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonpImpl-1.0.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
visibility=private
-bundles=com.ibm.websphere.javaee.jsonp.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.json:javax.json-api:1.0", \
 com.ibm.ws.org.glassfish.json.1.0
kind=ga
edition=core
WLP-Activation-Type: parallel
