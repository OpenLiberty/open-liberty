-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-8.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
Subsystem-Version: 8.0.0
-bundles=com.ibm.ws.javaee.version
-features=io.openliberty.microProfile.internal-2.2; ibm.tolerates:="3.0,3.3,4.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
