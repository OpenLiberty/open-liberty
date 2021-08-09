-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appLifecycle-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-Process-Types: server, \
 client
-bundles=com.ibm.ws.app.manager.lifecycle; start-phase:=SERVICE_EARLY
kind=ga
edition=core
WLP-Activation-Type: parallel
