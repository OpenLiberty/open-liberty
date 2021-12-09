-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.ee-9.0
singleton=true
visibility = private

-features= com.ibm.websphere.appserver.servlet-5.0; ibm.tolerates:="6.0"

-bundles= com.ibm.ws.app.manager.wab.jakarta; start-phase:=APPLICATION_EARLY

kind=ga
edition=core
WLP-Activation-Type: parallel
