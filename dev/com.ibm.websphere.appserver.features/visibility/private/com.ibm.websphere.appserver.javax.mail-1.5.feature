-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.mail-1.5
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
Subsystem-Version: 1.5
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0, 8.0"
-bundles=com.ibm.ws.com.sun.mail.javax.mail.1.5
kind=ga
edition=core
