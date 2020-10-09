-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.iioptransport-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.iiopcommon-1.0
-bundles=com.ibm.ws.transport.iiop.server
kind=ga
edition=base
WLP-Activation-Type: parallel
