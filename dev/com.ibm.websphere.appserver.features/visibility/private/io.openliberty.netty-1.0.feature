-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.netty-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: com.ibm.websphere.endpoint; type="ibm-api"
IBM-Process-Types: \
 server, \
 client
-features= \
 io.openliberty.io.netty, \
 io.openliberty.io.netty.ssl
-bundles= \
 io.openliberty.netty
-jars=
-files=
kind=noship
edition=full
WLP-Activation-Type: parallel
