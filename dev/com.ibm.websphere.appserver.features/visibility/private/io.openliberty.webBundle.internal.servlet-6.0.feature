-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.servlet-6.0
singleton=true
visibility = private

-features=io.openliberty.servlet.api-6.0, \
  io.openliberty.servlet.internal-6.0

-bundles= com.ibm.ws.app.manager.wab.jakarta; start-phase:=APPLICATION_EARLY

kind=beta
edition=core
WLP-Activation-Type: parallel
