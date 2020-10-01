-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.persistence.base-2.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.containerServices-1.0
-bundles=com.ibm.ws.javaee.persistence.2.2; location:=lib/
kind=ga
edition=core
WLP-Activation-Type: parallel
