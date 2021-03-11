-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.netty-1.0
visibility=public
WLP-DisableAllFeatures-OnConflict: false
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
 io.openliberty.netty ; type="ibm-api",\
 com.ibm.wsspi.bytebuffer; type="ibm-api"
IBM-ShortName: netty-1.0
Subsystem-Name: Liberty Netty based IO Channels 1.0
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
