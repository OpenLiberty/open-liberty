-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.mail-1.6
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
Subsystem-Version: 1.6
-features=com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0, 7.0"
-bundles=com.ibm.ws.com.sun.mail.javax.mail.1.6
kind=ga
edition=core
WLP-Activation-Type: parallel
