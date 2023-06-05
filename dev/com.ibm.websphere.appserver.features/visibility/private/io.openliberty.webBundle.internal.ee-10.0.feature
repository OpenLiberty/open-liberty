-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.ee-10.0
singleton=true
visibility = private

-features= io.openliberty.servlet.internal-6.0; ibm.tolerates:="6.1"

-bundles= com.ibm.ws.app.manager.wab.jakarta; start-phase:=APPLICATION_EARLY

kind=ga
edition=core
WLP-Activation-Type: parallel
