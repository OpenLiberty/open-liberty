-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeePlatform-8.0
WLP-DisableAllFeatures-OnConflict: false
IBM-Process-Types: client, server
-features=io.openliberty.eePlatform.7.0.internal-1.0, \
 com.ibm.websphere.appserver.eeCompatible-8.0
kind=ga
edition=core
WLP-Activation-Type: parallel
