-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-7.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
Subsystem-Version: 7.0.0
-bundles=com.ibm.ws.javaee.version
-features=io.openliberty.microProfile.internal-1.4
kind=ga
edition=core
WLP-Activation-Type: parallel
