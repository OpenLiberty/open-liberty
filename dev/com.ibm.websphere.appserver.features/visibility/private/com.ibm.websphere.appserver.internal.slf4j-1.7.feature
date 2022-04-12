-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.slf4j-1.7
WLP-DisableAllFeatures-OnConflict: false
-bundles=com.ibm.ws.org.slf4j.jdk14, \
 com.ibm.ws.org.slf4j.api
kind=ga
edition=core
WLP-Activation-Type: parallel
