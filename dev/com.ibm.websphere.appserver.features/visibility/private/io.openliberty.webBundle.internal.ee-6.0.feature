-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

-features= com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0"

-bundles= com.ibm.ws.app.manager.wab; start-phase:=APPLICATION_EARLY

edition=core
kind=ga
WLP-Activation-Type: parallel
