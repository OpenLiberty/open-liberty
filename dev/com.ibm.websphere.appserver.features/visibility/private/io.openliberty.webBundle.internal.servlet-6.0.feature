-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.servlet-6.0
singleton=true
visibility = private

Subsystem-Name: OSGi Application

-features=io.openliberty.servlet.api-6.0, \
  io.openliberty.servlet.internal-6.0

-bundles= com.ibm.ws.app.manager.wab.jakarta; start-phase:=APPLICATION_EARLY, \
  com.ibm.ws.eba.wab.integrator

kind=beta
edition=core
WLP-Activation-Type: parallel
